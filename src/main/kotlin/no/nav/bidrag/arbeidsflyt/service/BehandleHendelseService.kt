package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.model.OppdaterOppgaver
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.repository.JournalpostRepository
import no.nav.bidrag.arbeidsflyt.utils.FeatureToggle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandleHendelseService(private val oppgaveService: OppgaveService, private val journalpostRepository: JournalpostRepository, private val featureToggle: FeatureToggle) {
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
        if (!featureToggle.isFeatureEnabled(FeatureToggle.Feature.LAGRE_JOURNALPOST)){
            return
        }
        try {
            LOGGER.info("Lagrer journalpost ${journalpostHendelse.journalpostId} fra hendelse")
            val existing = journalpostRepository.findByJournalpostIdContaining(journalpostId = journalpostHendelse.journalpostId)
            if (existing.isPresent){
                val journalpost = existing.get()
                journalpost.status = journalpostHendelse.journalstatus ?: journalpost.status
                journalpost.enhet = journalpostHendelse.enhet ?: journalpost.enhet
                journalpost.tema = journalpostHendelse.fagomrade ?: journalpost.tema
                journalpost.gjelderId = journalpostHendelse.aktorId ?: journalpost.gjelderId
                journalpostRepository.save(journalpost)
            } else {
                journalpostRepository.save(Journalpost(
                    journalpostId = if (journalpostHendelse.harJournalpostIdJOARKPrefix()) journalpostHendelse.journalpostIdUtenPrefix else journalpostHendelse.journalpostId,
                    status = journalpostHendelse.journalstatus,
                    tema = journalpostHendelse.fagomrade,
                    enhet = journalpostHendelse.enhet,
                    gjelderId = journalpostHendelse.aktorId
                ))
            }
        } catch (e: Exception){
            LOGGER.error("Det skjedde en feil ved lagring av journalpost ${journalpostHendelse.journalpostId} fra hendelse", e)
        }

    }
}
