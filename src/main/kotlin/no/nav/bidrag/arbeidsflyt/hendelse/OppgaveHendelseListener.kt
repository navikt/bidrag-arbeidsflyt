package no.nav.bidrag.arbeidsflyt.hendelse

import io.micrometer.core.instrument.MeterRegistry
import no.nav.bidrag.arbeidsflyt.PROFILE_KAFKA_TEST
import no.nav.bidrag.arbeidsflyt.PROFILE_NAIS
import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.service.BehandleOppgaveHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
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
    private val meterRegistry: MeterRegistry
) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(OppgaveHendelseListener::class.java)
    }

    @KafkaListener(containerFactory="oppgaveKafkaListenerContainerFactory", topics = ["\${TOPIC_OPPGAVE_ENDRET}"])
    fun lesOppgaveEndretHendelse(consumerRecord: ConsumerRecord<String, String>) {
        val oppgaveEndretHendelse = jsonMapperService.mapOppgaveHendelse(consumerRecord.value())

        if (oppgaveEndretHendelse.erTemaBIDEllerFAR()) {
            LOGGER.info("Mottatt oppgave endret hendelse med journalpostId ${oppgaveEndretHendelse.journalpostId}, " +
                    "oppgaveId ${oppgaveEndretHendelse.id}," +
                    "statuskategori ${oppgaveEndretHendelse.statuskategori}, " +
                    "tema ${oppgaveEndretHendelse.tema}, " +
                    "oppgavetype ${oppgaveEndretHendelse.oppgavetype}, " +
                    "opprettetAv ${oppgaveEndretHendelse.opprettetAv}, " +
                    "endretAv ${oppgaveEndretHendelse.endretAv}, " +
                    "tilordnetRessurs ${oppgaveEndretHendelse.tilordnetRessurs}, " +
                    "versjon ${oppgaveEndretHendelse.versjon}, " +
                    "og status ${oppgaveEndretHendelse.status}")
            behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveEndretHendelse)
            persistenceService.slettFeiledeMeldingerMedOppgaveid(oppgaveEndretHendelse.id)
        }
    }

    @KafkaListener(containerFactory="oppgaveKafkaListenerContainerFactory", topics = ["\${TOPIC_OPPGAVE_OPPRETTET}"])
    fun lesOppgaveOpprettetHendelse(consumerRecord: ConsumerRecord<String, String>) {
        val oppgaveOpprettetHendelse = jsonMapperService.mapOppgaveHendelse(consumerRecord.value())

        if (oppgaveOpprettetHendelse.erTemaBIDEllerFAR() && oppgaveOpprettetHendelse.erJournalforingOppgave) {
            LOGGER.info("Mottatt oppgave opprettet hendelse med journalpostId ${oppgaveOpprettetHendelse.journalpostId}, " +
                    "oppgaveId ${oppgaveOpprettetHendelse.id}," +
                    "statuskategori ${oppgaveOpprettetHendelse.statuskategori}, " +
                    "tema ${oppgaveOpprettetHendelse.tema}, " +
                    "oppgavetype ${oppgaveOpprettetHendelse.oppgavetype}, " +
                    "opprettetAv ${oppgaveOpprettetHendelse.opprettetAv}, " +
                    "og status ${oppgaveOpprettetHendelse.status}")
            behandleOppgaveHendelseService.behandleOpprettOppgave(oppgaveOpprettetHendelse)
            measureOppgaveOpprettetHendelse(oppgaveOpprettetHendelse)
        }
    }

    fun measureOppgaveOpprettetHendelse(oppgaveOpprettetHendelse: OppgaveHendelse){
        if (oppgaveOpprettetHendelse.erTemaBIDEllerFAR() && oppgaveOpprettetHendelse.erJournalforingOppgave){
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
