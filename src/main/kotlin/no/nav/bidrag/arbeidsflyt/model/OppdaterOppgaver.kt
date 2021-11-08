package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.arbeidsflyt.service.OppgaveService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OppdaterOppgaver(
    private val journalpostHendelse: JournalpostHendelse,
    private val oppgaveService: OppgaveService
) {
    private var finnOppdaterteOppgaverForHendelse = true
    private lateinit var oppgaverForHendelse: OppgaverForHendelse

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

            oppgaveService.ferdigstillOppgaver(
                endretAvEnhetsnummer = journalpostHendelse.hentEndretAvEnhetsnummer(),
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

        if (journalpostHendelse.erMottaksregistrert && oppgaverForHendelse.harIkkeJournalforingsoppgaveForJournalpost(journalpostHendelse)) {
            LOGGER.info("En mottaksregistert journalpost uten journalføringsoppgave. Rapportert av ${journalpostHendelse.hentSaksbehandlerInfo()}.")

            oppgaveService.opprettOppgave(journalpostHendelse)
            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    fun ferdigstillJournalforingsoppgaver(): OppdaterOppgaver {
        finnOppgaverForHendelse()

        if (journalpostHendelse.erJournalstatusEndretTilIkkeMottatt() && oppgaverForHendelse.harJournalforingsoppgaver()) {
            LOGGER.info("En journalført journalpost skal ikke ha journalføringsoppgaver. Rapportert av ${journalpostHendelse.hentSaksbehandlerInfo()}.")
            oppgaveService.ferdigstillOppgaver(
                endretAvEnhetsnummer = journalpostHendelse.hentEndretAvEnhetsnummer(),
                oppgaverForHendelse = oppgaverForHendelse
            )
            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    private fun finnOppgaverForHendelse() {
        if (finnOppdaterteOppgaverForHendelse) {
            oppgaverForHendelse = oppgaveService.finnOppgaverForHendelse(journalpostHendelse)
            finnOppdaterteOppgaverForHendelse = false
        }
    }
}
