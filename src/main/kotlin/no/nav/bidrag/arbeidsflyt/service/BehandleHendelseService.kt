package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.consumer.PersonConsumer
import no.nav.bidrag.arbeidsflyt.model.BehandleJournalpostHendelse
import no.nav.bidrag.arbeidsflyt.utils.numericOnly
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BehandleHendelseService(private val arbeidsfordelingService: ArbeidsfordelingService, private val oppgaveService: OppgaveService, private val persistenceService: PersistenceService, private val personConsumer: PersonConsumer) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BehandleHendelseService::class.java)
    }

    fun behandleHendelse(journalpostHendelse: JournalpostHendelse) {
        LOGGER.info("Behandler journalpostHendelse: ${journalpostHendelse.printSummary()}")
        SECURE_LOGGER.info("Behandler journalpostHendelse: $journalpostHendelse")

        populerMedAktoerIdHvisMangler(journalpostHendelse)

        persistenceService.lagreEllerOppdaterJournalpostFraHendelse(journalpostHendelse)

        BehandleJournalpostHendelse(journalpostHendelse, oppgaveService, arbeidsfordelingService)
            .oppdaterEksterntFagomrade()
            .oppdaterEndretEnhetsnummer()
            .oppdaterOppgaveMedAktoerId()
            .opprettJournalforingsoppgave()
            .ferdigstillJournalforingsoppgaver()
            .opprettEllerEndreBehandleDokumentOppgaver()
    }

    fun populerMedAktoerIdHvisMangler(journalpostHendelse: JournalpostHendelse){
        if (journalpostHendelse.aktorId.isNullOrEmpty() && !journalpostHendelse.fnr.isNullOrEmpty()){
            LOGGER.info("Hendelse mangler aktørid. Henter og oppdaterer hendelsedata med aktørid")
            personConsumer.hentPerson(journalpostHendelse.fnr?.numericOnly())
                .ifPresent { journalpostHendelse.aktorId = it.aktoerId }
            SECURE_LOGGER.info("Hendelse manglet aktørid. Hentet og oppdatert hendelsedata med aktørid ${journalpostHendelse.aktorId} og fnr ${journalpostHendelse.fnr}")
        }
    }
}
