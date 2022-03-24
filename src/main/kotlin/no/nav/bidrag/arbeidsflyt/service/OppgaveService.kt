package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppdaterOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OverforOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.UpdateOppgaveAfterOpprettRequest
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.model.OppgaveDataForHendelse
import no.nav.bidrag.arbeidsflyt.model.OppgaverForHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OppgaveService(private val oppgaveConsumer: OppgaveConsumer) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(OppgaveService::class.java)
    }

    internal fun finnAapneOppgaverForJournalpost(journalpostId: String): OppgaverForHendelse {
        val oppgaveSokRequest = OppgaveSokRequest(journalpostId)

        return OppgaverForHendelse(
            oppgaveConsumer.finnOppgaverForJournalpost(oppgaveSokRequest).oppgaver
                .map { OppgaveDataForHendelse(it) }
        )
    }

    internal fun finnOppgaverForHendelse(journalpostHendelse: JournalpostHendelse): OppgaverForHendelse {
        val oppgaveSokRequest = OppgaveSokRequest(journalpostHendelse.journalpostId)

        return OppgaverForHendelse(
            oppgaveConsumer.finnOppgaverForJournalpost(oppgaveSokRequest).oppgaver
                .map { OppgaveDataForHendelse(it) }
        )
    }

    internal fun oppdaterOppgaver(oppgaverForHendelse: OppgaverForHendelse, journalpostHendelse: JournalpostHendelse) {
        oppgaverForHendelse.dataForHendelse.forEach {
            oppgaveConsumer.endreOppgave(
                endretAvEnhetsnummer = journalpostHendelse.hentEndretAvEnhetsnummer(),
                patchOppgaveRequest = OppdaterOppgaveRequest(it, journalpostHendelse.aktorId)
            )
        }
    }

    internal fun overforOppgaver(oppgaverForHendelse: OppgaverForHendelse, journalpostHendelse: JournalpostHendelse) {
        oppgaverForHendelse.dataForHendelse.forEach {
            oppgaveConsumer.endreOppgave(
                endretAvEnhetsnummer = journalpostHendelse.hentEndretAvEnhetsnummer(),
                patchOppgaveRequest = OverforOppgaveRequest(it, journalpostHendelse.enhet ?: "na")
            )
        }
    }

    internal fun ferdigstillJournalforingsOppgaver(endretAvEnhetsnummer: String?, oppgaverForHendelse: OppgaverForHendelse) {
        oppgaverForHendelse.hentJournalforingsOppgaver().forEach {
            LOGGER.info("Ferdigstiller oppgave med type ${it.oppgavetype} og journalpostId ${it.journalpostId}")
            oppgaveConsumer.endreOppgave(
                endretAvEnhetsnummer = endretAvEnhetsnummer,
                patchOppgaveRequest = FerdigstillOppgaveRequest(it)
            )
        }
    }

    internal fun opprettJournalforingOppgave(opprettJournalforingsOppgaveRequest: OpprettJournalforingsOppgaveRequest) {
        oppgaveConsumer.opprettOppgave(opprettJournalforingsOppgaveRequest)
    }
}
