package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OpprettOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OverforOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.UpdateOppgaveWithJournalpostPrefixRequest
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import org.springframework.stereotype.Service

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
        oppgaveSokResponse.oppgaver.forEach { ferdigstillOppgave(it, oppgaveSokRequest.fagomrade, journalforendeEnhet) }
    }

    private fun ferdigstillOppgave(oppgaveData: OppgaveData, fagomrade: String, enhetsnummer: String) {
        oppgaveConsumer.endreOppgave(FerdigstillOppgaveRequest(oppgaveData, fagomrade, enhetsnummer))
    }

    internal fun opprettOppgave(journalpostHendelse: JournalpostHendelse) {
        val opprettOppgaveRequest = OpprettOppgaveRequest(
            journalpostId = journalpostHendelse.hentJournalpostIdUtenPrefix(),
            aktoerId = journalpostHendelse.hentAktoerId(),
            tema = journalpostHendelse.hentFagomradeFraDetaljer()
        )
        // Opprett oppgave doesn`t support journalpostId with prefix. Have to patch oppgave after opprett
        val oppgaveData = oppgaveConsumer.opprettOppgave(opprettOppgaveRequest)
        oppgaveConsumer.endreOppgave(UpdateOppgaveWithJournalpostPrefixRequest(oppgaveData, journalpostHendelse.journalpostId))
    }
}