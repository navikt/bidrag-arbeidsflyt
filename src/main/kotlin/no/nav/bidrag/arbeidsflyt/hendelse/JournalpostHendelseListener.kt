package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.arbeidsflyt.service.PersistenceService
import org.springframework.kafka.annotation.KafkaListener

class JournalpostHendelseListener(
    private val jsonMapperService: JsonMapperService,
    private val behandeHendelseService: BehandleHendelseService,
    private val persistenceService: PersistenceService,
) {
    @KafkaListener(groupId = "\${NAIS_APP_NAME}", topics = ["\${TOPIC_JOURNALPOST}"])
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
