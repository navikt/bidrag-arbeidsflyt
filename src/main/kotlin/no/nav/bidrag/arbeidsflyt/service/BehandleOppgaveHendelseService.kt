package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandleOppgaveHendelseService(
    var persistenceService: PersistenceService,
    var oppgaveService: OppgaveService,
    var arbeidsfordelingService: ArbeidsfordelingService
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BehandleOppgaveHendelseService::class.java)
    }

    fun behandleOpprettOppgave(oppgaveHendelse: OppgaveHendelse){
        persistenceService.lagreJournalforingsOppgaveFraHendelse(oppgaveHendelse)
    }

    @Transactional
    fun behandleEndretOppgave(oppgaveHendelse: OppgaveHendelse){
        if (oppgaveHendelse.hasJournalpostId){
            opprettNyJournalforingOppgaveHvisNodvendig(oppgaveHendelse)
        } else {
            LOGGER.debug("Oppgave ${oppgaveHendelse.id} har ingen journalpostid. Stopper videre behandling.")
        }

        persistenceService.oppdaterEllerSlettOppgaveMetadataFraHendelse(oppgaveHendelse)
    }

    fun opprettNyJournalforingOppgaveHvisNodvendig(oppgaveHendelse: OppgaveHendelse) {
        if (oppgaveHendelse.erAvsluttetJournalforingsoppgave() || erOppgavetypeEndretFraJournalforingTilAnnet(oppgaveHendelse)) {
            opprettNyJournalforingOppgaveHvisJournalpostMottatt(oppgaveHendelse)
        }
    }

    fun opprettNyJournalforingOppgaveHvisJournalpostMottatt(oppgaveHendelse: OppgaveHendelse){
        // Antar at Bidrag journlpost lagres med BID- prefix i databasen og oppgaven
        persistenceService.hentJournalpostMedStatusMottatt(oppgaveHendelse.journalpostId!!)
            .ifPresentOrElse({
                run {
                    if (harIkkeAapneJournalforingsoppgaver(oppgaveHendelse.journalpostId)) {
                        LOGGER.info("Journalpost ${oppgaveHendelse.journalpostId} har status MOTTATT men har ingen journalføringsoppgave. Oppretter ny journalføringsoppgave")
                        opprettJournalforingOppgaveFraHendelse(oppgaveHendelse, it)
                    }
                }
            },
            { LOGGER.info("Journalpost ${oppgaveHendelse.journalpostId} som tilhører oppgave ${oppgaveHendelse.id} har ikke status MOTTATT. Stopper videre behandling.") })
    }

    fun harIkkeAapneJournalforingsoppgaver(journalpostId: String): Boolean {
        val aapneOppgaveAPI = oppgaveService.finnAapneOppgaverForJournalpost(journalpostId)
        return aapneOppgaveAPI.harIkkeJournalforingsoppgave()
    }

    fun opprettJournalforingOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse, journalpost: Journalpost){
        val journalforendeEnhet = oppgaveHendelse.tildeltEnhetsnr ?: arbeidsfordelingService.hentArbeidsfordeling(oppgaveHendelse.hentIdent)
        oppgaveService.opprettJournalforingOppgave(OpprettJournalforingsOppgaveRequest(oppgaveHendelse, journalforendeEnhet))
    }

    fun erOppgavetypeEndretFraJournalforingTilAnnet(oppgaveHendelse: OppgaveHendelse): Boolean {
        if(oppgaveHendelse.erJournalforingOppgave){
            return false
        }

        val prevOppgaveState = persistenceService.hentJournalforingOppgave(oppgaveHendelse.id)
        if (prevOppgaveState.isPresent && prevOppgaveState.get().erJournalforingOppgave()){
            LOGGER.info("Oppgavetype for oppgave ${oppgaveHendelse.id} er endret fra JFR til ${oppgaveHendelse.oppgavetype}")
            return true
        }

        return false

    }
}