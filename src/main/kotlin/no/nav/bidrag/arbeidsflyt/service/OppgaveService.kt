package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.DefaultOpprettOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.EndreForNyttDokumentRequest
import no.nav.bidrag.arbeidsflyt.dto.EndreMellomBidragFagomrader
import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppdaterOppgave
import no.nav.bidrag.arbeidsflyt.dto.OppdaterOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveType
import no.nav.bidrag.arbeidsflyt.dto.OpprettBehandleDokumentOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OverforOppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.Fagomrade
import no.nav.bidrag.arbeidsflyt.model.OppgaverForHendelse
import no.nav.bidrag.arbeidsflyt.model.journalpostIdUtenPrefix
import no.nav.bidrag.arbeidsflyt.model.journalpostMedPrefix
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OppgaveService(
    private val oppgaveConsumer: OppgaveConsumer,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(OppgaveService::class.java)
    }

    fun oppdaterAlleOppgaverSomTilhørerSammeBehandling(oppgave: OppgaveData) {
        if (oppgave.behandlingsid == null) return
        val oppgaver = finnOppgaverForBehandling(oppgave.behandlingsid!!.toLong())
        oppgaver.forEach {
            if (it.id == oppgave.id) return@forEach
            oppdaterOppgave(
                OppdaterOppgave(it)
                    .overforTilEnhet(oppgave.tildeltEnhetsnr!!),
            )
        }
    }

    internal fun finnOppgaverForBehandling(
        behandlingId: Long,
    ): List<OppgaveData> {
        val oppgaveSokRequest =
            OppgaveSokRequest()
                .leggTilBehandlingreferanse(behandlingId.toString())

        return oppgaveConsumer
            .søkOppgaver(
                oppgaveSokRequest
                    .copy()
                    .leggTilBehandlingreferanse(behandlingId.toString()),
            ).oppgaver
    }

    internal fun finnOppgaverForSøknad(
        søknadId: Long? = null,
        behandlingId: Long? = null,
        saksnr: String,
        tema: String = Fagomrade.BIDRAG,
        oppgaveType: OppgaveType,
    ): OppgaverForHendelse {
        val oppgaveSokRequest =
            OppgaveSokRequest()
                .leggTilOppgavetype(oppgaveType)
                .leggTilFagomrade(tema)
                .leggTilSaksreferanse(saksnr)

        val oppgaverForBehandling =
            if (behandlingId != null) {
                oppgaveConsumer
                    .søkOppgaver(
                        oppgaveSokRequest
                            .copy()
                            .leggTilBehandlingreferanse(behandlingId.toString()),
                    ).oppgaver
            } else {
                emptyList()
            }.filter { søknadId == null || it.søknadsid != null && it.søknadsid!!.toLong() == søknadId }
        val oppgaverForSøknad =
            if (søknadId != null) {
                oppgaveConsumer
                    .søkOppgaver(
                        oppgaveSokRequest
                            .copy()
                            .leggTilSøknadsreferanse(søknadId.toString()),
                    ).oppgaver
            } else {
                emptyList()
            }
        val andreOppgaver =
            if (søknadId == null && behandlingId == null) {
                oppgaveConsumer.søkOppgaver(oppgaveSokRequest).oppgaver
            } else {
                emptyList()
            }
        val alleOppgaver = (oppgaverForBehandling + oppgaverForSøknad + andreOppgaver).distinctBy { it.id }
        return OppgaverForHendelse(alleOppgaver)
    }

    internal fun finnBehandlingsoppgaverForSaker(
        saker: List<String>,
        tema: String? = null,
    ): OppgaverForHendelse {
        val oppgaveSokRequest =
            OppgaveSokRequest()
                .brukBehandlingSomOppgaveType()
                .leggTilSaksreferanser(saker)

        if (tema != null) {
            oppgaveSokRequest.leggTilFagomrade(tema)
        }

        return OppgaverForHendelse(oppgaveConsumer.søkOppgaver(oppgaveSokRequest).oppgaver)
    }

    internal fun finnAapneJournalforingOppgaverForJournalpost(journalpostId: String): OppgaverForHendelse {
        val oppgaveSokRequest =
            OppgaveSokRequest()
                .leggTilJournalpostId(journalpostId)

        return OppgaverForHendelse(
            oppgaveConsumer.søkOppgaver(oppgaveSokRequest).oppgaver,
        )
    }

    internal fun hentOppgave(oppgaveId: Long): OppgaveData = oppgaveConsumer.hentOppgave(oppgaveId)

    internal fun oppdaterOppgaver(
        oppgaverForHendelse: OppgaverForHendelse,
        journalpostHendelse: JournalpostHendelse,
    ) {
        oppgaverForHendelse.dataForHendelse.forEach {
            oppgaveConsumer.endreOppgave(
                endretAvEnhetsnummer = journalpostHendelse.hentEndretAvEnhetsnummer(),
                patchOppgaveRequest = OppdaterOppgaveRequest(it, journalpostHendelse.aktorId),
            )
        }
    }

    internal fun oppdaterOppgave(
        oppdaterOppgave: OppdaterOppgave,
        endretAvEnhetsnummer: String? = null,
    ) {
        oppdaterOppgave.oppdaterOppgaveBeskrivelse()
        oppgaveConsumer.endreOppgave(oppdaterOppgave, endretAvEnhetsnummer)
    }

    internal fun overforOppgaver(
        oppgaverForHendelse: OppgaverForHendelse,
        journalpostHendelse: JournalpostHendelse,
    ) {
        oppgaverForHendelse.dataForHendelse.forEach {
            oppgaveConsumer.endreOppgave(
                endretAvEnhetsnummer = journalpostHendelse.hentEndretAvEnhetsnummer(),
                patchOppgaveRequest =
                    OverforOppgaveRequest(
                        it,
                        journalpostHendelse.enhet ?: "na",
                        journalpostHendelse.hentSaksbehandlerInfo(),
                    ),
            )
        }
    }

    internal fun endreMellomBidragFagomrade(
        oppgaverForHendelse: OppgaverForHendelse,
        journalpostHendelse: JournalpostHendelse,
        fagomradeGammelt: String? = null,
        fagomradeNy: String,
        saksbehandlerHarTilgang: Boolean,
    ) {
        oppgaverForHendelse.hentJournalforingsOppgaver().forEach {
            oppgaveConsumer.endreOppgave(
                endretAvEnhetsnummer = journalpostHendelse.hentEndretAvEnhetsnummer(),
                patchOppgaveRequest =
                    EndreMellomBidragFagomrader(
                        it,
                        journalpostHendelse.hentSaksbehandlerInfo(),
                        fagomradeGammelt,
                        fagomradeNy,
                        overførTilFellesbenk = !saksbehandlerHarTilgang,
                    ),
            )
        }
    }

    internal fun ferdigstillJournalforingsOppgaver(
        endretAvEnhetsnummer: String?,
        oppgaverForHendelse: OppgaverForHendelse,
    ) {
        oppgaverForHendelse.hentJournalforingsOppgaver().forEach {
            LOGGER.info("Ferdigstiller oppgave med type ${it.oppgavetype} og journalpostId ${it.journalpostId}")
            oppgaveConsumer.endreOppgave(
                endretAvEnhetsnummer = endretAvEnhetsnummer,
                patchOppgaveRequest = FerdigstillOppgaveRequest(it),
            )
        }
    }

    internal fun opprettEllerEndreBehandleDokumentOppgaver(
        journalpostHendelse: JournalpostHendelse,
        behandlingsOppgaver: OppgaverForHendelse,
    ) {
        val oppgaverSomSkalEndres =
            behandlingsOppgaver.hentBehandleDokumentOppgaverSomSkalOppdateresForNyttDokument(
                journalpostHendelse.journalpostIdUtenPrefix,
            )
        endreForNyttDokument(journalpostHendelse, oppgaverSomSkalEndres)

        val sakerSomKreverNyBehandleDokumentOppgave =
            behandlingsOppgaver.hentSakerSomKreverNyBehandleDokumentOppgave(
                journalpostHendelse.sakstilknytninger ?: emptyList(),
            )
        opprettBehandleDokumentOppgaveForSaker(journalpostHendelse, sakerSomKreverNyBehandleDokumentOppgave)
    }

    fun opprettOppgave(request: DefaultOpprettOppgaveRequest): OppgaveData = oppgaveConsumer.opprettOppgave(request)

    internal fun opprettBehandleDokumentOppgaveForSaker(
        journalpostHendelse: JournalpostHendelse,
        saker: List<String>,
    ) {
        LOGGER.info("Antall behandle dokument oppgaver som skal opprettes: ${saker.size} for saker $saker")
        saker.forEach {
            LOGGER.info("Oppretter behandle dokument oppgave for sak $it og journalpostId ${journalpostHendelse.journalpostId}")
            opprettOppgave(
                OpprettBehandleDokumentOppgaveRequest(
                    saksreferanse = it,
                    _journalpostId = journalpostHendelse.journalpostMedPrefix,
                    aktoerId = journalpostHendelse.aktorId,
                    tittel = journalpostHendelse.tittel!!,
                    dokumentDato = journalpostHendelse.dokumentDato,
                    sporingsdata = journalpostHendelse.sporing!!,
                    saksbehandlersInfo = journalpostHendelse.hentSaksbehandlerInfo(),
                ),
            )
        }
    }

    internal fun endreForNyttDokument(
        journalpostHendelse: JournalpostHendelse,
        oppgaver: List<OppgaveData>,
    ) {
        LOGGER.info("Antall behandle dokument oppgaver som skal oppdateres: {}", oppgaver.size)

        for (oppgaveData in oppgaver) {
            val request = EndreForNyttDokumentRequest(oppgaveData, journalpostHendelse)
            oppgaveConsumer.endreOppgave(request)
            LOGGER.info("Endret beskrivelse for oppgave {}", oppgaveData.id)
            SECURE_LOGGER.info(
                "Endret beskrivelse for oppgave {} med beskrivelse: {}",
                oppgaveData.id,
                request.beskrivelse,
            )
        }
    }

    internal fun opprettJournalforingOppgave(opprettJournalforingsOppgaveRequest: OpprettJournalforingsOppgaveRequest) {
        oppgaveConsumer.opprettOppgave(opprettJournalforingsOppgaveRequest)
    }
}
