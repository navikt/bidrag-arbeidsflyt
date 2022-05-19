package no.nav.bidrag.arbeidsflyt.hendelse

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
    @Transactional
    fun processMessages(){

        val messages = dlqKafkaRepository.findByRetryTrue()
        LOGGER.info("KafkaDLQRetryScheduler fant ${messages.size} meldinger som skal prosesseres")

        messages.stream().forEach {
            LOGGER.info("Behandler melding ${it.id} med topicName ${it.topicName} og nøkkel ${it.messageKey} ")
            when (it.topicName) {
                topicOppgaveEndret -> {
                    val oppgaveEndretHendelse = jsonMapperService.mapOppgaveHendelse(it.payload)
                    behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveEndretHendelse)
                }
                topicJournalpost -> {
                    val journalpostHendelse = jsonMapperService.mapJournalpostHendelse(it.payload)
                    behandleHendelseService.behandleHendelse(journalpostHendelse)
                }
                else -> {
                    LOGGER.warn("Behandler ikke melding ${it.id} fordi det finnes ingen støtte for å behandle meldinger med topicName ${it.topicName} ")
                }
            }
            it.retry = false
            dlqKafkaRepository.save(it)
        }

    }
}