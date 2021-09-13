package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OverforOppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
private val LOGGER = LoggerFactory.getLogger(OppgaveService::class.java)

@Service
class OppgaveService(private val oppgaveConsumer: OppgaveConsumer) {

    internal fun overforOppgaver(journalpostId: String, fagomrade: String, journalpostHendelse: JournalpostHendelse) {
        val oppgaveSokRequest = OppgaveSokRequest(journalpostId, fagomrade)
        val oppgaveSokResponse = oppgaveConsumer.finnOppgaverForJournalpost(oppgaveSokRequest)
        val nyJournalforendeEnhet = journalpostHendelse.hentNyttJournalforendeEnhetsnummer()

        oppgaveSokResponse.oppgaver.forEach { overforOppgave(OverforOppgaveRequest(it, nyJournalforendeEnhet)) }
    }

    private fun overforOppgave(overforOppgaveRequest: OverforOppgaveRequest) {
        oppgaveConsumer.endreOppgave(overforOppgaveRequest)
    }

    internal fun ferdigstillOppgaver(journalpostId: String, fagomrade: String, journalpostHendelse: JournalpostHendelse) {
        val oppgaveSokRequest = OppgaveSokRequest(journalpostId, fagomrade)
        val oppgaveSokResponse = oppgaveConsumer.finnOppgaverForJournalpost(oppgaveSokRequest)
        val journalforendeEnhet = journalpostHendelse.hentEnhetsnummer()
        LOGGER.info("Oppgave ferdigstillOppgaver")
        oppgaveSokResponse.oppgaver.forEach { ferdigstillOppgave(it, oppgaveSokRequest.fagomrade, journalforendeEnhet) }
    }

    private fun ferdigstillOppgave(oppgaveData: OppgaveData, fagomrade: String, enhetsnummer: String) {
        oppgaveConsumer.endreOppgave(FerdigstillOppgaveRequest(oppgaveData, fagomrade, enhetsnummer))
    }
}