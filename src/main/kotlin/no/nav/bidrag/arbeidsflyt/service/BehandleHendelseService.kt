package no.nav.bidrag.arbeidsflyt.service

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
class DefaultBehandleHendelseService(private val oppgaveService: OppgaveService) : BehandleHendelseService {

    override fun behandleHendelse(journalpostHendelse: JournalpostHendelse) {
        LOGGER.info("Behandler journalpostHendelse: $journalpostHendelse")

        when (journalpostHendelse.hentHendelse()) {
            Hendelse.AVVIK_ENDRE_FAGOMRADE -> ferdigstillOppgaverNarFagomradeIkkeErBidEllerFar(journalpostHendelse)
            Hendelse.AVVIK_OVERFOR_TIL_ANNEN_ENHET -> overforOppgaverTilAnnenEnhet(journalpostHendelse)
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

    private fun overforOppgaverTilAnnenEnhet(journalpostHendelse: JournalpostHendelse) {
        val fagomrade = journalpostHendelse.hentFagomradeFraId()
        val nyJournalforendeEnhet = journalpostHendelse.hentNyttJournalforendeEnhetsnummer()

        val overforOppgaverForPrefixetId = CompletableFuture.supplyAsync {
            oppgaveService.overforOppgaver(OppgaveSokRequest(journalpostHendelse.journalpostId, fagomrade), nyJournalforendeEnhet)
        }

        val overforOppgaverUtenPrefixetId = CompletableFuture.supplyAsync {
            oppgaveService.overforOppgaver(OppgaveSokRequest(journalpostHendelse.hentJournalpostIdUtenPrefix(), fagomrade), nyJournalforendeEnhet)
        }

        CompletableFuture.allOf(overforOppgaverForPrefixetId, overforOppgaverUtenPrefixetId)
            .get() // overfører oppgaver tilhørende journalpost (med og uten prefix)
    }

    private fun ferdigstillOppgaver(journalpostHendelse: JournalpostHendelse) {
        val fagomrade = journalpostHendelse.hentFagomradeFraId()
        val journalforendeEnhet = journalpostHendelse.hentEnhetsnummer()

        val ferdigstillOppgaverForPrefixetId = CompletableFuture.supplyAsync {
            oppgaveService.ferdigstillOppgaver(OppgaveSokRequest(journalpostHendelse.journalpostId, fagomrade), journalforendeEnhet)
        }

        val ferdigstillOppgaverUtenPrefixetId = CompletableFuture.supplyAsync {
            oppgaveService.ferdigstillOppgaver(OppgaveSokRequest(journalpostHendelse.hentJournalpostIdUtenPrefix(), fagomrade), journalforendeEnhet)
        }

        CompletableFuture.allOf(ferdigstillOppgaverForPrefixetId, ferdigstillOppgaverUtenPrefixetId)
            .get() // ferdigstiller oppgaver tilhørende journalpost med og uten prefix på id
    }
}
