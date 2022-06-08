package no.nav.bidrag.arbeidsflyt

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import no.nav.bidrag.arbeidsflyt.consumer.BidragOrganisasjonConsumer
import no.nav.bidrag.arbeidsflyt.consumer.DefaultOppgaveConsumer
import no.nav.bidrag.arbeidsflyt.consumer.DefaultPersonConsumer
import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.consumer.PersonConsumer
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.hendelse.KafkaJournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.hendelse.KafkaRetryListener
import no.nav.bidrag.arbeidsflyt.model.EndreOppgaveFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.OpprettOppgaveFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.arbeidsflyt.service.PersistenceService
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.ExceptionLogger
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RootUriTemplateHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.Scope
import org.springframework.core.env.Environment
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Duration
import javax.sql.DataSource


@Configuration
@Profile(value = [PROFILE_KAFKA_TEST, PROFILE_NAIS, "local"])
@EnableScheduling
@EnableRetry
@EnableSchedulerLock(defaultLockAtMostFor = "20m")
class HendelseConfiguration {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(HendelseConfiguration::class.java)
    }

    @Bean
    fun lockProvider(dataSource: DataSource): LockProvider? {
        return JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(JdbcTemplate(dataSource))
                .usingDbTime()
                .build()
        )
    }

    @Bean
    fun journalpostHendelseListener(
        jsonMapperService: JsonMapperService, behandleHendelseService: BehandleHendelseService
    ): JournalpostHendelseListener = KafkaJournalpostHendelseListener(
        jsonMapperService, behandleHendelseService
    )

    @Bean
    fun defaultErrorHandler(@Value("\${KAFKA_MAX_RETRY:10}") maxRetries: Int, persistenceService: PersistenceService): DefaultErrorHandler? {
        LOGGER.info("Init kafka errorhandler with exponential backoff and maxRetries=$maxRetries")
        val errorHandler =  DefaultErrorHandler({ rec: ConsumerRecord<*, *>, ex: Exception? ->
            val key = rec.key()
            val value = rec.value()
            val offset = rec.offset()
            val topic =  rec.topic()
            val partition =  rec.partition()
            val errorMessage = "Håndtering av Kafka melding feilet. Nøkkel $key, partition $partition, topic $topic og offset $offset. Melding som feilet: $value"
            LOGGER.error(errorMessage, ex)
            SECURE_LOGGER.error(errorMessage, ex) // Log message without censoring sensitive data
            val retryableException = !(ex?.cause is OpprettOppgaveFeiletFunksjoneltException || ex?.cause is EndreOppgaveFeiletFunksjoneltException)
            persistenceService.lagreDLQKafka(topic, key?.toString(), value?.toString() ?: "{}", retryableException)
        }, ExponentialBackOffWithMaxRetries(1))
        errorHandler.setRetryListeners(KafkaRetryListener())
        errorHandler.addNotRetryableExceptions(OpprettOppgaveFeiletFunksjoneltException::class.java, EndreOppgaveFeiletFunksjoneltException::class.java)
        return errorHandler
    }

    @Bean
    fun oppgaveKafkaListenerContainerFactory(oppgaveConsumerFactory: ConsumerFactory<Long, String>, defaultErrorHandler: DefaultErrorHandler): ConcurrentKafkaListenerContainerFactory<Long, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<Long, String>()
        factory.consumerFactory = oppgaveConsumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        //Retry consumer/listener even if authorization fails
        factory.setContainerCustomizer { container ->
            container.containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(10L))
        }

        factory.setCommonErrorHandler(defaultErrorHandler);
        return factory;
    }

    @Bean
    fun oppgaveConsumerFactory(@Value("\${KAFKA_BOOTSTRAP_SERVERS}") bootstrapServers: String,
                        @Value("\${KAFKA_GROUP_ID}") groupId: String,
                        @Value("\${OPPGAVE_KAFKA_OFFSET_RESET:latest}") offsetReset: String,
                        @Value("\${NAV_TRUSTSTORE_PATH}") trustStorePath: String,
                        @Value("\${NAV_TRUSTSTORE_PASSWORD}") trustStorePassword: String,
                        @Value("\${SERVICE_USER_USERNAME}") username: String,
                        @Value("\${SERVICE_USER_PASSWORD}") password: String,
                        environment: Environment): ConsumerFactory<Long, String> {
        val props = mutableMapOf<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = groupId
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = offsetReset
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        props["spring.deserializer.key.delegate.class"] = LongDeserializer::class.java
        props["spring.deserializer.value.delegate.class"] = StringDeserializer::class.java
        if (!environment.activeProfiles.contains(PROFILE_KAFKA_TEST)){
            props[SaslConfigs.SASL_JAAS_CONFIG] =
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";"
            props[SaslConfigs.SASL_MECHANISM] = "PLAIN"
            props[SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"
            props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = trustStorePath
            props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = trustStorePassword
        }
        return DefaultKafkaConsumerFactory(props)
    }
}

@Configuration
@EnableSecurityConfiguration
class ArbeidsflytConfiguration {

    @Bean
    @Scope("prototype")
    fun restTemplate(): HttpHeaderRestTemplate {
        val httpHeaderRestTemplate = HttpHeaderRestTemplate(HttpComponentsClientHttpRequestFactory())
        httpHeaderRestTemplate.addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER) { CorrelationId.fetchCorrelationIdForThread() }
        return httpHeaderRestTemplate
    }

    @Bean
    fun oppgaveConsumer(
        @Value("\${OPPGAVE_URL}") oppgaveUrl: String,
        restTemplate: HttpHeaderRestTemplate, securityTokenService: SecurityTokenService
    ): OppgaveConsumer {
        restTemplate.uriTemplateHandler = RootUriTemplateHandler(oppgaveUrl)
        restTemplate.interceptors.add(securityTokenService.serviceUserAuthTokenInterceptor("oppgave"))
        return DefaultOppgaveConsumer(restTemplate)
    }

    @Bean
    fun personConsumer(
        @Value("\${PERSON_URL}") personUrl: String,
        restTemplate: HttpHeaderRestTemplate, securityTokenService: SecurityTokenService
    ): PersonConsumer {
        restTemplate.uriTemplateHandler = RootUriTemplateHandler("$personUrl/bidrag-person")
        restTemplate.interceptors.add(securityTokenService.serviceUserAuthTokenInterceptor("person"))
        return DefaultPersonConsumer(restTemplate)
    }

    @Bean
    fun organisasjonConsumer(
        @Value("\${ORGANISASJON_URL}") organisasjonUrl: String,
        restTemplate: HttpHeaderRestTemplate, securityTokenService: SecurityTokenService
    ): BidragOrganisasjonConsumer {
        restTemplate.uriTemplateHandler = RootUriTemplateHandler("$organisasjonUrl/bidrag-organisasjon")
        restTemplate.interceptors.add(securityTokenService.serviceUserAuthTokenInterceptor("organisasjon"))
        return BidragOrganisasjonConsumer(restTemplate)
    }

    @Bean
    fun ExceptionLogger() = ExceptionLogger(BidragArbeidsflyt::class.java.simpleName)
}
