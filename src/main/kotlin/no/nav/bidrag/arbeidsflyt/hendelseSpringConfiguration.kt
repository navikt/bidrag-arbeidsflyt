package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.consumer.DefaultOppgaveConsumer
import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.hendelse.KafkaJournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.hendelse.KafkaOppgaveHendelseListener
import no.nav.bidrag.arbeidsflyt.hendelse.OppgaveHendelseListener
import no.nav.bidrag.arbeidsflyt.model.Token
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.arbeidsflyt.service.SecurityTokenService
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.ExceptionLogger
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RootUriTemplateHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.Scope
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.kafka.listener.KafkaListenerErrorHandler
import org.springframework.kafka.listener.ListenerExecutionFailedException
import org.springframework.messaging.Message
import java.util.Optional

private const val KAFKA_LISTENER_ERROR_HANDLER = "KafkaListenerErrorHandler"

@Configuration
@Profile(PROFILE_LIVE)
@EnableJwtTokenValidation
class HendelseConfiguration {
    @Bean
    fun oppgaveHendelseListener(
        jsonMapperService: JsonMapperService, behandleHendelseService: BehandleHendelseService
    ): OppgaveHendelseListener = KafkaOppgaveHendelseListener()

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
}

@Configuration
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
        restTemplate: HttpHeaderRestTemplate, securityTokenService: SecurityTokenService,
    ): OppgaveConsumer {
        restTemplate.uriTemplateHandler = RootUriTemplateHandler(oppgaveUrl)
        restTemplate.interceptors.add(securityTokenService.generateBearerToken(Token.OPPGAVE_CLIENT_REGISTRATION_ID))
        return DefaultOppgaveConsumer(restTemplate)
    }

    @Bean
    fun ExceptionLogger() = ExceptionLogger(BidragArbeidsflyt::class.java.simpleName)
}
