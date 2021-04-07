package no.nav.bidrag.arbeidsflyt.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(JsonMapperService::class.java)

@Service
class JsonMapperService(
    private val objectMapper: ObjectMapper,
    private val behandeHendelseService: BehandleHendelseService
) {
    fun lesHendelse(hendelse: String) {
        val journalpostHendelse = try {
            objectMapper.readValue(hendelse, JournalpostHendelse::class.java)
        } finally {
            LOGGER.debug("Leser hendelse: {}", hendelse)
        }

        behandeHendelseService.behandleHendelse(journalpostHendelse)
    }
}
