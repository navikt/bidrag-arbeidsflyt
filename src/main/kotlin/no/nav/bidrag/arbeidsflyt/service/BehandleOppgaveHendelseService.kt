package no.nav.bidrag.arbeidsflyt.service

import io.micrometer.core.instrument.MeterRegistry
import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.hendelse.dto.OppgaveKafkaHendelse
import no.nav.bidrag.arbeidsflyt.model.OppdaterOppgaveFraHendelse
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandleOppgaveHendelseService(
    var persistenceService: PersistenceService,
    var oppgaveService: OppgaveService,
    var journalpostService: JournalpostService,
    var applicationContext: ApplicationContext,
    var arbeidsfordelingService: OrganisasjonService,
    private val meterRegistry: MeterRegistry,
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BehandleOppgaveHendelseService::class.java)
    }

    @Transactional
    fun behandleOppgaveHendelse(oppgaveHendelse: OppgaveKafkaHendelse) {
        LOGGER.info(
            """
            Mottatt oppgave ${oppgaveHendelse.hendelse.hendelsestype} med 
            oppgaveId ${oppgaveHendelse.oppgave.oppgaveId}, 
            versjon ${oppgaveHendelse.oppgave.versjon},
            opgpavetype ${oppgaveHendelse.oppgave.kategorisering?.oppgavetype},
            tema ${oppgaveHendelse.oppgave.kategorisering?.tema},
            utførtAv ${oppgaveHendelse.utfortAv?.navIdent} (enhet ${oppgaveHendelse.utfortAv?.enhetsnr}),       
           """.replaceIndent(" ").replace("\n", "")
        )
        SECURE_LOGGER.info("""
            Mottatt oppgave ${oppgaveHendelse.hendelse.hendelsestype} med 
            oppgaveId ${oppgaveHendelse.oppgave.oppgaveId}, 
            versjon ${oppgaveHendelse.oppgave.versjon},
            opgpavetype ${oppgaveHendelse.oppgave.kategorisering?.oppgavetype},
            hendelse $oppgaveHendelse
            """.replaceIndent(" ").replace("\n", ""))

        if (oppgaveHendelse.erOppgaveOpprettetHendelse) {
            val oppgave = oppgaveService.hentOppgave(oppgaveHendelse.oppgaveId)
            behandleOpprettOppgave(oppgave)
            measureOppgaveOpprettetHendelse(oppgave)
        } else if (oppgaveHendelse.erOppgaveEndretHendelse) {
            val oppgave = oppgaveService.hentOppgave(oppgaveHendelse.oppgaveId)
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
            LOGGER.debug("Oppgave ${oppgave.id} har ingen journalpostid. Stopper videre behandling.")
        }

        persistenceService.oppdaterEllerSlettOppgaveMetadataFraHendelse(oppgave)
    }

    fun oppdaterOppgaveFraHendelse(oppgave: OppgaveData): OppdaterOppgaveFraHendelse {
        return applicationContext.getBean(OppdaterOppgaveFraHendelse::class.java)
            .behandle(oppgave)
    }

    fun opprettNyJournalforingOppgaveHvisNodvendig(oppgave: OppgaveData) {
        if (!oppgave.tilhorerFagpost && (oppgave.erAvsluttetJournalforingsoppgave() || erOppgavetypeEndretFraJournalforingTilAnnet(
                oppgave
            ))
        ) {
            opprettNyJournalforingOppgaveHvisJournalpostMottatt(oppgave)
        }
    }

    fun opprettNyJournalforingOppgaveHvisJournalpostMottatt(oppgave: OppgaveData) {
        if (harIkkeAapneJournalforingsoppgaver(oppgave.journalpostId!!)) {
            journalpostService.hentJournalpostMedStatusMottatt(oppgave.journalpostIdMedPrefix!!)
                .takeIf { it?.erBidragFagomrade == true }
                ?.run {
                    LOGGER.info("Journalpost ${oppgave.journalpostId} har status MOTTATT men har ingen journalføringsoppgave. Oppretter ny journalføringsoppgave")
                    opprettJournalforingOppgaveFraHendelse(oppgave)
                }
                ?: run { LOGGER.info("Journalpost ${oppgave.journalpostId} som tilhører oppgave ${oppgave.id} har ikke status MOTTATT. Stopper videre behandling.") }
        }
    }

    fun harIkkeAapneJournalforingsoppgaver(journalpostId: String): Boolean {
        val aapneOppgaveAPI =
            oppgaveService.finnAapneJournalforingOppgaverForJournalpost(journalpostId)
        return aapneOppgaveAPI.harIkkeJournalforingsoppgave()
    }

    fun opprettJournalforingOppgaveFraHendelse(oppgave: OppgaveData) {
        val tildeltEnhetsnr = oppgave.tildeltEnhetsnr
            ?: arbeidsfordelingService.hentArbeidsfordeling(oppgave.hentIdent).verdi
        oppgaveService.opprettJournalforingOppgave(
            OpprettJournalforingsOppgaveRequest(
                oppgave,
                tildeltEnhetsnr
            )
        )
    }

    fun erOppgavetypeEndretFraJournalforingTilAnnet(oppgave: OppgaveData): Boolean {
        if (oppgave.erJournalforingOppgave) {
            return false
        }

        val prevOppgaveState = persistenceService.hentJournalforingOppgave(oppgave.id)
        if (prevOppgaveState?.erJournalforingOppgave() == true) {
            LOGGER.info("Oppgavetype for oppgave ${oppgave.id} er endret fra JFR til ${oppgave.oppgavetype}")
            return true
        }

        return false
    }

    fun measureOppgaveOpprettetHendelse(oppgaveOpprettetHendelse: OppgaveData) {
        if (oppgaveOpprettetHendelse.erTemaBIDEllerFAR() && oppgaveOpprettetHendelse.erJournalforingOppgave) {
            meterRegistry.counter(
                "jfr_oppgave_opprettet",
                "tema", oppgaveOpprettetHendelse.tema ?: "UKJENT",
                "enhet", oppgaveOpprettetHendelse.tildeltEnhetsnr ?: "UKJENT",
                "opprettetAv", oppgaveOpprettetHendelse.opprettetAv ?: "UKJENT",
                "opprettetAvEnhetsnr", oppgaveOpprettetHendelse.opprettetAvEnhetsnr ?: "UKJENT"
            ).increment()
        }
    }
}
