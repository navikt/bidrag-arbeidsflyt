package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveEndretHendelse
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.DefaultBehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener

interface OppgaveHendelseListener {
    fun lesHendelse(consumerRecord: ConsumerRecord<String, OppgaveEndretHendelse>)
}

class KafkaOppgaveHendelseListener: OppgaveHendelseListener {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(KafkaOppgaveHendelseListener::class.java)
    }

    @KafkaListener(groupId = "bidrag-arbeidsflyt", topics = ["\${TOPIC_OPPGAVE_ENDRET}"], errorHandler = "hendelseErrorHandler")
    override fun lesHendelse(consumerRecord: ConsumerRecord<String, OppgaveEndretHendelse>) {
        LOGGER.info("Mottatt oppgave endret hendelse {}", consumerRecord)
        LOGGER.info("Mottatt oppgave endret hendelse 2 {}", consumerRecord.value())
    }
}
