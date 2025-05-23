package no.nav.bidrag.arbeidsflyt.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.hendelse.dto.OppgaveKafkaHendelse
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JsonMapperService(
    private val objectMapper: ObjectMapper,
) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(JsonMapperService::class.java)
    }

    fun mapJournalpostHendelse(hendelse: String): JournalpostHendelse =
        try {
            objectMapper.readValue(hendelse, JournalpostHendelse::class.java)
        } finally {
            SECURE_LOGGER.debug("Leser hendelse: {}", hendelse)
        }

    fun mapOppgaveHendelseV2(hendelse: String): OppgaveKafkaHendelse =
        try {
            objectMapper.readValue(hendelse, OppgaveKafkaHendelse::class.java)
        } finally {
            SECURE_LOGGER.debug("Leser hendelse: {}", hendelse)
        }
}
