package no.nav.bidrag.arbeidsflyt.service

import jakarta.transaction.Transactional
import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.model.erEksterntFagomrade
import no.nav.bidrag.arbeidsflyt.model.erMottattStatus
import no.nav.bidrag.arbeidsflyt.model.hentTema
import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.arbeidsflyt.persistence.repository.DLQKafkaRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.JournalpostRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.OppgaveRepository
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PersistenceService(
    private val oppgaveRepository: OppgaveRepository,
    private val dlqKafkaRepository: DLQKafkaRepository,
    private val journalpostRepository: JournalpostRepository
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(PersistenceService::class.java)
    }

    fun hentJournalforingOppgave(oppgaveId: Long): Oppgave? {
        return oppgaveRepository.findByOppgaveId(oppgaveId)?.takeIf { it.erJournalforingOppgave() }
    }

    fun hentJournalpostMedStatusMottatt(journalpostId: String): Journalpost? {
        return journalpostRepository.findByJournalpostId(journalpostId)?.takeIf { it.erStatusMottatt && it.erBidragFagomrade }
    }

    @Transactional
    fun lagreEllerOppdaterJournalpostFraHendelse(journalpostHendelse: JournalpostHendelse) {
        val journalpostId = journalpostHendelse.journalpostId
        if (!journalpostHendelse.erMottattStatus || journalpostHendelse.erEksterntFagomrade) {
            deleteJournalpost(journalpostId)
            LOGGER.info("Slettet journalpost $journalpostId fra hendelse fra databasen fordi status ikke lenger er MOTTATT eller er endret til ekstern fagområde (status=${journalpostHendelse.hentStatus()}, fagomrade=${journalpostHendelse.hentTema()})")
        } else {
            saveOrUpdateMottattJournalpost(journalpostId, journalpostHendelse)
            LOGGER.info("Lagret journalpost $journalpostId i databasen")
        }
    }

    @Transactional
    fun lagreDLQKafka(topic: String, key: String?, payload: String, retry: Boolean = true) {
        try {
            dlqKafkaRepository.save(
                DLQKafka(
                    messageKey = key ?: "UKJENT",
                    topicName = topic,
                    payload = payload,
                    retry = retry
                )
            )
        } catch (e: Exception) {
            LOGGER.error("Det skjedde en feil ved lagring av feilet kafka melding", e)
        }
    }

    @Transactional
    fun lagreJournalforingsOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse) {
        if (!oppgaveHendelse.erJournalforingOppgave) {
            LOGGER.debug("Oppgave ${oppgaveHendelse.id} har oppgavetype ${oppgaveHendelse.oppgavetype}. Skal bare lagre oppgaver med type JFR. Lagrer ikke oppgave")
            return
        }
        val oppgave = Oppgave(
            oppgaveId = oppgaveHendelse.id,
            oppgavetype = oppgaveHendelse.oppgavetype!!,
            status = oppgaveHendelse.status?.name!!,
            journalpostId = oppgaveHendelse.journalpostId
        )
        oppgaveRepository.save(oppgave)
        LOGGER.info("Lagret oppgave med id ${oppgaveHendelse.id} i databasen.")
    }

    @Transactional
    fun oppdaterEllerSlettOppgaveMetadataFraHendelse(oppgaveHendelse: OppgaveHendelse) {
        if (oppgaveHendelse.erAapenJournalforingsoppgave()) {
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
    fun slettFeiledeMeldingerMedOppgaveid(oppgaveid: Long) {
        try {
            dlqKafkaRepository.deleteByMessageKey(oppgaveid.toString())
        } catch (e: Exception) {
            LOGGER.error("Det skjedde en feil ved sletting av feilede meldinger med oppgaveid $oppgaveid", e)
        }
    }

    @Transactional
    fun slettFeiledeMeldingerMedJournalpostId(journalpostId: String) {
        try {
            dlqKafkaRepository.deleteByMessageKey(journalpostId)
        } catch (e: Exception) {
            LOGGER.error("Det skjedde en feil ved sletting av feilede meldinger med journalpostid $journalpostId", e)
        }
    }

    fun saveOrUpdateMottattJournalpost(journalpostId: String, journalpostHendelse: JournalpostHendelse) {
        journalpostRepository.findByJournalpostId(journalpostId)?.run {
            journalpostRepository.save(
                copy(
                    status = journalpostHendelse.hentStatus()?.name ?: this.status,
                    enhet = journalpostHendelse.enhet ?: this.enhet,
                    tema = journalpostHendelse.hentTema() ?: this.tema
                )
            )
        } ?: run {
            journalpostRepository.save(
                Journalpost(
                    journalpostId = journalpostId,
                    status = journalpostHendelse.hentStatus()?.name ?: "UKJENT",
                    tema = journalpostHendelse.hentTema() ?: "BID",
                    enhet = journalpostHendelse.enhet ?: "UKJENT"
                )
            )
        }
    }

    fun deleteJournalpost(journalpostId: String) {
        journalpostRepository.deleteByJournalpostId(journalpostId)
    }
}
