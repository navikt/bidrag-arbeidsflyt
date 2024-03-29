package no.nav.bidrag.arbeidsflyt.hendelse

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.arbeidsflyt.model.KunneIkkeProsessereKafkaMelding
import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import no.nav.bidrag.arbeidsflyt.persistence.repository.DLQKafkaRepository
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.BehandleOppgaveHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.arbeidsflyt.service.OppgaveService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Component
class KafkaDLQRetryScheduler(
    private val jsonMapperService: JsonMapperService,
    private val dlqKafkaRepository: DLQKafkaRepository,
    private val behandleOppgaveHendelseService: BehandleOppgaveHendelseService,
    private val behandleHendelseService: BehandleHendelseService,
    private val oppgaveService: OppgaveService,
) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(KafkaDLQRetryScheduler::class.java)
    }

    @Value("\${SCHEDULER_MAX_RETRY:10}")
    lateinit var maxRetry: Number

    @Value("\${TOPIC_OPPGAVE_HENDELSE}")
    lateinit var topicOppgaveHendelse: String

    @Value("\${TOPIC_JOURNALPOST}")
    lateinit var topicJournalpost: String

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES, initialDelay = 10)
    @SchedulerLock(name = "processKafkaDLQMessages", lockAtLeastFor = "10m")
    @Transactional
    fun processMessages() {
        val messages = dlqKafkaRepository.findByRetryTrueOrderByCreatedTimestampAsc()
        LOGGER.info("KafkaDLQRetryScheduler fant ${messages.size} meldinger som skal prosesseres. MAX_RETRY=$maxRetry")

        messages.stream().forEach {
            try {
                LOGGER.info("Behandler melding ${it.id} med topicName ${it.topicName} og nøkkel ${it.messageKey} ")
                processMessage(it)
                dlqKafkaRepository.delete(it)
            } catch (e: Exception) {
                LOGGER.error("Det skjedde feil ved prosessering av melding med id=${it.id} og nøkkel=${it.messageKey}", e)
                it.retryCount += 1
                if (it.retryCount >= maxRetry.toInt()) {
                    LOGGER.error(
                        "Har prossesert dead_letter_kafka melding med id ${it.id} - ${it.retryCount} ganger hvor MAX_RETRY=$maxRetry. Stopper reprossesering av melding ved å sette retry=false. En utvikler må sette retry=true og retry_count=0 for at melding skal prosesseres på nytt",
                        e,
                    )
                    it.retry = false
                }
                dlqKafkaRepository.save(it)
            }
        }
    }

    fun processMessage(message: DLQKafka) {
        when (message.topicName) {
            topicOppgaveHendelse -> {
                val oppgaveEndretHendelse = jsonMapperService.mapOppgaveHendelseV2(message.payload)
                behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveEndretHendelse)
            }
            topicJournalpost -> {
                val journalpostHendelse = jsonMapperService.mapJournalpostHendelse(message.payload)
                behandleHendelseService.behandleHendelse(journalpostHendelse)
            }
            else -> {
                throw KunneIkkeProsessereKafkaMelding(
                    "Behandler ikke melding ${message.id} fordi det finnes ingen støtte for å behandle meldinger med topicName ${message.topicName}",
                )
            }
        }
    }
}
