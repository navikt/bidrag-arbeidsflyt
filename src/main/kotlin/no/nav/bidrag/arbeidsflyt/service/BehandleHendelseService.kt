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

        lagreJournalpost(journalpostHendelse)

        OppdaterOppgaver(journalpostHendelse, oppgaveService)
            .oppdaterEksterntFagomrade()
            .oppdaterEndretEnhetsnummer()
            .oppdaterOppgaveMedAktoerId()
            .opprettJournalforingsoppgave()
            .ferdigstillJournalforingsoppgaver()

    }

    fun lagreJournalpost(journalpostHendelse: JournalpostHendelse){
        try {
            val existing = journalpostRepository.findByJournalpostIdContaining(journalpostId = journalpostHendelse.journalpostId)
            if (existing.isPresent){
                val journalpost = existing.get()
                journalpost.status = journalpostHendelse.journalstatus ?: journalpost.status
                journalpostRepository.save(journalpost)
            } else {
                journalpostRepository.save(Journalpost(
                    journalpostId = if (journalpostHendelse.harJournalpostIdJOARKPrefix()) journalpostHendelse.hentJournalpostIdUtenPrefix() else journalpostHendelse.journalpostId,
                    status = journalpostHendelse.journalstatus
                ))
            }
        } catch (e: Exception){
            LOGGER.error("Det skjedde en feil ved lagring av journalpost ${journalpostHendelse.journalpostId} fra hendelse", e)
        }

    }
}
