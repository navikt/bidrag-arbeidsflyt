package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.arbeidsflyt.service.PersistenceService
import org.springframework.kafka.annotation.KafkaListener

interface JournalpostHendelseListener {
    fun lesHendelse(hendelse: String)
}

class KafkaJournalpostHendelseListener(
    jsonMapperService: JsonMapperService,
    behandeHendelseService: BehandleHendelseService,
    persistenceService: PersistenceService
) : PojoJournalpostHendelseListener(jsonMapperService, persistenceService, behandeHendelseService) {

    @KafkaListener(groupId = "bidrag-arbeidsflyt-local", topics = ["\${TOPIC_JOURNALPOST}"])
    override fun lesHendelse(hendelse: String) {
        super.lesHendelse(hendelse)
    }
}

open class PojoJournalpostHendelseListener(
    private val jsonMapperService: JsonMapperService,
    private val persistenceService: PersistenceService,
    private val behandeHendelseService: BehandleHendelseService
) : JournalpostHendelseListener {

    override fun lesHendelse(hendelse: String) {
        val journalpostHendelse = jsonMapperService.mapJournalpostHendelse(hendelse)
        behandeHendelseService.behandleHendelse(journalpostHendelse)

        // Feilede meldinger med samme journalpostid kan ignoreres da det er alltid siste melding som gjelder
        persistenceService.slettFeiledeMeldingerMedJournalpostId(journalpostHendelse.journalpostId)
    }
}
