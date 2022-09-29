package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.dto.OppdaterOppgave
import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.dto.OppgaveType
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

var ENHET_FAGPOST="2950"
var ENHET_IT_AVDELINGEN="2990"
@Service
class BehandleOppgaveHendelseService(
    var persistenceService: PersistenceService,
    var oppgaveService: OppgaveService,
    var arbeidsfordelingService: ArbeidsfordelingService,
    var journalpostService: JournalpostService
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BehandleOppgaveHendelseService::class.java)
    }

    fun behandleOpprettOppgave(oppgaveHendelse: OppgaveHendelse){
        if (oppgaveHendelse.erJournalforingOppgave){
            persistenceService.lagreJournalforingsOppgaveFraHendelse(oppgaveHendelse)
        }

        if (oppgaveHendelse.hasJournalpostId){
            endreVurderDokumentOppgaveTilJournalforingHvisJournalpostMottatt(oppgaveHendelse)
        }
    }

    @Transactional
    fun behandleEndretOppgave(oppgaveHendelse: OppgaveHendelse){
        if (oppgaveHendelse.hasJournalpostId){
            overforOppgaveTilJournalforendeHvisIkkeJournalforende(oppgaveHendelse)
            endreVurderDokumentOppgaveTilJournalforingHvisJournalpostMottatt(oppgaveHendelse)
            opprettNyJournalforingOppgaveHvisNodvendig(oppgaveHendelse)
        } else {
            LOGGER.debug("Oppgave ${oppgaveHendelse.id} har ingen journalpostid. Stopper videre behandling.")
        }

        persistenceService.oppdaterEllerSlettOppgaveMetadataFraHendelse(oppgaveHendelse)
    }

    fun overforOppgaveTilJournalforendeHvisIkkeJournalforende(oppgaveHendelse: OppgaveHendelse) {
        val tildeltEnhetErIkkeEnJournalforendeEnhet = !erJournalforendeEnhet(oppgaveHendelse.tildeltEnhetsnr)
        val tilhorerOppgaveTypeJournalforendeEnhet = oppgaveHendelse.erAapenVurderDokumentOppgave() || oppgaveHendelse.erAapenJournalforingsoppgave()
        if (oppgaveHendelse.erTemaBIDEllerFAR() && tilhorerOppgaveTypeJournalforendeEnhet && tildeltEnhetErIkkeEnJournalforendeEnhet) {
            overforOppgaveTilJournalforendeEnhet(oppgaveHendelse)
        }
    }

    fun endreVurderDokumentOppgaveTilJournalforingHvisJournalpostMottatt(oppgaveHendelse: OppgaveHendelse){
        val erVurderDokumentOppgaveMedJournalpost = oppgaveHendelse.erAapenVurderDokumentOppgave() && oppgaveHendelse.hasJournalpostId
        if (erVurderDokumentOppgaveMedJournalpost && journalpostService.erJournalpostStatusMottatt(oppgaveHendelse.journalpostIdMedPrefix!!)){
            endreOppgaveTypeTilJournalforingEllerFerdigstill(oppgaveHendelse)
        }
    }

    fun overforOppgaveTilJournalforendeEnhet(oppgaveHendelse: OppgaveHendelse){
        val tildeltEnhetsnr = arbeidsfordelingService.hentArbeidsfordeling(oppgaveHendelse.hentIdent)
        LOGGER.info("Oppgave ${oppgaveHendelse.id} har oppgavetype=${oppgaveHendelse.oppgavetype} med tema BID men ligger på en ikke journalførende enhet ${oppgaveHendelse.tildeltEnhetsnr}. Overfører oppgave fra ${oppgaveHendelse.tildeltEnhetsnr} til $tildeltEnhetsnr.")
        oppgaveService.overforOppgaver(oppgaveHendelse, tildeltEnhetsnr)
    }

    fun endreOppgaveTypeTilJournalforingEllerFerdigstill(oppgaveHendelse: OppgaveHendelse){
        val oppgaver = oppgaveService.finnAapneJournalforingOppgaverForJournalpost(oppgaveHendelse.journalpostId!!)
        val oppdaterOppgave = OppdaterOppgave(oppgaveHendelse)
        if (oppgaver.harJournalforingsoppgaver()){
            LOGGER.info("Oppgave ${oppgaveHendelse.id} har oppgavetype=${oppgaveHendelse.oppgavetype} med tema BID men tilhørende journalpost har status MOTTATT. Journalposten har allerede en journalføringsoppgave med tema BID. Ferdigstiller oppgave.")
            oppdaterOppgave.ferdigstill()
        } else {
            LOGGER.info("Oppgave ${oppgaveHendelse.id} har oppgavetype=${oppgaveHendelse.oppgavetype} med tema BID men tilhørende journalpost har status MOTTATT. Endrer oppgave til journalføringsoppgave")
            oppdaterOppgave.endreOppgavetype(OppgaveType.JFR)
        }
        oppgaveService.oppdaterOppgave(oppdaterOppgave)
    }

    fun erJournalforendeEnhet(enhetNr: String?): Boolean {
       val ignoreEnhet = listOf(ENHET_FAGPOST, ENHET_IT_AVDELINGEN)
       return if (enhetNr != null) ignoreEnhet.contains(enhetNr) || arbeidsfordelingService.hentBidragJournalforendeEnheter().any { it.enhetIdent == enhetNr } else false
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
        val aapneOppgaveAPI = oppgaveService.finnAapneJournalforingOppgaverForJournalpost(journalpostId)
        return aapneOppgaveAPI.harIkkeJournalforingsoppgave()
    }

    fun opprettJournalforingOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse, journalpost: Journalpost){
        val tildeltEnhetsnr = oppgaveHendelse.tildeltEnhetsnr ?: arbeidsfordelingService.hentArbeidsfordeling(oppgaveHendelse.hentIdent)
        oppgaveService.opprettJournalforingOppgave(OpprettJournalforingsOppgaveRequest(oppgaveHendelse, tildeltEnhetsnr))
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