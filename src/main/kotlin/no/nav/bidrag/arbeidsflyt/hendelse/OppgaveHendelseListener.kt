package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener

interface OppgaveHendelseListener {
    fun lesHendelse(consumerRecord: ConsumerRecord<String, String>)
}

class KafkaOppgaveHendelseListener(jsonMapperService: JsonMapperService): PojoOppgaveEndretHendelseListener(
    jsonMapperService
) {

    @KafkaListener(containerFactory="oppgaveEndretKafkaListenerContainerFactory", topics = ["\${TOPIC_OPPGAVE_ENDRET}"], errorHandler = "hendelseErrorHandler")
    override fun lesHendelse(consumerRecord: ConsumerRecord<String, String>) {
        super.lesHendelse(consumerRecord);
    }
}

open class PojoOppgaveEndretHendelseListener(
    private val jsonMapperService: JsonMapperService
) : OppgaveHendelseListener {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(PojoOppgaveEndretHendelseListener::class.java)
    }

    override fun lesHendelse(consumerRecord: ConsumerRecord<String, String>) {
        val oppgaveEndretHendelse = jsonMapperService.mapOppgaveEndretHendelse(consumerRecord.value())

        if (oppgaveEndretHendelse.erTemaBIDEllerFAR()) {
            LOGGER.info("Mottatt oppgave endret hendelse med journalpostId ${oppgaveEndretHendelse.journalpostId}, " +
                    "statuskategori ${oppgaveEndretHendelse.statuskategori}, " +
                    "tema ${oppgaveEndretHendelse.tema}, " +
                    "oppgavetype ${oppgaveEndretHendelse.oppgavetype} " +
                    "og status ${oppgaveEndretHendelse.status}")
        }
    }
}
