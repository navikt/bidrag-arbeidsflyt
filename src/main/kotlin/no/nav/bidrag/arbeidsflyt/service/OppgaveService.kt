package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppdaterOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OpprettOppgaveRequest
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

    internal fun opprettOppgave(journalpostHendelse: JournalpostHendelse) {
        val opprettOppgaveRequest = OpprettOppgaveRequest(
            journalpostId = journalpostHendelse.hentJournalpostIdUtenPrefix(),
            aktoerId = journalpostHendelse.aktorId,
            tema = journalpostHendelse.fagomrade,
            tildeltEnhetsnr = journalpostHendelse.enhet
        )

        val oppgaveData = oppgaveConsumer.opprettOppgave(opprettOppgaveRequest)

        // Opprett oppgave doesn`t support journalpostId with prefix. Have to patch oppgave after opprett
        if (journalpostHendelse.harJournalpostIdBIDPrefix()) {
            oppgaveConsumer.endreOppgave(
                endretAvEnhetsnummer = journalpostHendelse.hentEndretAvEnhetsnummer(),
                patchOppgaveRequest = UpdateOppgaveAfterOpprettRequest(oppgaveData, journalpostHendelse.journalpostId)
            )
        }
    }
}
