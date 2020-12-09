package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(BehandleHendelseService::class.java)

@Service
class BehandleHendelseService {
    fun behandleHendelse(journalpostHendelse: JournalpostHendelse) {
        LOGGER.info("Behandler journalpostHendelse: $journalpostHendelse")
    }
}