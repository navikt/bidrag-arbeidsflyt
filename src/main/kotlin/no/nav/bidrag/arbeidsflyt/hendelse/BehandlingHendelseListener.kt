package no.nav.bidrag.arbeidsflyt.hendelse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.bidrag.arbeidsflyt.PROFILE_KAFKA_TEST
import no.nav.bidrag.arbeidsflyt.PROFILE_NAIS
import no.nav.bidrag.arbeidsflyt.service.BehandleBehandlingHendelseService
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
@Profile(value = [PROFILE_KAFKA_TEST, PROFILE_NAIS])
class BehandlingHendelseListener(
    private val service: BehandleBehandlingHendelseService,
) {
    @KafkaListener(groupId = "\${NAIS_APP_NAME}", topics = ["\${TOPIC_BEHANDLING_HENDELSE}"])
    fun lesHendelse(consumerRecord: ConsumerRecord<String, String>) {
        val hendelse = lesHendelse(consumerRecord.value())

        service.behandleHendelse(hendelse)
    }

    fun lesHendelse(hendelse: String): BehandlingHendelse = commonObjectmapper.readValue<BehandlingHendelse>(hendelse)
}
