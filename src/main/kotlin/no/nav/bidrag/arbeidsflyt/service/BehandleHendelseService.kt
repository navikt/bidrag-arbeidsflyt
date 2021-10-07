package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.Environment.fetchEnv
import no.nav.bidrag.arbeidsflyt.hendelse.HendelseFilter
import no.nav.bidrag.arbeidsflyt.model.Hendelse
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.model.MiljoVariabler.NAIS_APP_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

interface BehandleHendelseService {
    fun behandleHendelse(journalpostHendelse: JournalpostHendelse)
}

@Service
class DefaultBehandleHendelseService(
    private val oppgaveService: OppgaveService, private val hendelseFilter: HendelseFilter
) : BehandleHendelseService {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(DefaultBehandleHendelseService::class.java)
    }

    override fun behandleHendelse(journalpostHendelse: JournalpostHendelse) {
        if (hendelseFilter.stottedeHendelser.contains(journalpostHendelse.hendelse)) {
            when (journalpostHendelse.hentHendelse()) {
                Hendelse.AVVIK_ENDRE_FAGOMRADE -> ferdigstillOppgaverNarFagomradeIkkeErBidEllerFar(journalpostHendelse)
                Hendelse.AVVIK_OVERFOR_TIL_ANNEN_ENHET -> overforOppgaverTilAnnenEnhet(journalpostHendelse)
                Hendelse.JOURNALFOR_JOURNALPOST -> ferdigstillOppgaver(journalpostHendelse)
                Hendelse.OPPRETT_OPPGAVE -> opprettOppgave(journalpostHendelse)
                else -> throw UnsupportedOperationException("Ukjent '${journalpostHendelse.hendelse}'! Sjekk miljøvariabler og implementasjon.")
            }
        } else {
            LOGGER.warn("${fetchEnv(NAIS_APP_NAME)} har ikke støtte for hendelsen '${journalpostHendelse.hendelse}'!")
        }
    }

    private fun ferdigstillOppgaverNarFagomradeIkkeErBidEllerFar(journalpostHendelse: JournalpostHendelse) {
        if (journalpostHendelse.erBytteTilInterntFagomrade()) {
            LOGGER.info("Hendelsen ${journalpostHendelse.hendelse} er bytte til et internt fagområde")
        } else {
            LOGGER.info("${journalpostHendelse.hendelse} er bytte til eksternt fagområde: ${journalpostHendelse.hentFagomradeFraDetaljer()}")
            ferdigstillOppgaver(journalpostHendelse)
        }
    }

    private fun overforOppgaverTilAnnenEnhet(journalpostHendelse: JournalpostHendelse) {
        val fagomrade = journalpostHendelse.hentFagomradeFraDetaljer()

        val overforOppgaverForPrefixetId = CompletableFuture.supplyAsync {
            oppgaveService.overforOppgaver(journalpostHendelse.journalpostId, fagomrade, journalpostHendelse)
        }

        val overforOppgaverUtenPrefixetId = CompletableFuture.supplyAsync {
            oppgaveService.overforOppgaver(journalpostHendelse.hentJournalpostIdUtenPrefix(), fagomrade, journalpostHendelse)
        }

        CompletableFuture.allOf(overforOppgaverForPrefixetId, overforOppgaverUtenPrefixetId)
            .get() // overfører oppgaver tilhørende journalpost (med og uten prefix)
    }

    private fun ferdigstillOppgaver(journalpostHendelse: JournalpostHendelse) {
        val fagomrade = journalpostHendelse.hentFagomradeFraDetaljer()

        val ferdigstillOppgaverForPrefixetId = CompletableFuture.supplyAsync {
            oppgaveService.ferdigstillOppgaver(journalpostHendelse.journalpostId, fagomrade, journalpostHendelse)
        }

        val ferdigstillOppgaverUtenPrefixetId = CompletableFuture.supplyAsync {
            oppgaveService.ferdigstillOppgaver(journalpostHendelse.hentJournalpostIdUtenPrefix(), fagomrade, journalpostHendelse)
        }

        CompletableFuture.allOf(ferdigstillOppgaverForPrefixetId, ferdigstillOppgaverUtenPrefixetId)
            .get() // ferdigstiller oppgaver tilhørende journalpost med og uten prefix på id
    }

    private fun opprettOppgave(journalpostHendelse: JournalpostHendelse){
        oppgaveService.opprettOppgave(journalpostHendelse)
    }
}
