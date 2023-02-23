package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.arbeidsflyt.persistence.repository.DLQKafkaRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.OppgaveRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Optional
import javax.transaction.Transactional

@Service
class PersistenceService(
    private val oppgaveRepository: OppgaveRepository,
    private val dlqKafkaRepository: DLQKafkaRepository
){

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(PersistenceService::class.java)
    }

    fun hentJournalforingOppgave(oppgaveId: Long): Optional<Oppgave> {
        return oppgaveRepository.findByOppgaveId(oppgaveId).filter { it.erJournalforingOppgave() }
    }

    @Transactional
    fun lagreDLQKafka(topic: String, key: String?, payload: String, retry: Boolean = true){
        try {
            dlqKafkaRepository.save(DLQKafka(
                messageKey = key ?: "UKJENT",
                topicName = topic,
                payload = payload,
                retry = retry
            ))
        } catch (e: Exception){
            LOGGER.error("Det skjedde en feil ved lagring av feilet kafka melding", e)
        }
    }

    @Transactional
    fun lagreJournalforingsOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse){
        if (!oppgaveHendelse.erJournalforingOppgave){
            LOGGER.debug("Oppgave ${oppgaveHendelse.id} har oppgavetype ${oppgaveHendelse.oppgavetype}. Skal bare lagre oppgaver med type JFR. Lagrer ikke oppgave")
            return
        }
        val oppgave = Oppgave(
            oppgaveId = oppgaveHendelse.id,
            oppgavetype =  oppgaveHendelse.oppgavetype!!,
            status = oppgaveHendelse.status?.name!!,
            journalpostId = oppgaveHendelse.journalpostId
        )
        oppgaveRepository.save(oppgave)
        LOGGER.info("Lagret oppgave med id ${oppgaveHendelse.id} i databasen.")
    }

    @Transactional
    fun oppdaterEllerSlettOppgaveMetadataFraHendelse(oppgaveHendelse: OppgaveHendelse){
        if (oppgaveHendelse.erAapenJournalforingsoppgave()){
            oppgaveRepository.findById(oppgaveHendelse.id)
                .ifPresentOrElse({
                    LOGGER.info("Oppdaterer oppgave ${oppgaveHendelse.id} i databasen")
                    it.oppdaterOppgaveFraHendelse(oppgaveHendelse)
                    oppgaveRepository.save(it)
                }, {
                    LOGGER.info("Fant ingen oppgave med id ${oppgaveHendelse.id} i databasen. Lagrer opppgave")
                    lagreJournalforingsOppgaveFraHendelse(oppgaveHendelse)
                })
        } else {
            oppgaveRepository.findById(oppgaveHendelse.id).ifPresent {
                oppgaveRepository.deleteByOppgaveId(oppgaveHendelse.id)
                LOGGER.info("Slettet oppgave ${oppgaveHendelse.id} fra databasen fordi oppgave ikke lenger er åpen journalføringsoppgave")
            }
        }

    }

    @Transactional
    fun slettFeiledeMeldingerMedOppgaveid(oppgaveid: Long){
        try {
            dlqKafkaRepository.deleteByMessageKey(oppgaveid.toString())
        } catch (e: Exception){
            LOGGER.error("Det skjedde en feil ved sletting av feilede meldinger med oppgaveid $oppgaveid", e)
        }
    }

    @Transactional
    fun slettFeiledeMeldingerMedJournalpostId(journalpostId: String){
        try {
            dlqKafkaRepository.deleteByMessageKey(journalpostId)
        } catch (e: Exception){
            LOGGER.error("Det skjedde en feil ved sletting av feilede meldinger med journalpostid $journalpostId", e)
        }
    }

}