package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.PROFILE_KAFKA_TEST
import no.nav.bidrag.arbeidsflyt.PROFILE_NAIS
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.arbeidsflyt.utils.FeatureToggle
import no.nav.bidrag.arbeidsflyt.service.BehandleOppgaveHendelseService
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
    private val jsonMapperService: JsonMapperService,
    private val featureToggle: FeatureToggle
) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(OppgaveHendelseListener::class.java)
    }

    @KafkaListener(containerFactory="oppgaveKafkaListenerContainerFactory", topics = ["\${TOPIC_OPPGAVE_ENDRET}"])
    fun lesOppgaveEndretHendelse(consumerRecord: ConsumerRecord<String, String>) {
        val oppgaveEndretHendelse = jsonMapperService.mapOppgaveHendelse(consumerRecord.value())

        if (oppgaveEndretHendelse.erTemaBIDEllerFAR() && featureToggle.isFeatureEnabled(FeatureToggle.Feature.KAFKA_OPPGAVE)) {
            LOGGER.info("Mottatt oppgave endret hendelse med journalpostId ${oppgaveEndretHendelse.journalpostId}, " +
                    "statuskategori ${oppgaveEndretHendelse.statuskategori}, " +
                    "tema ${oppgaveEndretHendelse.tema}, " +
                    "oppgavetype ${oppgaveEndretHendelse.oppgavetype} " +
                    "og status ${oppgaveEndretHendelse.status}")
            behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveEndretHendelse)
        }
    }
}
