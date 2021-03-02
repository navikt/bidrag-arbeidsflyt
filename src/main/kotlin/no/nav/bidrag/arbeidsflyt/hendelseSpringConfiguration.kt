package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.consumer.DefaultOppgaveConsumer
import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.hendelse.DefaultJournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.service.HendelseService
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.ExceptionLogger
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RootUriTemplateHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpHeaders
import org.springframework.kafka.listener.KafkaListenerErrorHandler
import org.springframework.kafka.listener.ListenerExecutionFailedException
import org.springframework.messaging.Message
import org.springframework.web.client.RestTemplate
import java.util.Optional

const val ISSUER = "todo: for navdevice"
private val LOGGER = LoggerFactory.getLogger(HendelseConfiguration::class.java)

@Configuration
@Profile(PROFILE_LIVE)
@EnableJwtTokenValidation(ignore = ["springfox.documentation.swagger.web.ApiResourceController"])
class HendelseConfiguration {
    @Bean
    fun journalpostHendelseListener(hendelseService: HendelseService): JournalpostHendelseListener = DefaultJournalpostHendelseListener(
        hendelseService
    )

    @Bean
    fun hendelseErrorHandler(): KafkaListenerErrorHandler {
        return KafkaListenerErrorHandler { message: Message<*>, e: ListenerExecutionFailedException ->
            LOGGER.error("Message {} cause error: {} - {} - headers: {}", message.payload, e.javaClass.simpleName, e.message, message.headers)
            Optional.empty<Any>()
        }
    }
}

@Configuration
class ArbeidsflytConfiguration {

    @Bean
    fun oppgaveConsumer(restTemplate: HttpHeaderRestTemplate, @Value("\${OPPGAVE_URL}") oppgaveUrl: String): OppgaveConsumer {
        restTemplate.uriTemplateHandler = RootUriTemplateHandler(oppgaveUrl)
        return DefaultOppgaveConsumer(restTemplate)
    }

    @Bean
    @Scope("prototype")
    fun httpHeaderRestTemplate(): HttpHeaderRestTemplate {
        val httpHeaderRestTemplate = HttpHeaderRestTemplate()
        httpHeaderRestTemplate.addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER) { CorrelationId.fetchCorrelationIdForThread() }

        return httpHeaderRestTemplate
    }

    @Bean
    fun ExceptionLogger() = ExceptionLogger(BidragArbeidsflyt::class.java.simpleName)
}
