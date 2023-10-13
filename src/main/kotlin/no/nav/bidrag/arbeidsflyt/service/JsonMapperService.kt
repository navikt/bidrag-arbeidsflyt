package no.nav.bidrag.arbeidsflyt.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.arbeidsflyt.hendelse.dto.OppgaveKafkaHendelse
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JsonMapperService(private val objectMapper: ObjectMapper) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(JsonMapperService::class.java)
    }

    fun mapJournalpostHendelse(hendelse: String): JournalpostHendelse {
        return try {
            objectMapper.readValue(hendelse, JournalpostHendelse::class.java)
        } finally {
            LOGGER.debug("Leser hendelse: {}", hendelse)
        }
    }

    fun mapOppgaveHendelseV2(hendelse: String): OppgaveKafkaHendelse {
        return try {
            objectMapper.readValue(hendelse, OppgaveKafkaHendelse::class.java)
        } finally {
            LOGGER.debug("Leser hendelse: {}", hendelse)
        }
    }
}
