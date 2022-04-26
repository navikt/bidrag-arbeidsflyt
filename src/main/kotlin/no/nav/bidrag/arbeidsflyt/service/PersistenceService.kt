package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.PersonConsumer
import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.persistence.entity.DLKafka
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.arbeidsflyt.persistence.repository.DLKafkaRepository
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
    private val dlKafkaRepository: DLKafkaRepository,
    private val journalpostRepository: JournalpostRepository,
    private val featureToggle: FeatureToggle,
    private val personConsumer: PersonConsumer
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(PersistenceService::class.java)
    }

    fun hentOppgave(oppgaveId: Long): Optional<Oppgave> {
        return oppgaveRepository.findByOppgaveId(oppgaveId)
    }

    fun hentJournalpostMedStatusMottatt(journalpostId: String): Optional<Journalpost> {
        val harJournalpostIdPrefiks = journalpostId.contains("-")
        val journalpostIdUtenPrefiks = if (harJournalpostIdPrefiks) journalpostId.split('-')[1] else journalpostId
        return journalpostRepository.findByJournalpostIdContaining(journalpostIdUtenPrefiks)
            .filter{ it.erStatusMottatt && it.erBidragFagomrade }
    }

    @Transactional
    fun lagreDLKafka(topic: String, key: String?, payload: String){
        try {
            dlKafkaRepository.save(DLKafka(
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
            statuskategori = oppgaveHendelse.statuskategori?.name!!,
            journalpostId = oppgaveHendelse.journalpostId,
            tema = oppgaveHendelse.tema!!
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
        val gjelderId = personConsumer.hentPerson(journalpostHendelse.aktorId)
        journalpostRepository.findByJournalpostId(journalpostId)
            .ifPresentOrElse({
                it.status = journalpostHendelse.journalstatus ?: it.status
                it.enhet = journalpostHendelse.enhet ?: it.enhet
                it.tema = journalpostHendelse.fagomrade ?: it.tema
                it.gjelderId = gjelderId?.ident ?: it.gjelderId
                journalpostRepository.save(it)
            }, {
                journalpostRepository.save(
                    Journalpost(
                        journalpostId = journalpostId,
                        status = journalpostHendelse.journalstatus ?: "UKJENT",
                        tema = journalpostHendelse.fagomrade ?: "BID",
                        enhet = journalpostHendelse.enhet ?: "UKJENT",
                        gjelderId = gjelderId?.ident ?: journalpostHendelse.aktorId
                    )
                )
            })
    }

    fun deleteJournalpost(journalpostId: String){
        journalpostRepository.deleteByJournalpostId(journalpostId)
    }

}