package no.nav.bidrag.arbeidsflyt.hendelse

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.arbeidsflyt.service.HendelseService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener

class JournalpostHendelseListener(private val objectMapper: ObjectMapper, private val hendelseService: HendelseService) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(JournalpostHendelseListener::class.java)
    }

    @KafkaListener(groupId = "bidrag-arbeidsflyt", topics = ["\${TOPIC_JOURNALPOST}"], errorHandler = "hendelseErrorHandler")
    fun lesHendelse(hendelse: String) {
        val journalpostHendelse: JournalpostHendelse? = try {
            objectMapper.readValue(hendelse, JournalpostHendelse::class.java)
        } finally {
            LOGGER.debug("Leser hendelse: {}", hendelse)
        }

        hendelseService.behandleHendelse(
            journalpostHendelse ?: throw IllegalStateException(
                "Kunne ikke behandle JournalpostHendelse beskrevet som $hendelse"
            )
        )
    }
}
