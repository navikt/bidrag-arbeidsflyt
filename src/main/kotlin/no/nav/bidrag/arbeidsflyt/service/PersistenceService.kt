package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.PersonConsumer
import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.dto.Oppgavestatuskategori
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.arbeidsflyt.persistence.repository.JournalpostRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.OppgaveRepository
import no.nav.bidrag.arbeidsflyt.utils.FeatureToggle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Optional
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.transaction.Transactional

@Service
class PersistenceService(
    private val journalpostRepository: JournalpostRepository,
    private val oppgaveRepository: OppgaveRepository,
    private val featureToggle: FeatureToggle,
    private val personConsumer: PersonConsumer
) {

    @PersistenceContext
    lateinit var entityManager: EntityManager

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(PersistenceService::class.java)
    }

    fun hentOppgave(oppgaveId: Long): Optional<Oppgave> {
        return oppgaveRepository.findById(oppgaveId)
    }

    fun hentOppgaveDetached(oppgaveId: Long): Optional<Oppgave> {
        val oppgave = oppgaveRepository.findById(oppgaveId)
        oppgave.ifPresent { entityManager.detach(it) }
        return oppgave
    }

    fun hentJournalpost(journalpostId: String): Optional<Journalpost> {
        return journalpostRepository.findByJournalpostIdContaining(journalpostId)
    }

    fun finnAapneJournalforingsOppgaver(journalpostId: String): List<Oppgave>{
        val harJournalpostIdPrefiks = journalpostId.contains("-")
        val journalpostIdUtenPrefiks = if (harJournalpostIdPrefiks) journalpostId.split('-')[1] else journalpostId
        return oppgaveRepository.findAllByJournalpostIdContainingAndStatuskategoriAndOppgavetype(journalpostIdUtenPrefiks, Oppgavestatuskategori.AAPEN.name, "JFR")
    }

    @Transactional
    fun lagreOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse){
        if (!oppgaveHendelse.erJournalforingOppgave){
            LOGGER.info("Oppgave ${oppgaveHendelse.id} har oppgavetype ${oppgaveHendelse.oppgavetype}. Skal bare lagre oppgaver med type JFR. Lagrer ikke oppgave")
            return
        }
        val oppgave = Oppgave(
            oppgaveId = oppgaveHendelse.id,
            oppgavetype =  oppgaveHendelse.oppgavetype!!,
            status = oppgaveHendelse.status?.name!!,
            statuskategori = oppgaveHendelse.statuskategori?.name!!,
            journalpostId = oppgaveHendelse.journalpostId,
            tema = oppgaveHendelse.tema!!,
            ident = oppgaveHendelse.hentIdent
        )
        oppgaveRepository.save(oppgave)
        LOGGER.info("Lagret oppgave med id ${oppgaveHendelse.id} i databasen.")
    }

    @Transactional
    fun oppdaterOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse){
        oppgaveRepository.findById(oppgaveHendelse.id)
            .ifPresentOrElse({
                LOGGER.info("Oppdaterer oppgave ${oppgaveHendelse.id} i databasen")
                it.oppdaterOppgaveFraHendelse(oppgaveHendelse)
                oppgaveRepository.save(it)
            }, {
                LOGGER.info("Fant ingen oppgave med id ${oppgaveHendelse.id} i databasen. Lagrer opppgave")
                lagreOppgaveFraHendelse(oppgaveHendelse)
            })
    }

    fun lagreEllerOppdaterJournalpostFraHendelse(journalpostHendelse: JournalpostHendelse){
        if (!featureToggle.isFeatureEnabled(FeatureToggle.Feature.LAGRE_JOURNALPOST)){
            return
        }

        val journalpostId = if (journalpostHendelse.harJournalpostIdJOARKPrefix()) journalpostHendelse.journalpostIdUtenPrefix else journalpostHendelse.journalpostId

        val gjelderId = personConsumer.hentPerson(journalpostHendelse.aktorId)

        LOGGER.info("Lagrer journalpost ${journalpostHendelse.journalpostId} fra hendelse")
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

}