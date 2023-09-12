package no.nav.bidrag.arbeidsflyt.hendelse

import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import no.nav.bidrag.arbeidsflyt.PROFILE_KAFKA_TEST
import no.nav.bidrag.arbeidsflyt.PROFILE_NAIS
import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.hendelse.dto.OppgaveKafkaHendelse
import no.nav.bidrag.arbeidsflyt.service.BehandleOppgaveHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.arbeidsflyt.service.OppgaveService
import no.nav.bidrag.arbeidsflyt.service.PersistenceService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
@DependsOn("oppgaveKafkaListenerContainerFactory")
@Profile(value = [PROFILE_KAFKA_TEST, PROFILE_NAIS])
class OppgaveHendelseListener(
    private val behandleOppgaveHendelseService: BehandleOppgaveHendelseService,
    private val jsonMapperService: JsonMapperService
) {
    @KafkaListener(groupId = "bidrag-arbeisflyt", topics = ["\${TOPIC_OPPGAVE_HENDELSE}"])
    fun lesOppgaveHendelse(consumerRecord: ConsumerRecord<String, String>) {
        val oppgaveHendelse = jsonMapperService.mapOppgaveHendelseV2(consumerRecord.value())

        if (oppgaveHendelse.erTemaBIDEllerFAR()) {
            behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveHendelse)
        }
    }

}
