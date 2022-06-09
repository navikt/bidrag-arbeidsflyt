package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.consumer.PersonConsumer
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.model.OppdaterOppgaver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BehandleHendelseService(private val geografiskEnhetService: GeografiskEnhetService, private val oppgaveService: OppgaveService, private val persistenceService: PersistenceService, private val personConsumer: PersonConsumer) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BehandleHendelseService::class.java)
    }

    fun behandleHendelse(journalpostHendelse: JournalpostHendelse) {
        LOGGER.info("Behandler journalpostHendelse: ${journalpostHendelse.printSummary()}")
        SECURE_LOGGER.info("Behandler journalpostHendelse: $journalpostHendelse")

        populerMedAktoerIdHvisMangler(journalpostHendelse)

        persistenceService.lagreEllerOppdaterJournalpostFraHendelse(journalpostHendelse)

        OppdaterOppgaver(journalpostHendelse, oppgaveService, geografiskEnhetService)
            .oppdaterEksterntFagomrade()
            .oppdaterEndretEnhetsnummer()
            .oppdaterOppgaveMedAktoerId()
            .opprettJournalforingsoppgave()
            .ferdigstillJournalforingsoppgaver()
    }

    fun populerMedAktoerIdHvisMangler(journalpostHendelse: JournalpostHendelse){
        if (journalpostHendelse.aktorId.isNullOrEmpty() && !journalpostHendelse.fnr.isNullOrEmpty()){
            personConsumer.hentPerson(journalpostHendelse.fnr)
                .ifPresent { journalpostHendelse.aktorId = it.aktoerId }
        }
    }
}
