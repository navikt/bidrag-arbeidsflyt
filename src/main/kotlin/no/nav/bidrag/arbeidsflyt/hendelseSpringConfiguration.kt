package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.consumer.DefaultOppgaveConsumer
import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.hendelse.KafkaJournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.model.MiljoVariabler.NAIS_APP_NAME
import no.nav.bidrag.arbeidsflyt.model.MiljoVariabler.OPPGAVE_URL
import no.nav.bidrag.arbeidsflyt.model.Token
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.arbeidsflyt.service.SecurityTokenService
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.ExceptionLogger
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RootUriTemplateHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.Scope
import org.springframework.kafka.listener.KafkaListenerErrorHandler
import org.springframework.kafka.listener.ListenerExecutionFailedException
import org.springframework.messaging.Message
import java.util.Optional

internal object Environment {
    private val dummy = mapOf(
        OPPGAVE_URL to "https://dummy.test",
        NAIS_APP_NAME to "bidrag-arbeidsflyt"
    )

    internal fun fetchEnv(name: String) = System.getProperty(name) ?: System.getenv()[name] ?: dummy[name]
    ?: throw IllegalStateException(
        "Unable to find $name as a system property or an environment variable"
    )
}

@Configuration
@Profile(PROFILE_LIVE)
@EnableJwtTokenValidation
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
    fun hendelseErrorHandler(): KafkaListenerErrorHandler {
        return KafkaListenerErrorHandler { message: Message<*>, e: ListenerExecutionFailedException ->
            val messagePayload: Any = try {
                message.payload
            } catch (re: RuntimeException) {
                "Unable to read message payload"
            }

            LOGGER.error("Message {} cause error: {} - {} - headers: {}", messagePayload, e.javaClass.simpleName, e.message, message.headers)
            Optional.empty<Any>()
        }
    }
}

@Configuration
class ArbeidsflytConfiguration {

    @Bean
    @Scope("prototype")
    fun restTemplate(): HttpHeaderRestTemplate {
        val httpHeaderRestTemplate = HttpHeaderRestTemplate()
        httpHeaderRestTemplate.addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER) { CorrelationId.fetchCorrelationIdForThread() }
        return httpHeaderRestTemplate
    }

    @Bean
    fun oppgaveConsumer(restTemplate: HttpHeaderRestTemplate, securityTokenService: SecurityTokenService): OppgaveConsumer {
        restTemplate.uriTemplateHandler = RootUriTemplateHandler(Environment.fetchEnv(OPPGAVE_URL))
        restTemplate.interceptors.add(securityTokenService.generateBearerToken(Token.OPPGAVE_CLIENT_REGISTRATION_ID))
        return DefaultOppgaveConsumer(restTemplate)
    }

    @Bean
    fun ExceptionLogger() = ExceptionLogger(BidragArbeidsflyt::class.java.simpleName)
}
