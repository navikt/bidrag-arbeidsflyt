package no.nav.bidrag.arbeidsflyt.hendelse

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.RetryListener

class KafkaRetryListener: RetryListener {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(KafkaRetryListener::class.java)
    }

    override fun failedDelivery(record: ConsumerRecord<*, *>, exception: Exception, deliveryAttempt: Int) {
        LOGGER.warn("Håndtering av kafka melding ${record.value()} feilet. Dette er $deliveryAttempt. forsøk", exception)
    }

    override fun recovered(record: ConsumerRecord<*, *>, exception: java.lang.Exception) {
        LOGGER.warn("Håndtering av kafka melding ${record.value()} er enten suksess eller ignorert pågrunn av ugyldig data", exception)
    }

    override fun recoveryFailed(record: ConsumerRecord<*, *>, original: java.lang.Exception, failure: java.lang.Exception) {}
}