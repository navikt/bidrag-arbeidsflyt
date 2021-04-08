package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import org.springframework.kafka.annotation.KafkaListener

interface JournalpostHendelseListener {
    fun lesHendelse(hendelse: String)
}

class DefaultJournalpostHendelseListener(
    private val jsonMapperService: JsonMapperService,
    private val behandeHendelseService: BehandleHendelseService
) : JournalpostHendelseListener {

    @KafkaListener(groupId = "bidrag-arbeidsflyt", topics = ["\${TOPIC_JOURNALPOST}"], errorHandler = "hendelseErrorHandler")
    override fun lesHendelse(hendelse: String) {
        val journalpostHendelse = jsonMapperService.mapHendelse(hendelse)
        behandeHendelseService.behandleHendelse(journalpostHendelse)
    }
}
