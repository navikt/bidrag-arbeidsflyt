package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import org.springframework.kafka.annotation.KafkaListener

interface JournalpostHendelseListener {
    fun lesHendelse(hendelse: String)
}

class DefaultJournalpostHendelseListener(private val jsonMapperService: JsonMapperService) : JournalpostHendelseListener {

    @KafkaListener(groupId = "bidrag-arbeidsflyt", topics = ["\${TOPIC_JOURNALPOST}"], errorHandler = "hendelseErrorHandler")
    override fun lesHendelse(hendelse: String) {
        jsonMapperService.lesHendelse(hendelse)
    }
}
