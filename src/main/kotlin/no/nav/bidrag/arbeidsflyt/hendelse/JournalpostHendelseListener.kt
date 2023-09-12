package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.arbeidsflyt.service.PersistenceService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

class JournalpostHendelseListener(
    private val jsonMapperService: JsonMapperService,
    private val behandeHendelseService: BehandleHendelseService,
    private val persistenceService: PersistenceService
) {

    @KafkaListener(groupId = "bidrag-arbeidsflyt", topics = ["\${TOPIC_JOURNALPOST}"])
    fun hendeseLytter(hendelse: String) {
        prosesserHendelse(hendelse)
    }

    fun prosesserHendelse(hendelse: String) {
        val journalpostHendelse = jsonMapperService.mapJournalpostHendelse(hendelse)
        behandeHendelseService.behandleHendelse(journalpostHendelse)

        // Feilede meldinger med samme journalpostid kan ignoreres da det er alltid siste melding som gjelder
        persistenceService.slettFeiledeMeldingerMedJournalpostId(journalpostHendelse.journalpostId)
    }
}