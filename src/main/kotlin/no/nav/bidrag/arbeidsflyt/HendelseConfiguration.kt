package no.nav.bidrag.arbeidsflyt

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.service.HendelseService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.listener.KafkaListenerErrorHandler
import org.springframework.kafka.listener.ListenerExecutionFailedException
import org.springframework.messaging.Message
import java.util.Optional

@Configuration
@Profile(LIVE)
class HendelseConfiguration {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(HendelseConfiguration::class.java)
    }

    @Bean
    fun journalpostHendelseListener(objectMapper: ObjectMapper, hendelseService: HendelseService) = JournalpostHendelseListener(
        objectMapper, hendelseService
    )

    @Bean
    fun hendelseErrorHandler(): KafkaListenerErrorHandler {
        return KafkaListenerErrorHandler { message: Message<*>, e: ListenerExecutionFailedException ->
            LOGGER.error("Message {} cause error: {} - {} - headers: {}", message.payload, e.javaClass.simpleName, e.message, message.headers)
            Optional.empty<Any>()
        }
    }
}