package no.nav.bidrag.arbeidsflyt.hendelse

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.RetryListener
import java.lang.Exception

class KafkaRetryListener : RetryListener {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(KafkaRetryListener::class.java)
    }

    override fun failedDelivery(
        record: ConsumerRecord<*, *>,
        ex: Exception?,
        deliveryAttempt: Int,
    ) {
        LOGGER.warn("Håndtering av kafka melding ${record.value()} feilet. Dette er $deliveryAttempt. forsøk", ex)
    }

    override fun recovered(
        record: ConsumerRecord<*, *>,
        ex: Exception?,
    ) {
        LOGGER.warn("Håndtering av kafka melding ${record.value()} er enten suksess eller ignorert pågrunn av ugyldig data", ex)
    }

    override fun recoveryFailed(
        record: ConsumerRecord<*, *>,
        original: Exception?,
        failure: Exception,
    ) {}
}
