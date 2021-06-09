package no.nav.bidrag.arbeidsflyt.aop

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.model.CORRELATION_ID
import no.nav.bidrag.commons.CorrelationId
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
@Aspect
class HendelseCorrelationAspect(private val objectMapper: ObjectMapper) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(HendelseCorrelationAspect::class.java)
    }

    @Before(value = "execution(* no.nav.bidrag.arbeidsflyt.service.JsonMapperService.*(..)) and args(hendelse)")
    fun addCorrelationIdToThread(joinPoint: JoinPoint, hendelse: String) {
        try {
            val jsonNode = objectMapper.readTree(hendelse)
            val correlationIdJsonNode = jsonNode["sporing"]?.get(CORRELATION_ID)

            if (correlationIdJsonNode == null) {
                val unknown = "unknown-${System.currentTimeMillis().toString(16)}"
                LOGGER.warn("Unable to find correlation Id in '${hendelse.trim(' ')}', using '$unknown'")
                MDC.put(CORRELATION_ID, unknown)
            } else {
                val correlationId = CorrelationId.existing(correlationIdJsonNode.asText())
                MDC.put(CORRELATION_ID, correlationId.get())
            }
        } catch (e: Exception) {
            LOGGER.error("Unable to parse '$hendelse': ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    @Before(value = "execution(* no.nav.bidrag.arbeidsflyt.service.OppgaveService.*(..)) and args(oppgaveSokRequest, journalpostHendelse)")
    fun addCorrelationIdToThread(joinPoint: JoinPoint, oppgaveSokRequest: OppgaveSokRequest, journalpostHendelse: JournalpostHendelse) {
        val correlationId = journalpostHendelse.sporing?.correlationId

        if (correlationId != null) {
            MDC.put(CORRELATION_ID, correlationId)
        } else {
            val unknown = "${oppgaveSokRequest.journalpostId}-${System.currentTimeMillis().toString(16)}"
            LOGGER.warn("Unable to find correlation Id in $journalpostHendelse, using '$unknown'")
            MDC.put(CORRELATION_ID, unknown)
        }
    }

    @After(value = "execution(* no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService.*(..))")
    fun clearCorrelationIdFromBehandleHendelseService(joinPoint: JoinPoint) {
        MDC.clear()
    }

    @After(value = "execution(* no.nav.bidrag.arbeidsflyt.service.OppgaveService.*(..))")
    fun clearCorrelationIdFromOppgaveService(joinPoint: JoinPoint) {
        MDC.clear()
    }
}
