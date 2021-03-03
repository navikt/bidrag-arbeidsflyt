package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.lang.IllegalStateException
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
            JournalpostHendelser.JOURNALFOR_JOURNALPOST -> ferdigstillOppgaver(journalpostHendelse)
            JournalpostHendelser.NO_SUPPORT -> LOGGER.warn("bidrag-arbeidsflyt støtter ikke hendelsen '${journalpostHendelse.hendelse}'")
        }
    }

    private fun ferdigstillOppgaver(journalpostHendelse: JournalpostHendelse) {
        val oppgaveSokRequests = journalpostHendelse.hentOppgaveSokRequestsMedOgUtenPrefix()
        val fremtiden = CompletableFuture.supplyAsync { ferdigstillOppgaver(oppgaveSokRequests.first) }
        val enAnnenFremtid = CompletableFuture.supplyAsync { ferdigstillOppgaver(oppgaveSokRequests.second) }

        CompletableFuture.allOf(fremtiden, enAnnenFremtid).get()
    }

    private fun ferdigstillOppgaver(oppgaveSokRequest: OppgaveSokRequest) {
        val oppgaveSokResponse = oppgaveConsumer.finnOppgaverForJournalpost(oppgaveSokRequest)

        oppgaveSokResponse?.oppgaver?.forEach { ferdigstillOppgave(
            it,
            oppgaveSokRequest,
            oppgaveSokRequest.enhetsnummer ?: throw IllegalStateException("Mangler hendelsedata: detaljer.enhetsnummer")
        ) }
    }

    private fun ferdigstillOppgave(oppgaveData: OppgaveData, oppgaveSokRequest: OppgaveSokRequest, enhetsnummer: String) {
        oppgaveConsumer.ferdigstillOppgaver(FerdigstillOppgaveRequest(oppgaveData, oppgaveSokRequest.fagomrade, enhetsnummer))
    }
}
