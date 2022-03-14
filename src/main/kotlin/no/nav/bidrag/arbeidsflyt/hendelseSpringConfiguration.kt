package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.consumer.DefaultOppgaveConsumer
import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.hendelse.KafkaJournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.ExceptionLogger
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig
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
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.KafkaListenerErrorHandler
import org.springframework.kafka.listener.ListenerExecutionFailedException
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.messaging.Message
import java.time.Duration
import java.util.Optional


private const val KAFKA_LISTENER_ERROR_HANDLER = "KafkaListenerErrorHandler"

@Configuration
@Profile(value = [PROFILE_KAFKA_TEST, PROFILE_LIVE])
class HendelseConfiguration {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(HendelseConfiguration::class.java)
    }

    @Bean
    fun journalpostHendelseListener(
        jsonMapperService: JsonMapperService, behandleHendelseService: BehandleHendelseService
    ): JournalpostHendelseListener = KafkaJournalpostHendelseListener(
        jsonMapperService, behandleHendelseService
    )

    @Bean
    fun hendelseErrorHandler(exceptionLogger: ExceptionLogger): KafkaListenerErrorHandler {
        return KafkaListenerErrorHandler { message: Message<*>, e: ListenerExecutionFailedException ->
            try {
                message.payload
            } catch (t: Throwable) {
                exceptionLogger.logException(t, KAFKA_LISTENER_ERROR_HANDLER)
            }

            exceptionLogger.logException(e, KAFKA_LISTENER_ERROR_HANDLER)
            Optional.empty<Any>()
        }
    }

    @Bean
    fun oppgaveKafkaListenerContainerFactory(oppgaveConsumerFactory: ConsumerFactory<Long, String>): ConcurrentKafkaListenerContainerFactory<Long, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<Long, String>()
        factory.consumerFactory = oppgaveConsumerFactory

        factory.setErrorHandler { thrownException, data ->
            LOGGER.error("There was a problem during processing of the record from oppgave-endret kafka consumer.")
        }

        //Retry consumer/listener even if authorization fails
        factory.setContainerCustomizer { container ->
            container.containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(10L))
        }

        return factory;
    }

    @Bean
    fun oppgaveConsumerFactory(@Value("\${KAFKA_BOOTSTRAP_SERVERS}") bootstrapServers: String,
                        @Value("\${KAFKA_GROUP_ID}") groupId: String,
                        @Value("\${NAV_TRUSTSTORE_PATH}") trustStorePath: String,
                        @Value("\${NAV_TRUSTSTORE_PASSWORD}") trustStorePassword: String,
                        @Value("\${SERVICE_USER_USERNAME}") username: String,
                        @Value("\${SERVICE_USER_PASSWORD}") password: String,
                        environment: Environment): ConsumerFactory<Long, String> {
        val props = mutableMapOf<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = groupId
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = true
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
    fun ExceptionLogger() = ExceptionLogger(BidragArbeidsflyt::class.java.simpleName)
}
