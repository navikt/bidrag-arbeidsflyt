package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.hendelse.Hendelse
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelse
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
            Hendelse.AVVIK_ENDRE_FAGOMRADE -> ferdigstillOppgaverNarFagomradeIkkeErBidEllerFar(journalpostHendelse)
            Hendelse.AVVIK_OVERFOR_TIL_ANNEN_ENHET -> overforOppgaverTilhorende(journalpostHendelse)
            Hendelse.JOURNALFOR_JOURNALPOST -> ferdigstillOppgaver(journalpostHendelse)
            Hendelse.NO_SUPPORT -> LOGGER.warn("bidrag-arbeidsflyt støtter ikke hendelsen '${journalpostHendelse.hendelse}'")
        }
    }

    private fun ferdigstillOppgaverNarFagomradeIkkeErBidEllerFar(journalpostHendelse: JournalpostHendelse) {
        if (journalpostHendelse.erBytteTilInterntFagomrade()) {
            LOGGER.info("Hendelsen ${journalpostHendelse.hendelse} er bytte til et internt fagområde")
        } else {
            ferdigstillOppgaver(journalpostHendelse)
        }
    }

    private fun overforOppgaverTilhorende(journalpostHendelse: JournalpostHendelse) {
        val oppgaveSokRequests = journalpostHendelse.hentOppgaveSokRequestsMedOgUtenPrefix()
        val overforOppgaverA = CompletableFuture.supplyAsync { overforOppgaver(oppgaveSokRequests.first) }
        val overforOppgaverB = CompletableFuture.supplyAsync { overforOppgaver(oppgaveSokRequests.first) }

        CompletableFuture.allOf(overforOppgaverA, overforOppgaverB).get() // overfører oppgaver tilhørende journalpost (med og uten prefix)
    }

    private fun overforOppgaver(oppgaveSokRequest: OppgaveSokRequest) {
        val oppgaveSokResponse = oppgaveConsumer.finnOppgaverForJournalpost(oppgaveSokRequest)

        oppgaveSokResponse.oppgaver.forEach {
            println(it)
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

        oppgaveSokResponse.oppgaver.forEach {
            ferdigstillOppgave(it, oppgaveSokRequest.fagomrade, oppgaveSokRequest.hentEnhetsnummer())
        }
    }

    private fun ferdigstillOppgave(oppgaveData: OppgaveData, fagomrade: String, enhetsnummer: String) {
        oppgaveConsumer.ferdigstillOppgaver(FerdigstillOppgaveRequest(oppgaveData, fagomrade, enhetsnummer))
    }
}
