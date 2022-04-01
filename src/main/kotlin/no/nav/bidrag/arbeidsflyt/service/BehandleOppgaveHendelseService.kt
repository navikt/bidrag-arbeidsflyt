package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.utils.FeatureToggle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BehandleOppgaveHendelseService(
    var persistenceService: PersistenceService,
    var oppgaveService: OppgaveService,
    var featureToggle: FeatureToggle
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BehandleOppgaveHendelseService::class.java)
    }

    fun behandleEndretOppgave(oppgaveHendelse: OppgaveHendelse){
        LOGGER.info("Behandler endret oppgave ${oppgaveHendelse.id} med status ${oppgaveHendelse.status} endret av ${oppgaveHendelse.endretAv}.")

        if (oppgaveHendelse.hasJournalpostId){
            opprettNyJournalforingOppgaveHvisNodvendig(oppgaveHendelse)
        } else {
            LOGGER.debug("Oppgave ${oppgaveHendelse.id} har ingen journalpostid. Stopper videre behandling.")
        }
    }

    fun opprettNyJournalforingOppgaveHvisNodvendig(oppgaveHendelse: OppgaveHendelse){
        if (oppgaveHendelse.erAapenJournalforingsoppgave()){
            LOGGER.info("Oppgave ${oppgaveHendelse.id} er åpen journalføringsoppgave. Videre behandling er ikke nødvendig.")
            return
        }
        // Antar at Bidrag journlpost lagres med BID- prefix i databasen og oppgaven
        persistenceService.hentJournalpostMedStatusMottatt(oppgaveHendelse.journalpostId!!)
            .ifPresentOrElse({
                run {
                    if (harIkkeAapneJournalforingsoppgaver(oppgaveHendelse.journalpostId)) {
                        LOGGER.info("Journalpost ${oppgaveHendelse.journalpostId} har status MOTTATT men har ingen journalføringsoppgave. Oppretter ny journalføringsoppgave")
                        opprettJournalforingOppgaveFraHendelse(oppgaveHendelse)
                    }
                }
            },
            {LOGGER.info("Journalpost ${oppgaveHendelse.journalpostId} som tilhører oppgave ${oppgaveHendelse.id} har ikke status MOTTATT. Stopper videre behandling.")})
    }

    fun harIkkeAapneJournalforingsoppgaver(journalpostId: String): Boolean {
        val aapneOppgaveAPI = oppgaveService.finnAapneOppgaverForJournalpost(journalpostId)
        return aapneOppgaveAPI.harIkkeJournalforingsoppgave()
    }

    fun opprettJournalforingOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse){
        if (featureToggle.isFeatureEnabled(FeatureToggle.Feature.OPPRETT_OPPGAVE)){
            oppgaveService.opprettJournalforingOppgave(OpprettJournalforingsOppgaveRequest(oppgaveHendelse))
        } else {
            LOGGER.info("Feature ${FeatureToggle.Feature.OPPRETT_OPPGAVE} er ikke skrudd på. Oppretter ikke oppgave")
        }

    }
}