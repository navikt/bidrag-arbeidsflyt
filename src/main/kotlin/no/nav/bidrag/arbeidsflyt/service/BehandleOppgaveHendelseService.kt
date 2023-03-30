package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.ENHET_FAGPOST
import no.nav.bidrag.arbeidsflyt.model.OppdaterOppgaveFraHendelse
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

val OppgaveHendelse.tilhorerFagpost get() = tildeltEnhetsnr == ENHET_FAGPOST

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

    fun behandleOpprettOppgave(oppgaveHendelse: OppgaveHendelse) {
        if (oppgaveHendelse.erJournalforingOppgave) {
            persistenceService.lagreJournalforingsOppgaveFraHendelse(oppgaveHendelse)
        }

        if (oppgaveHendelse.hasJournalpostId) {
            oppdaterOppgaveFraHendelse(oppgaveHendelse)
                .overforOppgaveTilJournalforendeEnhetHvisTilhorerJournalforende()
                .endreVurderDokumentOppgaveTypeTilJournalforingHvisJournalpostMottatt()
                .utfor()
        } else {
            oppdaterOppgaveFraHendelse(oppgaveHendelse)
                .overforOppgaveTilJournalforendeEnhetHvisTilhorerJournalforende()
                .endreVurderDokumentOppgaveTypeTilVurderHenvendelseHvisIngenJournalpost()
                .utfor()
        }
    }

    @Transactional
    fun behandleEndretOppgave(oppgaveHendelse: OppgaveHendelse) {
        if (oppgaveHendelse.hasJournalpostId) {
            oppdaterOppgaveFraHendelse(oppgaveHendelse)
                .overforOppgaveTilJournalforendeEnhetHvisTilhorerJournalforende()
                .endreVurderDokumentOppgaveTypeTilJournalforingHvisJournalpostMottatt()
                .utfor()
            opprettNyJournalforingOppgaveHvisNodvendig(oppgaveHendelse)
        } else {
            LOGGER.debug("Oppgave ${oppgaveHendelse.id} har ingen journalpostid. Stopper videre behandling.")
        }

        persistenceService.oppdaterEllerSlettOppgaveMetadataFraHendelse(oppgaveHendelse)
    }

    fun oppdaterOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse): OppdaterOppgaveFraHendelse {
        return applicationContext.getBean(OppdaterOppgaveFraHendelse::class.java)
            .behandle(oppgaveHendelse)
    }
    fun opprettNyJournalforingOppgaveHvisNodvendig(oppgaveHendelse: OppgaveHendelse) {
        if (!oppgaveHendelse.tilhorerFagpost && (oppgaveHendelse.erAvsluttetJournalforingsoppgave() || erOppgavetypeEndretFraJournalforingTilAnnet(oppgaveHendelse))) {
            opprettNyJournalforingOppgaveHvisJournalpostMottatt(oppgaveHendelse)
        }
    }

    fun opprettNyJournalforingOppgaveHvisJournalpostMottatt(oppgaveHendelse: OppgaveHendelse) {
        if (harIkkeAapneJournalforingsoppgaver(oppgaveHendelse.journalpostId!!)) {
            journalpostService.hentJournalpostMedStatusMottatt(oppgaveHendelse.journalpostIdMedPrefix!!)
                .takeIf { it?.erBidragFagomrade == true }
                ?.run {
                    LOGGER.info("Journalpost ${oppgaveHendelse.journalpostId} har status MOTTATT men har ingen journalføringsoppgave. Oppretter ny journalføringsoppgave")
                    opprettJournalforingOppgaveFraHendelse(oppgaveHendelse)
                } ?: run { LOGGER.info("Journalpost ${oppgaveHendelse.journalpostId} som tilhører oppgave ${oppgaveHendelse.id} har ikke status MOTTATT. Stopper videre behandling.") }
        }
    }

    fun harIkkeAapneJournalforingsoppgaver(journalpostId: String): Boolean {
        val aapneOppgaveAPI = oppgaveService.finnAapneJournalforingOppgaverForJournalpost(journalpostId)
        return aapneOppgaveAPI.harIkkeJournalforingsoppgave()
    }

    fun opprettJournalforingOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse) {
        val tildeltEnhetsnr = oppgaveHendelse.tildeltEnhetsnr ?: arbeidsfordelingService.hentArbeidsfordeling(oppgaveHendelse.hentIdent)
        oppgaveService.opprettJournalforingOppgave(OpprettJournalforingsOppgaveRequest(oppgaveHendelse, tildeltEnhetsnr))
    }

    fun erOppgavetypeEndretFraJournalforingTilAnnet(oppgaveHendelse: OppgaveHendelse): Boolean {
        if (oppgaveHendelse.erJournalforingOppgave) {
            return false
        }

        val prevOppgaveState = persistenceService.hentJournalforingOppgave(oppgaveHendelse.id)
        if (prevOppgaveState?.erJournalforingOppgave() == true) {
            LOGGER.info("Oppgavetype for oppgave ${oppgaveHendelse.id} er endret fra JFR til ${oppgaveHendelse.oppgavetype}")
            return true
        }

        return false
    }
}
