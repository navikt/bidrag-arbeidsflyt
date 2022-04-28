package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.arbeidsflyt.persistence.repository.DLQKafkaRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.JournalpostRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.OppgaveRepository
import no.nav.bidrag.arbeidsflyt.utils.FeatureToggle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Optional
import javax.transaction.Transactional

@Service
class PersistenceService(
    private val oppgaveRepository: OppgaveRepository,
    private val dlqKafkaRepository: DLQKafkaRepository,
    private val journalpostRepository: JournalpostRepository,
    private val featureToggle: FeatureToggle
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(PersistenceService::class.java)
    }

    fun hentJournalforingOppgave(oppgaveId: Long): Optional<Oppgave> {
        return oppgaveRepository.findByOppgaveId(oppgaveId).filter { it.erJournalforingOppgave() }
    }

    fun hentJournalpostMedStatusMottatt(journalpostId: String): Optional<Journalpost> {
        val harJournalpostIdPrefiks = journalpostId.contains("-")
        val journalpostIdUtenPrefiks = if (harJournalpostIdPrefiks) journalpostId.split('-')[1] else journalpostId
        return journalpostRepository.findByJournalpostIdContaining(journalpostIdUtenPrefiks)
            .filter{ it.erStatusMottatt && it.erBidragFagomrade }
    }

    @Transactional
    fun lagreDLQKafka(topic: String, key: String?, payload: String){
        try {
            dlqKafkaRepository.save(DLQKafka(
                messageKey = key ?: "UKJENT",
                topicName = topic,
                payload = payload
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
            oppgaveRepository.deleteByOppgaveId(oppgaveHendelse.id)
        }

    }

    @Transactional
    fun lagreEllerOppdaterJournalpostFraHendelse(journalpostHendelse: JournalpostHendelse){
        if (!featureToggle.isFeatureEnabled(FeatureToggle.Feature.LAGRE_JOURNALPOST)){
            return
        }

        val journalpostId = if (journalpostHendelse.erJoarkJournalpost()) journalpostHendelse.journalpostIdUtenPrefix else journalpostHendelse.journalpostId


        if (!journalpostHendelse.erMottaksregistrert || journalpostHendelse.erEksterntFagomrade){
            LOGGER.info("Sletter journalpost $journalpostId fordi status ikke lenger er MOTTATT eller er endret til ekstern fagområde (status=${journalpostHendelse.journalstatus}, fagomrade=${journalpostHendelse.fagomrade})")
            deleteJournalpost(journalpostId)
        } else {
            LOGGER.info("Lagrer journalpost $journalpostId fra hendelse")
            saveOrUpdateMottattJournalpost(journalpostId, journalpostHendelse)
        }
    }

    fun saveOrUpdateMottattJournalpost(journalpostId: String, journalpostHendelse: JournalpostHendelse){
        journalpostRepository.findByJournalpostId(journalpostId)
            .ifPresentOrElse({
                it.status = journalpostHendelse.journalstatus ?: it.status
                it.enhet = journalpostHendelse.enhet ?: it.enhet
                it.tema = journalpostHendelse.fagomrade ?: it.tema
                journalpostRepository.save(it)
            }, {
                journalpostRepository.save(
                    Journalpost(
                        journalpostId = journalpostId,
                        status = journalpostHendelse.journalstatus ?: "UKJENT",
                        tema = journalpostHendelse.fagomrade ?: "BID",
                        enhet = journalpostHendelse.enhet ?: "UKJENT",
                    )
                )
            })
    }

    fun deleteJournalpost(journalpostId: String){
        journalpostRepository.deleteByJournalpostId(journalpostId)
    }

}