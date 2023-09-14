package no.nav.bidrag.arbeidsflyt.model

import mu.KotlinLogging
import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.consumer.BidragTIlgangskontrollConsumer
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.service.OppgaveService
import no.nav.bidrag.arbeidsflyt.service.OrganisasjonService
import no.nav.bidrag.arbeidsflyt.service.PersistenceService
import no.nav.bidrag.dokument.dto.JournalpostHendelse
private val LOGGER = KotlinLogging.logger {}

class BehandleJournalpostHendelse(
    private val journalpostHendelse: JournalpostHendelse,
    private val oppgaveService: OppgaveService,
    private val arbeidsfordelingService: OrganisasjonService,
    private val persistenceService: PersistenceService,
    private val tIlgangskontrollConsumer: BidragTIlgangskontrollConsumer
) {
    private var finnOppdaterteOppgaverForHendelse = true
    private lateinit var oppgaverForHendelse: OppgaverForHendelse

    init {
        finnOppgaverForHendelse()
    }
    fun oppdaterEksterntFagomrade(): BehandleJournalpostHendelse {
        if (journalpostHendelse.erEksterntFagomrade) {
            LOGGER.info("Endring til eksternt fagområde av ${journalpostHendelse.hentSaksbehandlerInfo()}.")

            oppgaveService.ferdigstillJournalforingsOppgaver(
                endretAvEnhetsnummer = journalpostHendelse.hentEndretAvEnhetsnummer(),
                oppgaverForHendelse = oppgaverForHendelse
            )

            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    fun oppdaterOverførMellomBidragFagomrader(): BehandleJournalpostHendelse {
        if (journalpostHendelse.erEksterntFagomrade || !journalpostHendelse.erMottattStatus) return this

        val fagområdeNy = journalpostHendelse.hentTema() ?: return this
        val journalpost = persistenceService.hentJournalpostMedStatusMottatt(journalpostHendelse.journalpostId)
        val fagområdeGammelt = journalpost?.tema
        val saksbehandlerIdent = journalpostHendelse.sporing?.brukerident

        val harTilgangTilTema = saksbehandlerIdent?.let { tIlgangskontrollConsumer.sjekkTilgangTema(fagområdeNy, saksbehandlerIdent) } ?: true
        SECURE_LOGGER.info("Sjekket tilgang til tema $fagområdeNy for saksbehandlerIdent $saksbehandlerIdent. Saksbehandler har tilgang = $harTilgangTilTema")
        val erEndringAvFagomrade = journalpost != null && journalpost.tema != fagområdeNy

        if (erEndringAvFagomrade || !harTilgangTilTema && oppgaverForHendelse.erJournalforingsoppgaverTildeltSaksbehandler()) {
            LOGGER.info("Endring fra fagområde ${fagområdeGammelt ?: "UKJENT"} til $fagområdeNy av ${journalpostHendelse.hentSaksbehandlerInfo()}. Saksbehandler har tilgang til ny tema = $harTilgangTilTema")
            oppgaveService.endreMellomBidragFagomrade(
                oppgaverForHendelse = oppgaverForHendelse,
                journalpostHendelse,
                fagområdeGammelt,
                fagområdeNy,
                harTilgangTilTema
            )

            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    fun oppdaterEndretEnhetsnummer(): BehandleJournalpostHendelse {
        finnOppgaverForHendelse()

        if (oppgaverForHendelse.erEndringAvTildeltEnhetsnummer(journalpostHendelse)) {
            LOGGER.info("Endret tilordnet ressurs utført av ${journalpostHendelse.hentSaksbehandlerInfo()}.")
            oppgaveService.overforOppgaver(oppgaverForHendelse, journalpostHendelse)
            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    fun oppdaterOppgaveMedAktoerId(): BehandleJournalpostHendelse {
        finnOppgaverForHendelse()

        if (oppgaverForHendelse.erEndringAvAktoerId(journalpostHendelse)) {
            LOGGER.info("Oppdaterer aktørid for oppgave. Rapportert av ${journalpostHendelse.hentSaksbehandlerInfo()}.")
            oppgaveService.oppdaterOppgaver(oppgaverForHendelse, journalpostHendelse)
            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    fun opprettJournalforingsoppgave(): BehandleJournalpostHendelse {
        finnOppgaverForHendelse()

        if (!journalpostHendelse.erEksterntFagomrade && journalpostHendelse.erMottattStatus && oppgaverForHendelse.harIkkeJournalforingsoppgave()) {
            LOGGER.info("En mottaksregistert journalpost uten journalføringsoppgave. Rapportert av ${journalpostHendelse.hentSaksbehandlerInfo()}.")

            val tildeltEnhetsnr = hentArbeidsfordeling()
            oppgaveService.opprettJournalforingOppgave(OpprettJournalforingsOppgaveRequest(journalpostHendelse, tildeltEnhetsnr))
            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    fun opprettEllerEndreBehandleDokumentOppgaver(): BehandleJournalpostHendelse {
        if (journalpostHendelse.erJournalfort && journalpostHendelse.erHendelseTypeJournalforing() && journalpostHendelse.harSaker) {
            val behandlingsOppgaver: OppgaverForHendelse = oppgaveService.finnBehandlingsoppgaverForSaker(journalpostHendelse.saker)
            if (behandlingsOppgaver.skalOppdatereEllerOppretteBehandleDokumentOppgaver(journalpostHendelse.journalpostId, journalpostHendelse.saker)) {
                LOGGER.info("En journalført journalpost skal ha oppdatert behandle dokument oppgaver for saker. Rapportert av ${journalpostHendelse.hentSaksbehandlerInfo()}.")
                validerGyldigDataForBehandleDokument()
                oppgaveService.opprettEllerEndreBehandleDokumentOppgaver(journalpostHendelse, behandlingsOppgaver)
                finnOppdaterteOppgaverForHendelse = true
            }
        }

        return this
    }

    fun hentArbeidsfordeling(): String {
        val tema = journalpostHendelse.hentTema()
        if (tema == Fagomrade.FARSKAP) {
            LOGGER.info("Journalposthendelse med journalpostId ${journalpostHendelse.journalpostId} har tema FAR. Bruker enhet $ENHET_FARSKAP ved arbeidsfordeling")
            return ENHET_FARSKAP
        }
        if (journalpostHendelse.hasEnhet) {
            val enhetEksitererOgErAktiv = arbeidsfordelingService.enhetEksistererOgErAktiv(journalpostHendelse.enhet)
            val erJournalførendeEnhet = arbeidsfordelingService.erJournalførendeEnhet(journalpostHendelse.enhet)
            val erGyldigEnhet = enhetEksitererOgErAktiv && erJournalførendeEnhet
            return if (!erGyldigEnhet) {
                LOGGER.warn("Enhet ${journalpostHendelse.enhet} fra hendelse er ikke journalførende enhet, eksisterer ikke eller er nedlagt. Henter enhet fra personens arbeidsfordeling.")
                arbeidsfordelingService.hentArbeidsfordeling(journalpostHendelse.aktorId, journalpostHendelse.behandlingstema).verdi
            } else {
                LOGGER.info("Bruker enhet ${journalpostHendelse.enhet} fra hendelsen ved arbeidsfordelingen.")
                journalpostHendelse.enhet!!
            }
        }
        return arbeidsfordelingService.hentArbeidsfordeling(journalpostHendelse.aktorId, journalpostHendelse.behandlingstema).verdi
    }
    private fun validerGyldigDataForBehandleDokument() {
        if (!journalpostHendelse.harTittel) throw ManglerDataForBehandleDokument("Kan ikke opprette/oppdatere behandle dokument oppgave fordi hendelse mangler tittel")
        if (!journalpostHendelse.harDokumentDato) throw ManglerDataForBehandleDokument("Kan ikke opprette/oppdatere behandle dokument oppgave fordi hendelse mangler dokument dato")
        if (!journalpostHendelse.harSporingsdataEnhet) throw ManglerDataForBehandleDokument("Kan ikke opprette/oppdatere behandle dokument oppgave fordi hendelse mangler enhetsnummer")
    }

    fun ferdigstillJournalforingsoppgaver(): BehandleJournalpostHendelse {
        finnOppgaverForHendelse()

        if (journalpostHendelse.erJournalstatusEndretTilIkkeMottatt() && oppgaverForHendelse.harJournalforingsoppgaver()) {
            LOGGER.info("En journalført journalpost skal ikke ha journalføringsoppgaver. Rapportert av ${journalpostHendelse.hentSaksbehandlerInfo()}.")
            oppgaveService.ferdigstillJournalforingsOppgaver(
                endretAvEnhetsnummer = journalpostHendelse.hentEndretAvEnhetsnummer(),
                oppgaverForHendelse = oppgaverForHendelse
            )
            finnOppdaterteOppgaverForHendelse = true
        }

        return this
    }

    private fun finnOppgaverForHendelse() {
        if (finnOppdaterteOppgaverForHendelse) {
            oppgaverForHendelse = oppgaveService.finnAapneJournalforingOppgaverForJournalpost(journalpostHendelse.journalpostId)
            finnOppdaterteOppgaverForHendelse = false
        }
    }
}
