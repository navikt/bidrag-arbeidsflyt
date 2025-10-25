package no.nav.bidrag.arbeidsflyt.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.UnleashFeatures
import no.nav.bidrag.arbeidsflyt.consumer.BidragBBMConsumer
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.hendelse.dto.OppgaveKafkaHendelse
import no.nav.bidrag.arbeidsflyt.model.OppdaterOppgaveFraHendelse
import no.nav.bidrag.arbeidsflyt.model.erAvsluttet
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadRequest
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@Service
class BehandleOppgaveHendelseService(
    var persistenceService: PersistenceService,
    var oppgaveService: OppgaveService,
    var journalpostService: JournalpostService,
    var applicationContext: ApplicationContext,
    var arbeidsfordelingService: OrganisasjonService,
    var behandlingService: BehandlingService,
    var behandlingHendelseService: BehandleBehandlingHendelseService,
    var bbmConsumer: BidragBBMConsumer,
    private val meterRegistry: MeterRegistry,
) {
    @Transactional
    fun behandleOppgaveHendelse(oppgaveHendelse: OppgaveKafkaHendelse) {
        val oppgave = oppgaveService.hentOppgave(oppgaveHendelse.oppgaveId)
        logHendelse(oppgaveHendelse, oppgave)
        if (oppgaveHendelse.erOppgaveOpprettetHendelse) {
            behandleOpprettOppgave(oppgave)
            measureOppgaveOpprettetHendelse(oppgave)
        } else if (oppgaveHendelse.erOppgaveEndretHendelse) {
            behandleEndretOppgave(oppgave)
            persistenceService.slettFeiledeMeldingerMedOppgaveid(oppgaveHendelse.oppgaveId)
        }
    }

    private fun behandleOpprettOppgave(oppgave: OppgaveData) {
        if (oppgave.erJournalforingOppgave) {
            persistenceService.lagreJournalforingsOppgaveFraHendelse(oppgave)
        }

        if (oppgave.hasJournalpostId) {
            oppdaterOppgaveFraHendelse(oppgave)
                .overforOppgaveTilJournalforendeEnhetHvisTilhorerJournalforende()
                .endreVurderDokumentOppgaveTypeTilJournalforingHvisJournalpostMottatt()
                .overforReturoppgaveTilFarskapHvisJournalpostHarTemaFAR()
                .utfor()
        } else {
            oppdaterOppgaveFraHendelse(oppgave)
                .overforOppgaveTilJournalforendeEnhetHvisTilhorerJournalforende()
                .endreVurderDokumentOppgaveTypeTilVurderHenvendelseHvisIngenJournalpost()
                .utfor()
        }
    }

    private fun behandleEndretOppgave(oppgave: OppgaveData) {
        if (oppgave.hasJournalpostId) {
            oppdaterOppgaveFraHendelse(oppgave)
                .overforOppgaveTilJournalforendeEnhetHvisTilhorerJournalforende()
                .endreVurderDokumentOppgaveTypeTilJournalforingHvisJournalpostMottatt()
                .utfor()
            opprettNyJournalforingOppgaveHvisNodvendig(oppgave)
        } else {
            if (UnleashFeatures.BEHANDLE_BEHANDLING_HENDELSE.isEnabled) {
                overførSøknadsoppgaverTilSammeEnhet(oppgave)
                opprettSøknadsoppgaveHvisBehandlingIkkeAvsluttet(oppgave)
            }

            behandlingService.oppdaterStatusPåOppgaverBehandlingTilFerdigstilt(oppgave)
        }

        persistenceService.oppdaterEllerSlettOppgaveMetadataFraHendelse(oppgave)
    }

    fun oppdaterOppgaveFraHendelse(oppgave: OppgaveData): OppdaterOppgaveFraHendelse =
        applicationContext
            .getBean(OppdaterOppgaveFraHendelse::class.java)
            .behandle(oppgave)

    fun opprettNyJournalforingOppgaveHvisNodvendig(oppgave: OppgaveData) {
        if (!oppgave.tilhorerFagpost &&
            (
                oppgave.erAvsluttetJournalforingsoppgave() ||
                    erOppgavetypeEndretFraJournalforingTilAnnet(
                        oppgave,
                    )
            )
        ) {
            opprettNyJournalforingOppgaveHvisJournalpostMottatt(oppgave)
        }
    }

    fun opprettNyJournalforingOppgaveHvisJournalpostMottatt(oppgave: OppgaveData) {
        if (harIkkeAapneJournalforingsoppgaver(oppgave.journalpostId!!)) {
            journalpostService
                .hentJournalpostMedStatusMottatt(oppgave.journalpostIdMedPrefix!!)
                .takeIf { it?.erBidragFagomrade == true }
                ?.run {
                    LOGGER.info { "Journalpost ${oppgave.journalpostId} har status MOTTATT men har ingen journalføringsoppgave. Oppretter ny journalføringsoppgave" }
                    opprettJournalforingOppgaveFraHendelse(oppgave)
                }
                ?: run {
                    LOGGER.info { "Journalpost ${oppgave.journalpostId} som tilhører oppgave ${oppgave.id} har ikke status MOTTATT. Stopper videre behandling." }
                }
        }
    }

    fun harIkkeAapneJournalforingsoppgaver(journalpostId: String): Boolean {
        val aapneOppgaveAPI =
            oppgaveService.finnAapneJournalforingOppgaverForJournalpost(journalpostId)
        return aapneOppgaveAPI.harIkkeJournalforingsoppgave()
    }

    fun opprettJournalforingOppgaveFraHendelse(oppgave: OppgaveData) {
        val tildeltEnhetsnr =
            oppgave.tildeltEnhetsnr
                ?: arbeidsfordelingService.hentArbeidsfordeling(oppgave.hentIdent).verdi
        oppgaveService.opprettJournalforingOppgave(
            OpprettJournalforingsOppgaveRequest(
                oppgave,
                tildeltEnhetsnr,
            ),
        )
    }

    fun opprettSøknadsoppgaveHvisBehandlingIkkeAvsluttet(oppgave: OppgaveData) {
        if (oppgave.endretAvArbeidsflyt()) return

        if (!oppgave.erStatusKategoriAvsluttet) return

        if (oppgave.søknadsid == null && oppgave.behandlingsid == null) return

        val behandling =
            behandlingService.finnBehandling(oppgave)

        if (behandling == null) {
            if (oppgave.søknadsid == null) {
                // TODO: Er det behov for å hente behandling?
                LOGGER.info { "Oppgave ${oppgave.id} har ingen søknadsid men har behandlingsid ${oppgave.behandlingsid}. Ignorer videre behandling" }
                return
            }
            try {
                val søknad = bbmConsumer.hentSøknad(HentSøknadRequest(oppgave.søknadsid!!))
                if (!søknad.søknad.erAvsluttet) {
                    LOGGER.info { "Fant ikke behandling som tilhører oppgave ${oppgave.id} med søknadsid ${oppgave.søknadsid}. Hentet søknad og gjenoppretter oppgave istedenfor" }
                    behandlingHendelseService.gjennopprettOppgaveHvisBehandlingIkkeFinnes(oppgave, søknad.søknad)
                }
            } catch (e: Exception) {
                LOGGER.error(e) { "Feil ved henting av søknad med søknadsid ${oppgave.søknadsid} for oppgave ${oppgave.id}" }
            }
        } else if (!behandling.erAvsluttet && behandling.hendelse != null) {
            LOGGER.info { "Oppgave ${oppgave.id} ble ferdigstilt men tilhørende behandling med søknadsid ${behandling.søknadsid} er ikke lukket. Opprett søknad på nytt" }
            behandlingHendelseService.behandleHendelse(behandling.hendelse!!)
        }
    }

    fun overførSøknadsoppgaverTilSammeEnhet(oppgave: OppgaveData) {
        if (oppgave.endretAvArbeidsflyt()) return
        if (!erSøknadsoppgaveEnhetEndretTilNoeAnnet(oppgave)) return

        oppgaveService.oppdaterAlleOppgaverSomTilhørerSammeBehandling(oppgave)
        behandlingService.oppdaterBehandlingEnhet(oppgave)
    }

    fun erSøknadsoppgaveEnhetEndretTilNoeAnnet(oppgave: OppgaveData): Boolean {
        if (!oppgave.erSøknadsoppgave) {
            return false
        }

        val prevOppgaveState = persistenceService.hentOppgave(oppgave.id) ?: return false

        return prevOppgaveState.tildeltEnhetsnr != null &&
            oppgave.tildeltEnhetsnr != null &&
            prevOppgaveState.tildeltEnhetsnr != oppgave.tildeltEnhetsnr
    }

    fun erOppgavetypeEndretFraJournalforingTilAnnet(oppgave: OppgaveData): Boolean {
        if (oppgave.erJournalforingOppgave) {
            return false
        }

        val prevOppgaveState = persistenceService.hentJournalforingOppgave(oppgave.id)
        if (prevOppgaveState?.erJournalforingOppgave() == true) {
            LOGGER.info { "Oppgavetype for oppgave ${oppgave.id} er endret fra JFR til ${oppgave.oppgavetype}" }
            return true
        }

        return false
    }

    fun measureOppgaveOpprettetHendelse(oppgaveOpprettetHendelse: OppgaveData) {
        if (oppgaveOpprettetHendelse.erTemaBIDEllerFAR() && oppgaveOpprettetHendelse.erJournalforingOppgave) {
            meterRegistry
                .counter(
                    "jfr_oppgave_opprettet",
                    "tema",
                    oppgaveOpprettetHendelse.tema ?: "UKJENT",
                    "enhet",
                    oppgaveOpprettetHendelse.tildeltEnhetsnr ?: "UKJENT",
                    "opprettetAv",
                    oppgaveOpprettetHendelse.opprettetAv ?: "UKJENT",
                    "opprettetAvEnhetsnr",
                    oppgaveOpprettetHendelse.opprettetAvEnhetsnr ?: "UKJENT",
                ).increment()
        }
    }

    private fun logHendelse(
        oppgaveHendelse: OppgaveKafkaHendelse,
        oppgave: OppgaveData,
    ) {
        try {
            SECURE_LOGGER.info(
                "Mottatt oppgave ${oppgaveHendelse.hendelse.hendelsestype} med " +
                    buildList {
                        add("oppgaveId ${oppgaveHendelse.oppgave.oppgaveId}")
                        add("versjon ${oppgaveHendelse.oppgave.versjon}")
                        add("opgpavetype ${oppgaveHendelse.oppgave.kategorisering?.oppgavetype}")
                        add("tema ${oppgaveHendelse.oppgave.kategorisering?.tema}")
                        add("journalpostId ${oppgave.journalpostId}")
                        add("tildelt ${oppgaveHendelse.oppgave.tilordning?.navIdent} (enhet ${oppgaveHendelse.oppgave.tilordning?.enhetsnr})")
                        add("utførtAv ${oppgaveHendelse.utfortAv?.navIdent} (enhet ${oppgaveHendelse.utfortAv?.enhetsnr})")
                        add("hendelse $oppgaveHendelse")
                    }.joinToString(", "),
            )
        } catch (e: Exception) {
            LOGGER.error(e) { "Det skjedde en feil ved logging av hendelse" }
        }
    }
}
