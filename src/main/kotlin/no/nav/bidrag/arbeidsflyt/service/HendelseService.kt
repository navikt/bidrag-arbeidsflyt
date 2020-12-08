package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HendelseService {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(HendelseService::class.java)
    }

    fun behandleHendelse(journalpostHendelse: JournalpostHendelse) {
        LOGGER.info("Behandler journalpostHendelse: $journalpostHendelse")
    }
}
