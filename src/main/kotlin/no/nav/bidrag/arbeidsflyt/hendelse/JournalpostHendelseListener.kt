package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.service.HendelseService
import org.springframework.kafka.annotation.KafkaListener

interface JournalpostHendelseListener {
    fun lesHendelse(hendelse: String)
}

class DefaultJournalpostHendelseListener(private val hendelseService: HendelseService) : JournalpostHendelseListener {

    @KafkaListener(groupId = "bidrag-arbeidsflyt", topics = ["\${TOPIC_JOURNALPOST}"], errorHandler = "hendelseErrorHandler")
    override fun lesHendelse(hendelse: String) {
        hendelseService.lesHendelse(hendelse)
    }
}
