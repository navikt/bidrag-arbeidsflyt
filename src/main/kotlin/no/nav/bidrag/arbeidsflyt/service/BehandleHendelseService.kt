package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.model.OppdaterOppgaver
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

        OppdaterOppgaver(journalpostHendelse, oppgaveService)
            .oppdaterEksterntFagomrade()
            .oppdaterEndretEnhetsnummer()
            .opprettJournalforingsoppgave()
            .ferdigstillJournalforingsoppgaver()
    }
}
