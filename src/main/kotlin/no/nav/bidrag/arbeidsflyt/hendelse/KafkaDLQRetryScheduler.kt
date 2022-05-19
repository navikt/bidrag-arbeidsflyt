package no.nav.bidrag.arbeidsflyt.hendelse

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.arbeidsflyt.model.KunneIkkeProsessereKafkaMelding
import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import no.nav.bidrag.arbeidsflyt.persistence.repository.DLQKafkaRepository
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.BehandleOppgaveHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class KafkaDLQRetryScheduler(
    private val jsonMapperService: JsonMapperService,
    private val dlqKafkaRepository: DLQKafkaRepository,
    private val behandleOppgaveHendelseService: BehandleOppgaveHendelseService,
    private val behandleHendelseService: BehandleHendelseService
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(KafkaDLQRetryScheduler::class.java)
    }

    @Value("\${TOPIC_OPPGAVE_ENDRET}")
    lateinit var topicOppgaveEndret: String;
    @Value("\${TOPIC_JOURNALPOST}")
    lateinit var topicJournalpost: String;

    @Scheduled(cron = "0 */2 * ? * *")
    @SchedulerLock(name = "processKafkaDLQMessages", lockAtMostFor = "50m", lockAtLeastFor = "10m")
    @Transactional
    fun processMessages(){

        val messages = dlqKafkaRepository.findByRetryTrue()
        LOGGER.info("KafkaDLQRetryScheduler fant ${messages.size} meldinger som skal prosesseres")

        messages.stream().forEach {
            try {
                LOGGER.info("Behandler melding ${it.id} med topicName ${it.topicName} og nøkkel ${it.messageKey} ")
                processMessage(it)
                dlqKafkaRepository.delete(it)
            } catch (e: Exception){
                LOGGER.error("Det skjedde feil ved prosessering av melding med id=${it.id} og nøkkel=${it.messageKey}", e)
                it.retry = false
                dlqKafkaRepository.save(it)
            }
        }

    }

    fun processMessage(message: DLQKafka) {
        when (message.topicName) {
            topicOppgaveEndret -> {
                val oppgaveEndretHendelse = jsonMapperService.mapOppgaveHendelse(message.payload)
                behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveEndretHendelse)
            }
            topicJournalpost -> {
                val journalpostHendelse = jsonMapperService.mapJournalpostHendelse(message.payload)
                behandleHendelseService.behandleHendelse(journalpostHendelse)
            }
            else -> {
                throw KunneIkkeProsessereKafkaMelding("Behandler ikke melding ${message.id} fordi det finnes ingen støtte for å behandle meldinger med topicName ${message.topicName}")
            }
        }
    }
}