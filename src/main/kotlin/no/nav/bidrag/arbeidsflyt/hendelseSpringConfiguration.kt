package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.hendelse.DefaultJournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.service.HendelseService
import no.nav.bidrag.commons.ExceptionLogger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.listener.KafkaListenerErrorHandler
import org.springframework.kafka.listener.ListenerExecutionFailedException
import org.springframework.messaging.Message
import org.springframework.web.client.RestTemplate
import java.util.Optional

private val LOGGER = LoggerFactory.getLogger(HendelseConfiguration::class.java)

@Configuration
@Profile(LIVE)
class HendelseConfiguration {
    @Bean
    fun journalpostHendelseListener(hendelseService: HendelseService): JournalpostHendelseListener = DefaultJournalpostHendelseListener(
        hendelseService
    )

    @Bean
    fun hendelseErrorHandler(): KafkaListenerErrorHandler {
        return KafkaListenerErrorHandler { message: Message<*>, e: ListenerExecutionFailedException ->
            LOGGER.error("Message {} cause error: {} - {} - headers: {}", message.payload, e.javaClass.simpleName, e.message, message.headers)
            Optional.empty<Any>()
        }
    }
}

@Configuration
class ArbeidsflytConfiguration {
    @Bean
    fun oppgaveConsumer(): OppgaveConsumer {
        val restTemplate = RestTemplate()
        return OppgaveConsumer(restTemplate)
    }

    @Bean
    fun ExceptionLogger() = ExceptionLogger(BidragArbeidsflyt::class.java.simpleName)
}
