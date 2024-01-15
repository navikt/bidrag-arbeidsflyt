package no.nav.bidrag.arbeidsflyt.service

import mu.KotlinLogging
import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.consumer.BidragTIlgangskontrollConsumer
import no.nav.bidrag.arbeidsflyt.consumer.PersonConsumer
import no.nav.bidrag.arbeidsflyt.model.BehandleJournalpostHendelse
import no.nav.bidrag.arbeidsflyt.utils.numericOnly
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import org.springframework.stereotype.Service

private val LOGGER = KotlinLogging.logger {}

@Service
class BehandleHendelseService(
    private val arbeidsfordelingService: OrganisasjonService,
    private val oppgaveService: OppgaveService,
    private val personConsumer: PersonConsumer,
    private val persistenceService: PersistenceService,
    private val tIlgangskontrollConsumer: BidragTIlgangskontrollConsumer,
) {
    fun behandleHendelse(journalpostHendelse: JournalpostHendelse) {
        LOGGER.info("Behandler journalpostHendelse: ${journalpostHendelse.printSummary()}")
        SECURE_LOGGER.info("Behandler journalpostHendelse: $journalpostHendelse")
        if (journalpostHendelse.erForsendelse()) {
            LOGGER.info("Ignorer journalpostHendelse med id ${journalpostHendelse.journalpostId}. Hendelsen gjelder forsendelse")
            return
        }
        val journalpostHendelseMedAktorId = populerMedAktoerIdHvisMangler(journalpostHendelse)

        BehandleJournalpostHendelse(
            journalpostHendelseMedAktorId,
            oppgaveService,
            arbeidsfordelingService,
            persistenceService,
            tIlgangskontrollConsumer,
        )
            .oppdaterEksterntFagomrade()
            .oppdaterEndretEnhetsnummer()
            .oppdaterOverførMellomBidragFagomrader()
            .oppdaterOppgaveMedAktoerId()
            .opprettJournalforingsoppgave()
            .ferdigstillJournalforingsoppgaver()
            .opprettEllerEndreBehandleDokumentOppgaver()

        persistenceService.lagreEllerOppdaterJournalpostFraHendelse(journalpostHendelse)
    }

    fun populerMedAktoerIdHvisMangler(journalpostHendelse: JournalpostHendelse): JournalpostHendelse {
        if (journalpostHendelse.aktorId.isNullOrEmpty() && !journalpostHendelse.fnr.isNullOrEmpty()) {
            LOGGER.info("Hendelse mangler aktørid. Henter og oppdaterer hendelsedata med aktørid")
            return personConsumer.hentPerson(journalpostHendelse.fnr?.numericOnly())?.let {
                SECURE_LOGGER.info("Hendelse manglet aktørid. Hentet og oppdatert hendelsedata med aktørid ${it.aktørId} og fnr ${journalpostHendelse.fnr}")
                journalpostHendelse.copy(aktorId = it.aktørId)
            } ?: journalpostHendelse
        }
        return journalpostHendelse
    }
}
