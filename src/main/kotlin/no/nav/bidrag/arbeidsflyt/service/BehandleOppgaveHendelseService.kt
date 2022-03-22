package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
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
        LOGGER.info("Behandler endret oppgave ${oppgaveHendelse.id} med status ${oppgaveHendelse.status}.")

        if (oppgaveHendelse.hasJournalpostId){
            opprettNyJournalforingOppgaveHvisNodvendig(oppgaveHendelse)
        } else {
            LOGGER.warn("Oppgave ${oppgaveHendelse.id} har ingen journalpostid. Stopper videre behandling.")
        }

        persistenceService.oppdaterOppgaveFraHendelse(oppgaveHendelse)
    }

    fun opprettNyJournalforingOppgaveHvisNodvendig(oppgaveHendelse: OppgaveHendelse){
        if (oppgaveHendelse.erJournalforingOppgave){
            opprettNyOppgaveHvisFerdigstiltOgJournalpostErMottatt(oppgaveHendelse)
        } else {
            opprettNyOppgaveHvisOppgavetypeEndretFraJournalforingTilNoeAnnet(oppgaveHendelse)
        }
    }

    fun opprettNyOppgaveHvisFerdigstiltOgJournalpostErMottatt(oppgaveHendelse: OppgaveHendelse){
        // Antar at Bidrag journlpost lagres med BID- prefix i databasen og oppgaven
        persistenceService.hentJournalpost(oppgaveHendelse.journalpostId!!).ifPresentOrElse({
            val aapneOppgaver = oppgaveService.finnAapneOppgaverForJournalpost(oppgaveHendelse.journalpostId)
            if (oppgaveHendelse.erStatusFerdigstilt() && it.erStatusMottatt() && aapneOppgaver.harIkkeJournalforingsoppgave()) {
                LOGGER.info("Oppgave ${oppgaveHendelse.id} ble lukket når tilhørende journalpost ${oppgaveHendelse.journalpostId} fortsatt har status MOTTATT. Oppretter ny oppgave")
                opprettJournalforingOppgave(oppgaveHendelse)
            }
        }, {LOGGER.warn("Fant ingen tilhørende journalpost for oppgave ${oppgaveHendelse.id} med journalpostid ${oppgaveHendelse.journalpostId}. Stopper videre behandling.")})
    }

    fun opprettNyOppgaveHvisOppgavetypeEndretFraJournalforingTilNoeAnnet(oppgaveHendelse: OppgaveHendelse){
        persistenceService.hentOppgave(oppgaveHendelse.id).ifPresent {
           val aapneOppgaver = oppgaveService.finnAapneOppgaverForJournalpost(oppgaveHendelse.journalpostId!!)
           if (it.erJournalforingOppgave() && !oppgaveHendelse.erJournalforingOppgave && aapneOppgaver.harIkkeJournalforingsoppgave()) {
               LOGGER.info("Oppgavetype på oppgave ${oppgaveHendelse.id} ble endret fra Journalføring (JFR) til ${oppgaveHendelse.oppgavetype}. Oppretter ny oppgave med type JFR.")
               opprettJournalforingOppgave(oppgaveHendelse)
           }
       }
    }

    fun opprettJournalforingOppgave(oppgaveHendelse: OppgaveHendelse){
        if (featureToggle.isFeatureEnabled(FeatureToggle.Feature.OPPRETT_OPPGAVE)){
            oppgaveService.opprettJournalforingOppgave(OpprettJournalforingsOppgaveRequest(oppgaveHendelse))
        } else {
            LOGGER.info("Feature ${FeatureToggle.Feature.OPPRETT_OPPGAVE} er ikke skrudd på. Oppretter ikke oppgave")
        }

    }
}