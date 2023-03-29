package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.EndreForNyttDokumentRequest
import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppdaterOppgave
import no.nav.bidrag.arbeidsflyt.dto.OppdaterOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OpprettBehandleDokumentOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OverforOppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.OppgaveDataForHendelse
import no.nav.bidrag.arbeidsflyt.model.OppgaverForHendelse
import no.nav.bidrag.arbeidsflyt.model.journalpostIdUtenPrefix
import no.nav.bidrag.arbeidsflyt.model.journalpostMedPrefix
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OppgaveService(private val oppgaveConsumer: OppgaveConsumer) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(OppgaveService::class.java)
    }

    internal fun finnBehandlingsoppgaverForSaker(saker: List<String>, tema: String? = null): OppgaverForHendelse {
        val oppgaveSokRequest = OppgaveSokRequest()
            .brukBehandlingSomOppgaveType()
            .leggTilSaksreferanser(saker)

        if (tema != null) {
            oppgaveSokRequest.leggTilFagomrade(tema)
        }

        return OppgaverForHendelse(
            oppgaveConsumer.finnOppgaverForJournalpost(oppgaveSokRequest).oppgaver
                .map { OppgaveDataForHendelse(it) }
        )
    }

    internal fun finnAapneJournalforingOppgaverForJournalpost(journalpostId: String): OppgaverForHendelse {
        val oppgaveSokRequest = OppgaveSokRequest()
            .leggTilJournalpostId(journalpostId)

        return OppgaverForHendelse(
            oppgaveConsumer.finnOppgaverForJournalpost(oppgaveSokRequest).oppgaver
                .map { OppgaveDataForHendelse(it) }
        )
    }

    internal fun oppdaterOppgaver(oppgaverForHendelse: OppgaverForHendelse, journalpostHendelse: JournalpostHendelse) {
        oppgaverForHendelse.dataForHendelse.forEach {
            oppgaveConsumer.endreOppgave(
                endretAvEnhetsnummer = journalpostHendelse.hentEndretAvEnhetsnummer(),
                patchOppgaveRequest = OppdaterOppgaveRequest(it, journalpostHendelse.aktorId)
            )
        }
    }

    internal fun oppdaterOppgave(oppdaterOppgave: OppdaterOppgave, endretAvEnhetsnummer: String? = null) {
        oppgaveConsumer.endreOppgave(oppdaterOppgave, endretAvEnhetsnummer)
    }

    internal fun overforOppgaver(oppgaverForHendelse: OppgaverForHendelse, journalpostHendelse: JournalpostHendelse) {
        oppgaverForHendelse.dataForHendelse.forEach {
            oppgaveConsumer.endreOppgave(
                endretAvEnhetsnummer = journalpostHendelse.hentEndretAvEnhetsnummer(),
                patchOppgaveRequest = OverforOppgaveRequest(it, journalpostHendelse.enhet ?: "na", journalpostHendelse.hentSaksbehandlerInfo())
            )
        }
    }

    internal fun ferdigstillJournalforingsOppgaver(endretAvEnhetsnummer: String?, oppgaverForHendelse: OppgaverForHendelse) {
        oppgaverForHendelse.hentJournalforingsOppgaver().forEach {
            LOGGER.info("Ferdigstiller oppgave med type ${it.oppgavetype} og journalpostId ${it.journalpostId}")
            oppgaveConsumer.endreOppgave(
                endretAvEnhetsnummer = endretAvEnhetsnummer,
                patchOppgaveRequest = FerdigstillOppgaveRequest(it)
            )
        }
    }

    internal fun opprettEllerEndreBehandleDokumentOppgaver(journalpostHendelse: JournalpostHendelse, behandlingsOppgaver: OppgaverForHendelse) {
        val oppgaverSomSkalEndres = behandlingsOppgaver.hentBehandleDokumentOppgaverSomSkalOppdateresForNyttDokument(journalpostHendelse.journalpostIdUtenPrefix)
        endreForNyttDokument(journalpostHendelse, oppgaverSomSkalEndres)

        val sakerSomKreverNyBehandleDokumentOppgave = behandlingsOppgaver.hentSakerSomKreverNyBehandleDokumentOppgave(journalpostHendelse.sakstilknytninger ?: emptyList())
        opprettBehandleDokumentOppgaveForSaker(journalpostHendelse, sakerSomKreverNyBehandleDokumentOppgave)
    }

    internal fun opprettBehandleDokumentOppgave(opprettBehandleDokumentOppgaveRequest: OpprettBehandleDokumentOppgaveRequest) {
        oppgaveConsumer.opprettOppgave(opprettBehandleDokumentOppgaveRequest)
    }

    internal fun opprettBehandleDokumentOppgaveForSaker(journalpostHendelse: JournalpostHendelse, saker: List<String>) {
        LOGGER.info("Antall behandle dokument oppgaver som skal opprettes: ${saker.size} for saker $saker")
        saker.forEach {
            LOGGER.info("Oppretter behandle dokument oppgave for sak $it og journalpostId ${journalpostHendelse.journalpostId}")
            opprettBehandleDokumentOppgave(
                OpprettBehandleDokumentOppgaveRequest(
                    saksreferanse = it,
                    _journalpostId = journalpostHendelse.journalpostMedPrefix,
                    aktoerId = journalpostHendelse.aktorId,
                    tittel = journalpostHendelse.tittel!!,
                    dokumentDato = journalpostHendelse.dokumentDato,
                    sporingsdata = journalpostHendelse.sporing!!,
                    saksbehandlersInfo = journalpostHendelse.hentSaksbehandlerInfo()
                )
            )
        }
    }
    internal fun endreForNyttDokument(journalpostHendelse: JournalpostHendelse, oppgaver: List<OppgaveDataForHendelse>) {
        LOGGER.info("Antall behandle dokument oppgaver som skal oppdateres: {}", oppgaver.size)

        for (oppgaveData in oppgaver) {
            val request = EndreForNyttDokumentRequest(oppgaveData, journalpostHendelse)
            oppgaveConsumer.endreOppgave(request)
            LOGGER.info("Endret beskrivelse for oppgave {}", oppgaveData.id)
            SECURE_LOGGER.info(
                "Endret beskrivelse for oppgave {} med beskrivelse: {}",
                oppgaveData.id,
                request.beskrivelse
            )
        }
    }

    internal fun opprettJournalforingOppgave(opprettJournalforingsOppgaveRequest: OpprettJournalforingsOppgaveRequest) {
        oppgaveConsumer.opprettOppgave(opprettJournalforingsOppgaveRequest)
    }
}
