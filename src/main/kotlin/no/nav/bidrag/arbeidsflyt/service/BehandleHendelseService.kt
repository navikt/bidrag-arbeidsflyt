package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

private val LOGGER = LoggerFactory.getLogger(DefaultBehandleHendelseService::class.java)

interface BehandleHendelseService {
    fun behandleHendelse(journalpostHendelse: JournalpostHendelse)
}

@Service
class DefaultBehandleHendelseService(private val oppgaveConsumer: OppgaveConsumer) : BehandleHendelseService {

    override fun behandleHendelse(journalpostHendelse: JournalpostHendelse) {
        LOGGER.info("Behandler journalpostHendelse: $journalpostHendelse")

        when (journalpostHendelse.hentHendelse()) {
            JournalpostHendelser.AVVIK_ENDRE_FAGOMRADE -> ferdigstillOppgaverNarFagomradeIkkeErBidEllerFar(journalpostHendelse)
            JournalpostHendelser.JOURNALFOR_JOURNALPOST -> ferdigstillOppgaver(journalpostHendelse)
            JournalpostHendelser.NO_SUPPORT -> LOGGER.warn("bidrag-arbeidsflyt støtter ikke hendelsen '${journalpostHendelse.hendelse}'")
        }
    }

    private fun ferdigstillOppgaverNarFagomradeIkkeErBidEllerFar(journalpostHendelse: JournalpostHendelse) {
        if (journalpostHendelse.erBytteTilInterntFagomrade()) {
            LOGGER.info("Hendelsen ${journalpostHendelse.hendelse} er bytte til et internt fagområde")
        } else {
            ferdigstillOppgaver(journalpostHendelse)
        }
    }

    private fun ferdigstillOppgaver(journalpostHendelse: JournalpostHendelse) {
        val oppgaveSokRequests = journalpostHendelse.hentOppgaveSokRequestsMedOgUtenPrefix()
        val ferdigstillOppgaver = CompletableFuture.supplyAsync { ferdigstillOppgaver(oppgaveSokRequests.first) }
        val ferdigstillAndreOppgaver = CompletableFuture.supplyAsync { ferdigstillOppgaver(oppgaveSokRequests.second) }

        CompletableFuture.allOf(ferdigstillOppgaver, ferdigstillAndreOppgaver).get() // ferdigstiller oppgaver for med og uten prefix på id
    }

    private fun ferdigstillOppgaver(oppgaveSokRequest: OppgaveSokRequest) {
        val oppgaveSokResponse = oppgaveConsumer.finnOppgaverForJournalpost(oppgaveSokRequest)

        oppgaveSokResponse?.oppgaver?.forEach {
            ferdigstillOppgave(it, oppgaveSokRequest)
        }
    }

    private fun ferdigstillOppgave(oppgaveData: OppgaveData, oppgaveSokRequest: OppgaveSokRequest) {
        oppgaveConsumer.ferdigstillOppgaver(FerdigstillOppgaveRequest(oppgaveData, oppgaveSokRequest.fagomrade, oppgaveSokRequest.hentEnhetsnummer()))
    }
}
