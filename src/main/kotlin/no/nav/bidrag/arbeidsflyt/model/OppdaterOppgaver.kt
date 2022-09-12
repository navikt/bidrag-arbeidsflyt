package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.service.ArbeidsfordelingService
import no.nav.bidrag.arbeidsflyt.service.OppgaveService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OppdaterOppgaver(
    private val journalpostHendelseIntern: JournalpostHendelseIntern,
    private val oppgaveService: OppgaveService,
    private val arbeidsfordelingService: ArbeidsfordelingService
) {
    private var finnOppdaterteOppgaverForHendelse = true
    private lateinit var oppgaverForHendelse: OppgaverForHendelse
    private var journalpostHendelse = journalpostHendelseIntern.journalpostHendelse

    init {
        finnOppgaverForHendelse()
    }

    companion object {
        @JvmStatic
        val LOGGER: Logger = LoggerFactory.getLogger(OppdaterOppgaver::class.java)
    }

    fun oppdaterEksterntFagomrade(): OppdaterOppgaver {
        if (journalpostHendelse.erEksterntFagomrade) {
            LOGGER.info("Endring til eksternt fagområde av ${journalpostHendelse.hentSaksbehandlerInfo()}.")

            oppgaveService.ferdigstillJournalforingsOppgaver(
                endretAvEnhetsnummer = journalpostHendelseIntern.journalpostHendelse.hentEndretAvEnhetsnummer(),
                oppgaverForHendelse = oppgaverForHendelse
            )

            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    fun oppdaterEndretEnhetsnummer(): OppdaterOppgaver {
        finnOppgaverForHendelse()

        if (oppgaverForHendelse.erEndringAvTildeltEnhetsnummer(journalpostHendelse)) {
            LOGGER.info("Endret tilordnet ressurs utført av ${journalpostHendelse.hentSaksbehandlerInfo()}.")
            oppgaveService.overforOppgaver(oppgaverForHendelse, journalpostHendelse)
            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    fun oppdaterOppgaveMedAktoerId(): OppdaterOppgaver {
        finnOppgaverForHendelse()

        if (oppgaverForHendelse.erEndringAvAktoerId(journalpostHendelse)) {
            LOGGER.info("Oppdaterer aktørid for oppgave. Rapportert av ${journalpostHendelse.hentSaksbehandlerInfo()}.")
            oppgaveService.oppdaterOppgaver(oppgaverForHendelse, journalpostHendelse)
            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    fun opprettJournalforingsoppgave(): OppdaterOppgaver {
        finnOppgaverForHendelse()

        if (!journalpostHendelse.erEksterntFagomrade && journalpostHendelse.erMottattStatus && oppgaverForHendelse.harIkkeJournalforingsoppgave()) {
            LOGGER.info("En mottaksregistert journalpost uten journalføringsoppgave. Rapportert av ${journalpostHendelse.hentSaksbehandlerInfo()}.")

            val tildeltEnhetsnr = arbeidsfordelingService.hentArbeidsfordeling(journalpostHendelse.aktorId)
            oppgaveService.opprettJournalforingOppgave(OpprettJournalforingsOppgaveRequest(journalpostHendelse, tildeltEnhetsnr))
            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    fun opprettEllerEndreBehandleDokumentOppgaver(): OppdaterOppgaver {
        val behandlingsOppgaver: OppgaverForHendelse = oppgaveService.finnBehandlingsoppgaverForSaker(journalpostHendelse.sakstilknytninger!!, journalpostHendelse.fagomrade)
        if (journalpostHendelseIntern.erJournalfortIdag && !behandlingsOppgaver.harOppdatertBehandleDokumentOppgaveForSaker(journalpostHendelseIntern.journalpostId, journalpostHendelseIntern.saker)) {
            LOGGER.info("En journalført journalpost skal ha oppdatert behandle dokument oppgaver for saker. Rapportert av ${journalpostHendelse.hentSaksbehandlerInfo()}.")
            oppgaveService.opprettEllerEndreBehandleDokumentOppgaver(journalpostHendelse, behandlingsOppgaver)
            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    fun ferdigstillJournalforingsoppgaver(): OppdaterOppgaver {
        finnOppgaverForHendelse()

        if (journalpostHendelseIntern.erJournalstatusEndretTilIkkeMottatt() && oppgaverForHendelse.harJournalforingsoppgaver()) {
            LOGGER.info("En journalført journalpost skal ikke ha journalføringsoppgaver. Rapportert av ${journalpostHendelse.hentSaksbehandlerInfo()}.")
            oppgaveService.ferdigstillJournalforingsOppgaver(
                endretAvEnhetsnummer = journalpostHendelse.hentEndretAvEnhetsnummer(),
                oppgaverForHendelse = oppgaverForHendelse
            )
            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    private fun finnOppgaverForHendelse() {
        if (finnOppdaterteOppgaverForHendelse) {
            oppgaverForHendelse = oppgaveService.finnAapneJournalforingOppgaverForJournalpost(journalpostHendelse.journalpostId)
            finnOppdaterteOppgaverForHendelse = false
        }
    }
}
