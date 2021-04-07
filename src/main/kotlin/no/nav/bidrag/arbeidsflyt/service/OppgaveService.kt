package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OverforOppgaveRequest
import org.springframework.stereotype.Service

@Service
class OppgaveService(private val oppgaveConsumer: OppgaveConsumer) {

    internal fun overforOppgaver(oppgaveSokRequest: OppgaveSokRequest, nyJournalforendeEnhet: String) {
        val oppgaveSokResponse = oppgaveConsumer.finnOppgaverForJournalpost(oppgaveSokRequest)

        oppgaveSokResponse.oppgaver.forEach { overforOppgave(OverforOppgaveRequest(it, nyJournalforendeEnhet)) }
    }

    private fun overforOppgave(overforOppgaveRequest: OverforOppgaveRequest) {
        oppgaveConsumer.endreOppgave(overforOppgaveRequest)
    }

    internal fun ferdigstillOppgaver(oppgaveSokRequest: OppgaveSokRequest, journalforendeEnhet: String) {
        val oppgaveSokResponse = oppgaveConsumer.finnOppgaverForJournalpost(oppgaveSokRequest)

        oppgaveSokResponse.oppgaver.forEach { ferdigstillOppgave(it, oppgaveSokRequest.fagomrade, journalforendeEnhet) }
    }

    private fun ferdigstillOppgave(oppgaveData: OppgaveData, fagomrade: String, enhetsnummer: String) {
        oppgaveConsumer.endreOppgave(FerdigstillOppgaveRequest(oppgaveData, fagomrade, enhetsnummer))
    }
}