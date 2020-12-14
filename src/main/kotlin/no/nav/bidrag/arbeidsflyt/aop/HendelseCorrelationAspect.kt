package no.nav.bidrag.arbeidsflyt.aop

import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelse
import no.nav.bidrag.commons.CorrelationId
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
@Aspect
class HendelseCorrelationAspect {

    @Before(value = "execution(* no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService.*(..)) and args(journalpostHendelse)")
    fun addCorrelationIdToThread(joinPoint: JoinPoint, journalpostHendelse: JournalpostHendelse) {
        MDC.put("correlationId", CorrelationId.existing(journalpostHendelse.sporing?.correlationId).get())
    }

    @After(value = "execution(* no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService.*(..))")
    fun clearCorrelationId(joinPoint: JoinPoint) {
        MDC.clear()
    }
}
