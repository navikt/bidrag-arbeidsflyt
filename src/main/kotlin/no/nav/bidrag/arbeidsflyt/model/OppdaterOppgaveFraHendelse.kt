package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.arbeidsflyt.dto.OppdaterOppgave
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.dto.OppgaveType
import no.nav.bidrag.arbeidsflyt.service.JournalpostService
import no.nav.bidrag.arbeidsflyt.service.OppgaveService
import no.nav.bidrag.arbeidsflyt.service.OrganisasjonService
import no.nav.bidrag.arbeidsflyt.service.erFarskap
import no.nav.bidrag.dokument.dto.BidragEnhet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(SCOPE_PROTOTYPE)
class OppdaterOppgaveFraHendelse(
    var arbeidsfordelingService: OrganisasjonService,
    var journalpostService: JournalpostService,
    var oppgaveService: OppgaveService
) {

    lateinit var oppdaterOppgave: OppdaterOppgave
    lateinit var oppgaveHendelse: OppgaveData

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(OppdaterOppgaveFraHendelse::class.java)
    }

    fun behandle(_oppgaveHendelse: OppgaveHendelse): OppdaterOppgaveFraHendelse {
        oppgaveHendelse = oppgaveService.hentOppgave(_oppgaveHendelse.id)
        oppdaterOppgave = OppdaterOppgave(oppgaveHendelse)
        return this
    }

    fun overforOppgaveTilJournalforendeEnhetHvisTilhorerJournalforende(): OppdaterOppgaveFraHendelse {
        val tildeltEnhetErIkkeEnJournalforendeEnhet = !erJournalforendeEnhet(oppgaveHendelse.tildeltEnhetsnr)
        val tilhorerOppgaveTypeJournalforendeEnhet =
            oppgaveHendelse.erAapenVurderDokumentOppgave() || oppgaveHendelse.erAapenJournalforingsoppgave()
        if (oppgaveHendelse.erTemaBIDEllerFAR() && tilhorerOppgaveTypeJournalforendeEnhet && tildeltEnhetErIkkeEnJournalforendeEnhet) {
            overforOppgaveTilJournalforendeEnhet(oppgaveHendelse)
        }
        return this
    }

    fun endreVurderDokumentOppgaveTypeTilJournalforingHvisJournalpostMottatt(): OppdaterOppgaveFraHendelse {
        val erVurderDokumentOppgaveMedJournalpost =
            oppgaveHendelse.erAapenVurderDokumentOppgave() && oppgaveHendelse.hasJournalpostId
        if (erVurderDokumentOppgaveMedJournalpost) {
            journalpostService.hentJournalpostMedStatusMottatt(oppgaveHendelse.journalpostIdMedPrefix!!)
                ?.apply { endreOppgaveTypeTilJournalforingEllerFerdigstill(oppgaveHendelse) }
        }

        return this
    }

    fun overforReturoppgaveTilFarskapHvisJournalpostHarTemaFAR(): OppdaterOppgaveFraHendelse {
        val erReturOppgaveMedJournalpost =
            oppgaveHendelse.erReturoppgave() && oppgaveHendelse.hasJournalpostId
        if (erReturOppgaveMedJournalpost && oppgaveHendelse.tildeltEnhetsnr != BidragEnhet.ENHET_FARSKAP) {
            journalpostService.hentJournalpost(oppgaveHendelse.journalpostIdMedPrefix!!)
                ?.takeIf { it.erFarskap }
                ?.apply { overforOppgaveTilFarskapEnhet() }
        }

        return this
    }

    fun endreVurderDokumentOppgaveTypeTilVurderHenvendelseHvisIngenJournalpost(): OppdaterOppgaveFraHendelse {
        val erVurderDokumentOppgaveUtenJournalpost =
            oppgaveHendelse.erAapenVurderDokumentOppgave() && !oppgaveHendelse.hasJournalpostId
        if (erVurderDokumentOppgaveUtenJournalpost) {
            LOGGER.info("Oppgave ${oppgaveHendelse.id} har oppgavetype=${oppgaveHendelse.oppgavetype} med tema BID men har ingen tilknyttet journalpost. Endrer oppgavetype til ${OppgaveType.VURD_HENV}")
            oppdaterOppgave.endreOppgavetype(OppgaveType.VURD_HENV)
        }

        return this
    }

    fun utfor(endretAvEnhetsnummer: String? = null) {
        if (oppdaterOppgave.hasChanged()) {
            oppgaveService.oppdaterOppgave(oppdaterOppgave, endretAvEnhetsnummer)
        }
    }

    private fun overforOppgaveTilFarskapEnhet() {
        val tildeltEnhetsnr = BidragEnhet.ENHET_FARSKAP
        LOGGER.info("Oppgave ${oppgaveHendelse.id} er returoppgave som har journalpost tema FAR. Oppgaven er tildelt ${oppgaveHendelse.tildeltEnhetsnr} som ikke er farskapenhet. Overfører til ${BidragEnhet.ENHET_FARSKAP}")
        oppdaterOppgave.overforTilEnhet(tildeltEnhetsnr)
    }
    private fun overforOppgaveTilJournalforendeEnhet(oppgaveHendelse: OppgaveData) {
        val tildeltEnhetsnr = arbeidsfordelingService.hentArbeidsfordeling(oppgaveHendelse.ident)
        LOGGER.info("Oppgave ${oppgaveHendelse.id} har oppgavetype=${oppgaveHendelse.oppgavetype} med tema BID men ligger på en ikke journalførende enhet ${oppgaveHendelse.tildeltEnhetsnr}. Overfører oppgave fra ${oppgaveHendelse.tildeltEnhetsnr} til $tildeltEnhetsnr.")
        oppdaterOppgave.overforTilEnhet(tildeltEnhetsnr.verdi)
    }

    private fun endreOppgaveTypeTilJournalforingEllerFerdigstill(oppgaveHendelse: OppgaveData) {
        val oppgaver = oppgaveService.finnAapneJournalforingOppgaverForJournalpost(oppgaveHendelse.journalpostId!!)
        if (oppgaver.harJournalforingsoppgaver()) {
            LOGGER.info("Oppgave ${oppgaveHendelse.id} har oppgavetype=${oppgaveHendelse.oppgavetype} med tema BID men tilhørende journalpost har status MOTTATT. Journalposten har allerede en journalføringsoppgave med tema BID. Ferdigstiller oppgave.")
            oppdaterOppgave.ferdigstill()
        } else {
            LOGGER.info("Oppgave ${oppgaveHendelse.id} har oppgavetype=${oppgaveHendelse.oppgavetype} med tema BID men tilhørende journalpost har status MOTTATT. Endrer oppgave til journalføringsoppgave")
            oppdaterOppgave.endreOppgavetype(OppgaveType.JFR)
        }
    }

    private fun erJournalforendeEnhet(enhetNr: String?): Boolean {
        val ignoreEnhet = listOf(ENHET_FAGPOST, ENHET_IT_AVDELINGEN, ENHET_YTELSE)
        return if (enhetNr != null) {
            ignoreEnhet.contains(enhetNr) || arbeidsfordelingService.hentBidragJournalforendeEnheter()
                .any { it.nummer.verdi == enhetNr }
        } else {
            false
        }
    }
}
