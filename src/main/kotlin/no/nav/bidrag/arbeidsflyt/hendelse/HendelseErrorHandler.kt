package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.commons.ExceptionLogger
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import org.springframework.util.backoff.ExponentialBackOff

private const val KAFKA_LISTENER_ERROR_HANDLER = "KafkaListenerErrorHandler"

@Component
class HendelseErrorHandler(var exceptionLogger: ExceptionLogger): DefaultErrorHandler(ExponentialBackOff()) {

    override fun doHandle(
        thrownException: Exception,
        data: ConsumerRecords<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        invokeListener: Runnable
    ) {
        super.doHandle(thrownException, data, consumer, container, invokeListener)
        exceptionLogger.logException(thrownException, KAFKA_LISTENER_ERROR_HANDLER)
    }
}