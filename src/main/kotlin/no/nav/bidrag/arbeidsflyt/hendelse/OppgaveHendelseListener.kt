package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.arbeidsflyt.utils.FeatureToggle
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener

interface OppgaveHendelseListener {
    fun lesOppgaveEndretHendelse(consumerRecord: ConsumerRecord<String, String>)
    fun lesOppgaveOpprettetHendelse(consumerRecord: ConsumerRecord<String, String>)
}

class KafkaOppgaveHendelseListenerImpl(jsonMapperService: JsonMapperService, featureToggle: FeatureToggle): OppgaveEndretHendelseListenerImpl(
    jsonMapperService, featureToggle
) {

    @KafkaListener(containerFactory="oppgaveEndretKafkaListenerContainerFactory", topics = ["\${TOPIC_OPPGAVE_ENDRET}"], errorHandler = "hendelseErrorHandler")
    override fun lesOppgaveEndretHendelse(consumerRecord: ConsumerRecord<String, String>) {
        super.lesOppgaveEndretHendelse(consumerRecord);
    }

    @KafkaListener(containerFactory="oppgaveEndretKafkaListenerContainerFactory", topics = ["\${TOPIC_OPPGAVE_OPPRETTET}"], errorHandler = "hendelseErrorHandler")
    override fun lesOppgaveOpprettetHendelse(consumerRecord: ConsumerRecord<String, String>) {
        super.lesOppgaveOpprettetHendelse(consumerRecord);
    }
}

open class OppgaveEndretHendelseListenerImpl(
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
            LOGGER.info("Mottatt oppgave oppprettet hendelse med journalpostId ${oppgaveOpprettetHendelse.journalpostId}, " +
                    "statuskategori ${oppgaveOpprettetHendelse.statuskategori}, " +
                    "ident ${oppgaveOpprettetHendelse.ident}, " +
                    "tema ${oppgaveOpprettetHendelse.tema}, " +
                    "oppgavetype ${oppgaveOpprettetHendelse.oppgavetype} " +
                    "og status ${oppgaveOpprettetHendelse.status}")
        }
    }
}
