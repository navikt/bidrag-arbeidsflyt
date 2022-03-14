package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.PROFILE_KAFKA_TEST
import no.nav.bidrag.arbeidsflyt.PROFILE_LIVE
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.arbeidsflyt.service.BehandleOppgaveHendelseService
import no.nav.bidrag.arbeidsflyt.utils.FeatureToggle
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

interface OppgaveHendelseListener {
    fun lesOppgaveEndretHendelse(consumerRecord: ConsumerRecord<String, String>)
    fun lesOppgaveOpprettetHendelse(consumerRecord: ConsumerRecord<String, String>)
}

@Service
@DependsOn("oppgaveKafkaListenerContainerFactory")
@Profile(value = [PROFILE_KAFKA_TEST, PROFILE_LIVE])
class KafkaOppgaveHendelseListenerImpl(behandleOppgaveHendelseService: BehandleOppgaveHendelseService, jsonMapperService: JsonMapperService, featureToggle: FeatureToggle):
    OppgaveEndretHendelseListenerImpl(
        behandleOppgaveHendelseService,
        jsonMapperService,
        featureToggle
) {

    @KafkaListener(containerFactory="oppgaveKafkaListenerContainerFactory", topics = ["\${TOPIC_OPPGAVE_ENDRET}"], errorHandler = "hendelseErrorHandler")
    override fun lesOppgaveEndretHendelse(consumerRecord: ConsumerRecord<String, String>) {
        super.lesOppgaveEndretHendelse(consumerRecord);
    }

    @KafkaListener(containerFactory="oppgaveKafkaListenerContainerFactory", topics = ["\${TOPIC_OPPGAVE_OPPRETTET}"], errorHandler = "hendelseErrorHandler")
    override fun lesOppgaveOpprettetHendelse(consumerRecord: ConsumerRecord<String, String>) {
        super.lesOppgaveOpprettetHendelse(consumerRecord);
    }
}

open class OppgaveEndretHendelseListenerImpl(
    private val behandleOppgaveHendelseService: BehandleOppgaveHendelseService,
    private val jsonMapperService: JsonMapperService,
    private val featureToggle: FeatureToggle
) : OppgaveHendelseListener {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(OppgaveEndretHendelseListenerImpl::class.java)
    }

    override fun lesOppgaveEndretHendelse(consumerRecord: ConsumerRecord<String, String>) {
        val oppgaveEndretHendelse = jsonMapperService.mapOppgaveHendelse(consumerRecord.value())

        if (oppgaveEndretHendelse.erTemaBIDEllerFAR() && featureToggle.isFeatureEnabled(FeatureToggle.Feature.KAFKA_OPPGAVE)) {
            behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveEndretHendelse)
            LOGGER.info("Mottatt oppgave endret hendelse med journalpostId ${oppgaveEndretHendelse.journalpostId}, " +
                    "statuskategori ${oppgaveEndretHendelse.statuskategori}, " +
                    "ident ${oppgaveEndretHendelse.ident}, " +
                    "tema ${oppgaveEndretHendelse.tema}, " +
                    "oppgavetype ${oppgaveEndretHendelse.oppgavetype} " +
                    "og status ${oppgaveEndretHendelse.status}")
        }
    }

    override fun lesOppgaveOpprettetHendelse(consumerRecord: ConsumerRecord<String, String>) {
        val oppgaveOpprettetHendelse = jsonMapperService.mapOppgaveHendelse(consumerRecord.value())

        if (oppgaveOpprettetHendelse.erTemaBIDEllerFAR() && featureToggle.isFeatureEnabled(FeatureToggle.Feature.KAFKA_OPPGAVE)) {
            behandleOppgaveHendelseService.behandleOpprettOppgave(oppgaveOpprettetHendelse)
            LOGGER.info("Mottatt oppgave oppprettet hendelse med journalpostId ${oppgaveOpprettetHendelse.journalpostId}, " +
                    "statuskategori ${oppgaveOpprettetHendelse.statuskategori}, " +
                    "ident ${oppgaveOpprettetHendelse.ident}, " +
                    "tema ${oppgaveOpprettetHendelse.tema}, " +
                    "oppgavetype ${oppgaveOpprettetHendelse.oppgavetype} " +
                    "og status ${oppgaveOpprettetHendelse.status}")
        }
    }
}
