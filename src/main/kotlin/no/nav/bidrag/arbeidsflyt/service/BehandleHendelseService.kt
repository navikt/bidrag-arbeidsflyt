package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface BehandleHendelseService {
    fun behandleHendelse(journalpostHendelse: JournalpostHendelse)
}

@Service
class DefaultBehandleHendelseService(private val oppgaveService: OppgaveService) : BehandleHendelseService {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(DefaultBehandleHendelseService::class.java)
    }

    override fun behandleHendelse(journalpostHendelse: JournalpostHendelse) {
        LOGGER.info("Behandler journalpostHendelse: $journalpostHendelse")

        val oppgaverForJournalpost = oppgaveService.finnOppgaverForJournalpost(journalpostHendelse)

        if (journalpostHendelse.erEksterntFagomrade) {
            LOGGER.info("Endring til eksternt fagområde av ${journalpostHendelse.hentSaksbehandlerInfo()}.")
            oppgaveService.ferdigstillOppgaver(oppgaverForJournalpost)
        }

        if (oppgaverForJournalpost.erEndringAvTildeltEnhetsnummer(journalpostHendelse)) {
            LOGGER.info("Endret tilordnet ressurs utført av ${journalpostHendelse.hentSaksbehandlerInfo()}.")
            oppgaveService.overforOppgaver(oppgaverForJournalpost, journalpostHendelse)
        }

        if (journalpostHendelse.erMottaksregistrertMedAktor && oppgaverForJournalpost.harIkkeJournalforingsoppgaveForAktor(journalpostHendelse)) {
            LOGGER.info("En mottaksregistert journalpost uten journalføringsoppgave. Rapportert av ${journalpostHendelse.hentSaksbehandlerInfo()}.")
            oppgaveService.opprettOppgave(journalpostHendelse)
        }

        if (journalpostHendelse.erJournalstatusEndretTilIkkeMottatt() && oppgaverForJournalpost.harJournalforingsoppgaver()) {
            LOGGER.info("En journalført journalpost skal ikke ha journalføringsoppgaver. Rapportert av ${journalpostHendelse.hentSaksbehandlerInfo()}.")
            oppgaveService.ferdigstillOppgaver(oppgaverForJournalpost)
        }
    }
}
