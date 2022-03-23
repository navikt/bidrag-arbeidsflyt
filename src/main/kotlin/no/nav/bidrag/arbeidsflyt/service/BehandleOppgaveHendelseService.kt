package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.arbeidsflyt.utils.FeatureToggle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Optional
import javax.transaction.Transactional

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

    @Transactional
    fun behandleOpprettOppgave(oppgaveHendelse: OppgaveHendelse){
        persistenceService.lagreOppgaveFraHendelse(oppgaveHendelse)
    }

    @Transactional
    fun behandleEndretOppgave(oppgaveHendelse: OppgaveHendelse){
        LOGGER.info("Behandler endret oppgave ${oppgaveHendelse.id} med status ${oppgaveHendelse.status} endret av ${oppgaveHendelse.endretAv}.")

        val existingOppgave = persistenceService.hentOppgaveDetached(oppgaveHendelse.id)
        persistenceService.oppdaterOppgaveFraHendelse(oppgaveHendelse)

        if (oppgaveHendelse.hasJournalpostId){
            opprettNyJournalforingOppgaveHvisNodvendig(oppgaveHendelse, existingOppgave)
        } else {
            LOGGER.warn("Oppgave ${oppgaveHendelse.id} har ingen journalpostid. Stopper videre behandling.")
        }

    }

    fun opprettNyJournalforingOppgaveHvisNodvendig(oppgaveHendelse: OppgaveHendelse, existingOppgave: Optional<Oppgave>){
        // Antar at Bidrag journlpost lagres med BID- prefix i databasen og oppgaven
        persistenceService.hentJournalpost(oppgaveHendelse.journalpostId!!)
            .ifPresentOrElse({ journalpost -> opprettNyOpgaveHvisJournalpostErMottattOgHarIngenJournalforingsoppgaver(journalpost, oppgaveHendelse, existingOppgave)},
                {LOGGER.warn("Fant ingen tilhørende journalpost for oppgave ${oppgaveHendelse.id} med journalpostid ${oppgaveHendelse.journalpostId}. Stopper videre behandling.")})
    }

    fun opprettNyOpgaveHvisJournalpostErMottattOgHarIngenJournalforingsoppgaver(journalpost: Journalpost, oppgaveHendelse: OppgaveHendelse, existingOppgave: Optional<Oppgave>){
        if (journalpost.erStatusMottatt() && harIkkeAapneJournalforingsoppgaver(oppgaveHendelse.journalpostId!!)){
            opprettNyJournalforingsoppgave(oppgaveHendelse, existingOppgave)
        }
    }

    fun opprettNyJournalforingsoppgave(oppgaveHendelse: OppgaveHendelse, existingOppgave: Optional<Oppgave>){
        val previousOppgave = existingOppgave.orElse(null)
        if (oppgaveHendelse.erStatusFerdigstilt() && oppgaveHendelse.erJournalforingOppgave) {
            LOGGER.info("Oppgave ${oppgaveHendelse.id} ble lukket når tilhørende journalpost ${oppgaveHendelse.journalpostId} fortsatt har status MOTTATT. Oppretter ny oppgave")
            opprettJournalforingOppgave(oppgaveHendelse)
        } else if (!oppgaveHendelse.erJournalforingOppgave && previousOppgave?.erJournalforingOppgave() == true) {
            LOGGER.info("Oppgavetype på oppgave ${oppgaveHendelse.id} ble endret fra Journalføring (JFR) til ${oppgaveHendelse.oppgavetype} når tilhørende journalpost ${oppgaveHendelse.journalpostId} fortsatt har status MOTTATT. Oppretter ny oppgave med type JFR.")
            opprettJournalforingOppgave(oppgaveHendelse)
        }
    }

    fun harIkkeAapneJournalforingsoppgaver(journalpostId: String): Boolean{
         // TODO: Check by querying database instead of using api. Needs time to populate database
        val aapneOppgaveDB = persistenceService.finnAapneJournalforingsOppgaver(journalpostId)
        val aapneOppgaveAPI = oppgaveService.finnAapneOppgaverForJournalpost(journalpostId)

        LOGGER.info("Åpne journalføringsoppgaver i databasen ${aapneOppgaveDB.size}, fra api ${aapneOppgaveAPI.hentJournalforingsOppgaver().size} med harIkkeJournalforingsoppgave: ${aapneOppgaveAPI.harIkkeJournalforingsoppgave()}")
        if (aapneOppgaveDB.size != aapneOppgaveAPI.hentJournalforingsOppgaver().size){
            LOGGER.warn("Det var ulik resultat på antall åpne journalføringsoppgaver i databasen (${aapneOppgaveDB.size}) og fra api (${aapneOppgaveAPI.hentJournalforingsOppgaver().size})")
        }
        return aapneOppgaveAPI.harIkkeJournalforingsoppgave()
    }



    fun opprettJournalforingOppgave(oppgaveHendelse: OppgaveHendelse){
        if (featureToggle.isFeatureEnabled(FeatureToggle.Feature.OPPRETT_OPPGAVE)){
            oppgaveService.opprettJournalforingOppgave(OpprettJournalforingsOppgaveRequest(oppgaveHendelse))
        } else {
            LOGGER.info("Feature ${FeatureToggle.Feature.OPPRETT_OPPGAVE} er ikke skrudd på. Oppretter ikke oppgave")
        }

    }
}