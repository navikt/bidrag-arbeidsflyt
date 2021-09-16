package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.Environment.fetchEnv
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
        if (hendelseFilter.kanUtfore(journalpostHendelse.hentHendelse())) {
            when (journalpostHendelse.hentHendelse()) {
                Hendelse.AVVIK_ENDRE_FAGOMRADE -> ferdigstillOppgaverNarFagomradeIkkeErBidEllerFar(journalpostHendelse)
                Hendelse.AVVIK_OVERFOR_TIL_ANNEN_ENHET -> overforOppgaverTilAnnenEnhet(journalpostHendelse)
                Hendelse.JOURNALFOR_JOURNALPOST -> ferdigstillOppgaver(journalpostHendelse)
                Hendelse.NO_SUPPORT -> LOGGER.warn("${fetchEnv(NAIS_APP_NAME)} støtter ikke behandling av hendelsen '${journalpostHendelse.hendelse}'")
            }
        } else {
            LOGGER.warn("${fetchEnv(NAIS_APP_NAME)} støtter ikke hendelsen '${journalpostHendelse.hendelse}' i et nais cluster")
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
}

interface HendelseFilter {
    fun kanUtfore(hendelse: Hendelse): Boolean
}

open class DefaultHendelseFilter(private val stottedeHendelser: List<Hendelse> = emptyList()) : HendelseFilter {
    override fun kanUtfore(hendelse: Hendelse) = stottedeHendelser.contains(hendelse)
}
