package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.service.GeografiskEnhetService
import no.nav.bidrag.arbeidsflyt.service.OppgaveService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OppdaterOppgaver(
    private val journalpostHendelse: JournalpostHendelse,
    private val oppgaveService: OppgaveService,
    private val geografiskEnhetService: GeografiskEnhetService
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

            oppgaveService.ferdigstillJournalforingsOppgaver(
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

        if (!journalpostHendelse.erEksterntFagomrade && journalpostHendelse.erMottaksregistrert && oppgaverForHendelse.harIkkeJournalforingsoppgave()) {
            LOGGER.info("En mottaksregistert journalpost uten journalføringsoppgave. Rapportert av ${journalpostHendelse.hentSaksbehandlerInfo()}.")

            val personId = journalpostHendelse.aktorId ?: journalpostHendelse.fnr
            val tildeltEnhetsnr = geografiskEnhetService.hentGeografiskEnhetFailSafe(personId)
            if (tildeltEnhetsnr != journalpostHendelse.enhet){
                LOGGER.warn("Beregnet tildeltenhetsnr $tildeltEnhetsnr er ikke lik enhet fra hendelse ${journalpostHendelse.enhet}")
            }
            SECURE_LOGGER.info("Fant tildeltEnhetsnr $tildeltEnhetsnr for person $personId, fikk tildeltEnhetsnr ${journalpostHendelse.enhet}")
            oppgaveService.opprettJournalforingOppgave(OpprettJournalforingsOppgaveRequest(journalpostHendelse))
            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    fun ferdigstillJournalforingsoppgaver(): OppdaterOppgaver {
        finnOppgaverForHendelse()

        if (journalpostHendelse.erJournalstatusEndretTilIkkeMottatt() && oppgaverForHendelse.harJournalforingsoppgaver()) {
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
            oppgaverForHendelse = oppgaveService.finnOppgaverForHendelse(journalpostHendelse)
            finnOppdaterteOppgaverForHendelse = false
        }
    }
}
