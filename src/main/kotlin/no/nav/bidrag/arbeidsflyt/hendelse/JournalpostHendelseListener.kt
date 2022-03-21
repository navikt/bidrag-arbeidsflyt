package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import org.springframework.kafka.annotation.KafkaListener

interface JournalpostHendelseListener {
    fun lesHendelse(hendelse: String)
}

class KafkaJournalpostHendelseListener(
    jsonMapperService: JsonMapperService, behandeHendelseService: BehandleHendelseService
) : PojoJournalpostHendelseListener(jsonMapperService, behandeHendelseService) {

    @KafkaListener(groupId = "bidrag-arbeidsflyt", topics = ["\${TOPIC_JOURNALPOST}"])
    override fun lesHendelse(hendelse: String) {
        super.lesHendelse(hendelse)
    }
}

open class PojoJournalpostHendelseListener(
    private val jsonMapperService: JsonMapperService,
    private val behandeHendelseService: BehandleHendelseService
) : JournalpostHendelseListener {

    override fun lesHendelse(hendelse: String) {
        val journalpostHendelse = jsonMapperService.mapJournalpostHendelse(hendelse)
        behandeHendelseService.behandleHendelse(journalpostHendelse)
    }
}
