package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.model.OppdaterOppgaver
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.repository.JournalpostRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class BehandleHendelseService(private val oppgaveService: OppgaveService, private val journalpostRepository: JournalpostRepository) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BehandleHendelseService::class.java)
    }

    @Transactional
    fun behandleHendelse(journalpostHendelse: JournalpostHendelse) {
        LOGGER.info("Behandler journalpostHendelse: $journalpostHendelse")

        OppdaterOppgaver(journalpostHendelse, oppgaveService)
            .oppdaterEksterntFagomrade()
            .oppdaterEndretEnhetsnummer()
            .oppdaterOppgaveMedAktoerId()
            .opprettJournalforingsoppgave()
            .ferdigstillJournalforingsoppgaver()

        lagreJournalpost(journalpostHendelse)
    }

    fun lagreJournalpost(journalpostHendelse: JournalpostHendelse){
        val existing = journalpostRepository.findByJournalpostIdContaining(journalpostId = journalpostHendelse.journalpostId)
        if (existing.isPresent){
            val journalpost = existing.get()
            journalpost.status = journalpostHendelse.journalstatus ?: journalpost.status
            journalpostRepository.save(journalpost)
        } else {
            journalpostRepository.save(Journalpost(
                journalpostId = journalpostHendelse.journalpostId,
                status = journalpostHendelse.journalstatus
            ))
        }
    }
}
