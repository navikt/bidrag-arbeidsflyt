package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.OppdaterOppgaveFraHendelse
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandleOppgaveHendelseService(
    var persistenceService: PersistenceService,
    var oppgaveService: OppgaveService,
    var journalpostService: JournalpostService,
    var applicationContext: ApplicationContext,
    var arbeidsfordelingService: OrganisasjonService
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BehandleOppgaveHendelseService::class.java)
    }

    fun behandleOpprettOppgave(oppgave: OppgaveHendelse) {
        behandleOpprettOppgave(oppgaveService.hentOppgave(oppgave.id))
    }

    @Transactional
    fun behandleEndretOppgave(oppgave: OppgaveHendelse) {
        behandleEndretOppgave(oppgaveService.hentOppgave(oppgave.id))
    }

    fun behandleOpprettOppgave(oppgave: OppgaveData) {
        if (oppgave.erJournalforingOppgave) {
            persistenceService.lagreJournalforingsOppgaveFraHendelse(oppgave)
        }

        if (oppgave.hasJournalpostId) {
            oppdaterOppgaveFraHendelse(oppgave)
                .overforOppgaveTilJournalforendeEnhetHvisTilhorerJournalforende()
                .endreVurderDokumentOppgaveTypeTilJournalforingHvisJournalpostMottatt()
                .overforReturoppgaveTilFarskapHvisJournalpostHarTemaFAR()
                .utfor()
        } else {
            oppdaterOppgaveFraHendelse(oppgave)
                .overforOppgaveTilJournalforendeEnhetHvisTilhorerJournalforende()
                .endreVurderDokumentOppgaveTypeTilVurderHenvendelseHvisIngenJournalpost()
                .utfor()
        }
    }

    @Transactional
    fun behandleEndretOppgave(oppgave: OppgaveData) {
        if (oppgave.hasJournalpostId) {
            oppdaterOppgaveFraHendelse(oppgave)
                .overforOppgaveTilJournalforendeEnhetHvisTilhorerJournalforende()
                .endreVurderDokumentOppgaveTypeTilJournalforingHvisJournalpostMottatt()
                .utfor()
            opprettNyJournalforingOppgaveHvisNodvendig(oppgave)
        } else {
            LOGGER.debug("Oppgave ${oppgave.id} har ingen journalpostid. Stopper videre behandling.")
        }

        persistenceService.oppdaterEllerSlettOppgaveMetadataFraHendelse(oppgave)
    }

    fun oppdaterOppgaveFraHendelse(oppgave: OppgaveData): OppdaterOppgaveFraHendelse {
        return applicationContext.getBean(OppdaterOppgaveFraHendelse::class.java)
            .behandle(oppgave)
    }
    fun opprettNyJournalforingOppgaveHvisNodvendig(oppgave: OppgaveData) {
        if (!oppgave.tilhorerFagpost && (oppgave.erAvsluttetJournalforingsoppgave() || erOppgavetypeEndretFraJournalforingTilAnnet(oppgave))) {
            opprettNyJournalforingOppgaveHvisJournalpostMottatt(oppgave)
        }
    }

    fun opprettNyJournalforingOppgaveHvisJournalpostMottatt(oppgave: OppgaveData) {
        if (harIkkeAapneJournalforingsoppgaver(oppgave.journalpostId!!)) {
            journalpostService.hentJournalpostMedStatusMottatt(oppgave.journalpostIdMedPrefix!!)
                .takeIf { it?.erBidragFagomrade == true }
                ?.run {
                    LOGGER.info("Journalpost ${oppgave.journalpostId} har status MOTTATT men har ingen journalføringsoppgave. Oppretter ny journalføringsoppgave")
                    opprettJournalforingOppgaveFraHendelse(oppgave)
                } ?: run { LOGGER.info("Journalpost ${oppgave.journalpostId} som tilhører oppgave ${oppgave.id} har ikke status MOTTATT. Stopper videre behandling.") }
        }
    }

    fun harIkkeAapneJournalforingsoppgaver(journalpostId: String): Boolean {
        val aapneOppgaveAPI = oppgaveService.finnAapneJournalforingOppgaverForJournalpost(journalpostId)
        return aapneOppgaveAPI.harIkkeJournalforingsoppgave()
    }

    fun opprettJournalforingOppgaveFraHendelse(oppgave: OppgaveData) {
        val tildeltEnhetsnr = oppgave.tildeltEnhetsnr ?: arbeidsfordelingService.hentArbeidsfordeling(oppgave.hentIdent).verdi
        oppgaveService.opprettJournalforingOppgave(OpprettJournalforingsOppgaveRequest(oppgave, tildeltEnhetsnr))
    }

    fun erOppgavetypeEndretFraJournalforingTilAnnet(oppgave: OppgaveData): Boolean {
        if (oppgave.erJournalforingOppgave) {
            return false
        }

        val prevOppgaveState = persistenceService.hentJournalforingOppgave(oppgave.id)
        if (prevOppgaveState?.erJournalforingOppgave() == true) {
            LOGGER.info("Oppgavetype for oppgave ${oppgave.id} er endret fra JFR til ${oppgave.oppgavetype}")
            return true
        }

        return false
    }
}
