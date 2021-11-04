package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OpprettOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OverforOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.UpdateOppgaveAfterOpprettRequest
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.model.OppgaveDataForHendelse
import no.nav.bidrag.arbeidsflyt.model.OppgaverForHendelse
import org.springframework.stereotype.Service

@Service
class OppgaveService(private val oppgaveConsumer: OppgaveConsumer) {

    internal fun finnOppgaverForHendelse(journalpostHendelse: JournalpostHendelse): OppgaverForHendelse {
        val oppgaveSokRequest = OppgaveSokRequest(journalpostHendelse.journalpostId)

        return OppgaverForHendelse(
            oppgaveConsumer.finnOppgaverForJournalpost(oppgaveSokRequest).oppgaver
                .map { OppgaveDataForHendelse(it) }
        )
    }

    internal fun overforOppgaver(oppgaverForHendelse: OppgaverForHendelse, journalpostHendelse: JournalpostHendelse) {
        oppgaverForHendelse.dataForHendelse.forEach { oppgaveConsumer.endreOppgave(OverforOppgaveRequest(it, journalpostHendelse.enhet ?: "na")) }
    }

    internal fun ferdigstillOppgaver(oppgaverForJournalpost: OppgaverForHendelse) {
        oppgaverForJournalpost.dataForHendelse.forEach {
            oppgaveConsumer.endreOppgave(FerdigstillOppgaveRequest(it))
        }
    }

    internal fun opprettOppgave(journalpostHendelse: JournalpostHendelse) {
        val opprettOppgaveRequest = OpprettOppgaveRequest(
            journalpostId = journalpostHendelse.hentJournalpostIdUtenPrefix(),
            aktoerId = journalpostHendelse.aktorId,
            tema = journalpostHendelse.fagomrade,
            tildeltEnhetsnr = journalpostHendelse.enhet
        )

        val oppgaveData = oppgaveConsumer.opprettOppgave(opprettOppgaveRequest)

        // Opprett oppgave doesn`t support journalpostId with prefix. Have to patch oppgave after opprett
        if (journalpostHendelse.harJournalpostIdPrefix()) {
            oppgaveConsumer.endreOppgave(UpdateOppgaveAfterOpprettRequest(oppgaveData, journalpostHendelse.journalpostId))
        }
    }
}
