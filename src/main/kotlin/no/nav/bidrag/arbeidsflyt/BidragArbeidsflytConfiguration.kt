package no.nav.bidrag.arbeidsflyt

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import no.nav.bidrag.arbeidsflyt.hendelse.KafkaRetryListener
import no.nav.bidrag.arbeidsflyt.model.EndreOppgaveFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.HentPersonFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.OpprettOppgaveFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.service.PersistenceService
import no.nav.bidrag.commons.ExceptionLogger
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.service.organisasjon.EnableSaksbehandlernavnProvider
import no.nav.bidrag.commons.unleash.EnableUnleashFeatures
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.util.backoff.FixedBackOff
import java.time.Duration
import javax.sql.DataSource

@Configuration
@EnableScheduling
@EnableRetry
@Import(RestOperationsAzure::class, AppContext::class)
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@EnableSaksbehandlernavnProvider
class HendelseConfiguration {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(HendelseConfiguration::class.java)
    }

    @Bean
    fun lockProvider(dataSource: DataSource): LockProvider? =
        JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration
                .builder()
                .withJdbcTemplate(JdbcTemplate(dataSource))
                .usingDbTime()
                .build(),
        )

    @Bean
    fun defaultErrorHandler(
        @Value("\${KAFKA_MAX_RETRY:10}") maxRetries: Int,
        persistenceService: PersistenceService,
    ): DefaultErrorHandler? {
        LOGGER.info("Init kafka errorhandler with exponential backoff and maxRetries=$maxRetries")
        val backoffStrategy =
            if (maxRetries == 0) {
                FixedBackOff(0, 0)
            } else {
                run {
                    val strategy = ExponentialBackOffWithMaxRetries(maxRetries)
                    strategy.multiplier = 2.0
                    strategy.maxInterval = 1200000L // 20 minutes
                    strategy
                }
            }

        val errorHandler =
            DefaultErrorHandler({ rec: ConsumerRecord<*, *>, ex: Exception? ->
                val key = rec.key()
                val value = rec.value()
                val offset = rec.offset()
                val topic = rec.topic()
                val partition = rec.partition()
                val errorMessage = "Håndtering av Kafka melding feilet. Nøkkel $key, partition $partition, topic $topic og offset $offset. Melding som feilet: $value"
                LOGGER.error(errorMessage, ex)
                SECURE_LOGGER.error(errorMessage, ex) // Log message without censoring sensitive data
                val retryableException = !(ex?.cause is OpprettOppgaveFeiletFunksjoneltException || ex?.cause is EndreOppgaveFeiletFunksjoneltException)
                persistenceService.lagreDLQKafka(topic, key?.toString()?.replace("\u0000", ""), value?.toString() ?: "{}", retryableException)
            }, backoffStrategy)
        errorHandler.setRetryListeners(KafkaRetryListener())
        errorHandler.addNotRetryableExceptions(
            HentPersonFeiletFunksjoneltException::class.java,
            OpprettOppgaveFeiletFunksjoneltException::class.java,
            EndreOppgaveFeiletFunksjoneltException::class.java,
        )
        return errorHandler
    }

    @Bean
    fun oppgaveKafkaListenerContainerFactory(
        oppgaveConsumerFactory: ConsumerFactory<Long, String>,
        defaultErrorHandler: DefaultErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<Long, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<Long, String>()
        factory.consumerFactory = oppgaveConsumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        // Retry consumer/listener even if authorization fails
        factory.setContainerCustomizer { container ->
            container.containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(10L))
        }

        factory.setCommonErrorHandler(defaultErrorHandler)
        return factory
    }
}

@Configuration
@EnableSecurityConfiguration
class ArbeidsflytConfiguration {
    @Bean
    fun clientRequestObservationConvention() = DefaultClientRequestObservationConvention()
}

@EnableUnleashFeatures
@Profile("nais")
@Configuration
class UnleashConfiguration
