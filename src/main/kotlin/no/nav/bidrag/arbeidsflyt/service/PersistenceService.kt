package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.PersonConsumer
import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.arbeidsflyt.persistence.repository.JournalpostRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.OppgaveRepository
import no.nav.bidrag.arbeidsflyt.utils.FeatureToggle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class PersistenceService(
    private val journalpostRepository: JournalpostRepository,
    private val oppgaveRepository: OppgaveRepository,
    private val featureToggle: FeatureToggle,
    private val personConsumer: PersonConsumer
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(PersistenceService::class.java)
    }

    fun hentOppgave(oppgaveId: Long): Optional<Oppgave> {
        return oppgaveRepository.findById(oppgaveId)
    }

    fun hentJournalpost(journalpostId: String): Optional<Journalpost> {
        return journalpostRepository.findByJournalpostId(journalpostId)
    }

    fun opprettOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse){
        val oppgave = Oppgave(
            oppgaveId = oppgaveHendelse.id,
            oppgavetype =  oppgaveHendelse.oppgavetype!!,
            status = oppgaveHendelse.status?.name!!,
            journalpostId = oppgaveHendelse.journalpostId,
            tema = oppgaveHendelse.tema!!
        )
        oppgaveRepository.save(oppgave)
        LOGGER.info("Lagret opprettet oppgave ${oppgaveHendelse.id} i databasen.")
    }

    fun oppdaterOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse){
        oppgaveRepository.findById(oppgaveHendelse.id)
            .ifPresentOrElse({
                it.oppdaterOppgaveFraHendelse(oppgaveHendelse)
                oppgaveRepository.save(it)
            }, {
                LOGGER.info("Fant ingen oppgave med id ${oppgaveHendelse.id} i databasen. Lagrer opppgave")
                opprettOppgaveFraHendelse(oppgaveHendelse)
            })
    }

    fun oppdaterJournalpostFraHendelse(journalpostHendelse: JournalpostHendelse){
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
                it.gjelderId = gjelderId.ident ?: it.gjelderId
                journalpostRepository.save(it)
            }, {
                journalpostRepository.save(
                    Journalpost(
                    journalpostId = journalpostId,
                    status = journalpostHendelse.journalstatus ?: "UKJENT",
                    tema = journalpostHendelse.fagomrade ?: "UKJENT",
                    enhet = journalpostHendelse.enhet ?: "UKJENT",
                    gjelderId = gjelderId.ident
                )
                )
            })
    }

}