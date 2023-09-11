package no.nav.bidrag.arbeidsflyt.hendelse

import io.micrometer.core.instrument.MeterRegistry
import no.nav.bidrag.arbeidsflyt.PROFILE_KAFKA_TEST
import no.nav.bidrag.arbeidsflyt.PROFILE_NAIS
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.hendelse.dto.OppgaveKafkaHendelseV2
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
    private val jsonMapperService: JsonMapperService,
    private val persistenceService: PersistenceService,
    private val meterRegistry: MeterRegistry,
    private val oppgaveService: OppgaveService
) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(OppgaveHendelseListener::class.java)
    }
    @KafkaListener(groupId = "bidrag-arbeisflyt-local", topics = ["\${TOPIC_OPPGAVE_HENDELSE}"])
    fun lesOppgaveHendelse(consumerRecord: ConsumerRecord<String, String>) {
        val oppgaveHendelse = jsonMapperService.mapOppgaveHendelseV2(consumerRecord.value())

        if (oppgaveHendelse.erTemaBIDEllerFAR()) {
            if (oppgaveHendelse.erOppgaveOpprettetHendelse) {
                LOGGER.info("Mottatt oppgave opprettet hendelse $oppgaveHendelse")
                val oppgave = oppgaveService.hentOppgave(oppgaveHendelse.oppgaveId)
                behandleOppgaveHendelseService.behandleOpprettOppgave(oppgave)
                measureOppgaveOpprettetHendelse(oppgave)
            } else if (oppgaveHendelse.erOppgaveEndretHendelse) {
                LOGGER.info("Mottatt oppgave endret hendelse $oppgaveHendelse")
                val oppgave = oppgaveService.hentOppgave(oppgaveHendelse.oppgaveId)
                behandleOppgaveHendelseService.behandleEndretOppgave(oppgave)
                persistenceService.slettFeiledeMeldingerMedOppgaveid(oppgaveHendelse.oppgaveId)
            }

        }
    }

//    @KafkaListener(containerFactory = "oppgaveKafkaListenerContainerFactory", topics = ["\${TOPIC_OPPGAVE_ENDRET}"])
//    fun lesOppgaveEndretHendelse(consumerRecord: ConsumerRecord<String, String>) {
//        val oppgaveEndretHendelse = jsonMapperService.mapOppgaveHendelse(consumerRecord.value())
//
//        if (oppgaveEndretHendelse.erTemaBIDEllerFAR()) {
//            LOGGER.info("Mottatt oppgave endret hendelse $oppgaveEndretHendelse")
//            val oppgave = oppgaveService.hentOppgave(oppgaveEndretHendelse.id)
//            behandleOppgaveHendelseService.behandleEndretOppgave(oppgave)
//            persistenceService.slettFeiledeMeldingerMedOppgaveid(oppgaveEndretHendelse.id)
//        }
//    }
//
//    @KafkaListener(containerFactory = "oppgaveKafkaListenerContainerFactory", topics = ["\${TOPIC_OPPGAVE_OPPRETTET}"])
//    fun lesOppgaveOpprettetHendelse(consumerRecord: ConsumerRecord<String, String>) {
//        val oppgaveOpprettetHendelse = jsonMapperService.mapOppgaveHendelse(consumerRecord.value())
//
//        if (oppgaveOpprettetHendelse.erTemaBIDEllerFAR()) {
//            LOGGER.info("Mottatt oppgave opprettet hendelse $oppgaveOpprettetHendelse")
//            val oppgave = oppgaveService.hentOppgave(oppgaveOpprettetHendelse.id)
//            behandleOppgaveHendelseService.behandleOpprettOppgave(oppgave)
//            measureOppgaveOpprettetHendelse(oppgave)
//        }
//    }

    fun measureOppgaveOpprettetHendelse(oppgaveOpprettetHendelse: OppgaveData) {
        if (oppgaveOpprettetHendelse.erTemaBIDEllerFAR() && oppgaveOpprettetHendelse.erJournalforingOppgave) {
            meterRegistry.counter(
                "jfr_oppgave_opprettet",
                "tema", oppgaveOpprettetHendelse.tema ?: "UKJENT",
                "enhet", oppgaveOpprettetHendelse.tildeltEnhetsnr ?: "UKJENT",
                "opprettetAv", oppgaveOpprettetHendelse.opprettetAv ?: "UKJENT",
                "opprettetAvEnhetsnr", oppgaveOpprettetHendelse.opprettetAvEnhetsnr ?: "UKJENT"
            ).increment()
        }
    }
}
