package no.nav.bidrag.arbeidsflyt.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.UnleashFeatures
import no.nav.bidrag.arbeidsflyt.consumer.BidragSakConsumer
import no.nav.bidrag.arbeidsflyt.dto.OppdaterOppgave
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveType
import no.nav.bidrag.arbeidsflyt.dto.OpprettSøknadsoppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.Fagomrade
import no.nav.bidrag.arbeidsflyt.model.erAvsluttet
import no.nav.bidrag.arbeidsflyt.model.mapTilOpprettOppgave
import no.nav.bidrag.arbeidsflyt.persistence.entity.Behandling
import no.nav.bidrag.arbeidsflyt.persistence.entity.BehandlingBarn
import no.nav.bidrag.arbeidsflyt.persistence.entity.BehandlingOppgave
import no.nav.bidrag.arbeidsflyt.persistence.entity.BehandlingOppgaveDetaljer
import no.nav.bidrag.commons.service.forsendelse.bidragsmottaker
import no.nav.bidrag.commons.service.forsendelse.bidragspliktig
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.behandling.tilBeskrivelse
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.util.visningsnavn
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelse
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelseBarn
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}
val BehandlingStatusType.erAvsluttet get() = listOf(BehandlingStatusType.AVBRUTT, BehandlingStatusType.VEDTAK_FATTET).contains(this)

@Service
class BehandleBehandlingHendelseService(
    var oppgaveService: OppgaveService,
    var sakConsumer: BidragSakConsumer,
    val persistenceService: PersistenceService,
    val behandlingService: BehandlingService,
) {
    @Transactional
    fun gjennopprettOppgaveHvisBehandlingIkkeFinnes(
        oppgaveData: OppgaveData,
        søknad: HentSøknad,
    ) {
        if (søknad.erAvsluttet) return

        val åpneOppgaver =
            oppgaveService
                .finnOppgaverForSøknad(
                    søknadId = oppgaveData.søknadsid?.toLong(),
                    behandlingId = oppgaveData.behandlingsid?.toLong(),
                    saksnr = oppgaveData.saksreferanse!!,
                    tema = oppgaveData.tema!!,
                    oppgaveType = OppgaveType.valueOf(oppgaveData.oppgavetype!!),
                ).dataForHendelse

        secureLogger.info { "Fant $åpneOppgaver for opppgave ${oppgaveData.id} og søknad ${oppgaveData.søknadsid}" }
        if (åpneOppgaver.isEmpty()) {
            LOGGER.info { "Gjennoppretter oppgave for sak ${oppgaveData.saksreferanse} og søknadsid ${oppgaveData.søknadsid} og behandlingsid ${oppgaveData.behandlingsid}" }
            oppgaveService.opprettOppgave(oppgaveData.mapTilOpprettOppgave())
        }
    }

    @Transactional
    fun behandleHendelse(hendelse: BehandlingHendelse) {
        val behandling = hentHendelse(hendelse)
        if (behandling.id != 0L && behandling.status.erAvsluttet) {
            secureLogger.info { "Behandling med id ${behandling.id} og behandlingsid ${behandling.behandlingsid} er allerede avsluttet med status ${behandling.status}. Ignorerer hendelse $hendelse" }
            return
        }
        if (!UnleashFeatures.BEHANDLE_BEHANDLING_HENDELSE.isEnabled) {
            secureLogger.info { "Behandling av hendelse er skrudd av. Lagrer behandling uten å opprette eller slette oppgaver" }
        }
        hendelse.barn.groupBy { Pair(it.saksnummer, it.søknadsid) }.forEach { (saksnummerSøknadPair, barnliste) ->
            val førsteBarn = barnliste.find { !it.status.lukketStatus } ?: barnliste.first()
            val kreverOppgave = barnliste.any { it.status.kreverOppgave }
            val saksnummer = saksnummerSøknadPair.first
            val søknadsid = saksnummerSøknadPair.second
            slettÅpneOppgaverUtenSøknadsreferanse(saksnummer)
            val åpneOppgaver =
                oppgaveService
                    .finnOppgaverForSøknad(
                        søknadId = søknadsid,
                        behandlingId = hendelse.behandlingsid,
                        saksnr = saksnummer,
                        tema = finnFagområdeForSøknad(førsteBarn.stønadstype),
                        oppgaveType = finnOppgavetypeForStønadstype(førsteBarn.behandlingstema),
                    ).dataForHendelse
            secureLogger.info { "Fant ${åpneOppgaver.size} åpne søknadsoppgaver for sak $saksnummer og søknadsid $søknadsid og behandlingsid = ${hendelse.behandlingsid}" }
            oppdaterNormDatoOgMottattdato(hendelse, behandling)
            if (kreverOppgave && åpneOppgaver.isEmpty() && UnleashFeatures.BEHANDLE_BEHANDLING_HENDELSE.isEnabled) {
                opprettOppgave(behandling, førsteBarn, hendelse)
            } else if (!kreverOppgave && UnleashFeatures.BEHANDLE_BEHANDLING_HENDELSE.isEnabled) {
                ferdigstillOppgaver(åpneOppgaver)
            } else {
                oppdaterOppgaveDetaljer(behandling, åpneOppgaver)
            }
        }
        oppdaterOgLagreBehandling(hendelse, behandling)
        persistenceService.slettFeiledeMeldingerMedSøknadId(hendelse.søknadsid ?: hendelse.behandlingsid!!)
    }

    private fun oppdaterOppgaveDetaljer(
        behandling: Behandling,
        åpneOppgaver: List<OppgaveData>,
    ) {
        val åpneOppgaveIder = åpneOppgaver.map { it.id }
        val oppgaveDetaljer = behandling.oppgave ?: BehandlingOppgave(oppgaver = setOf())
        val eksisterendeOppgaver = oppgaveDetaljer.oppgaver.filter { !åpneOppgaveIder.contains(it.oppgaveId) }
        val oppdaterteOppgaver =
            åpneOppgaver
                .map { oppgave ->
                    BehandlingOppgaveDetaljer(
                        saksnummer = oppgave.saksreferanse ?: "UKJENT",
                        oppgaveId = oppgave.id,
                        enhet = oppgave.tildeltEnhetsnr ?: "UKJENT",
                        søknadsid = oppgave.søknadsid?.toLong(),
                    )
                }.toSet()
        behandling.oppgave = oppgaveDetaljer.copy(oppgaver = (eksisterendeOppgaver + oppdaterteOppgaver).toSet())
    }

    private fun opprettOppgave(
        behandling: Behandling,
        barn: BehandlingHendelseBarn,
        hendelse: BehandlingHendelse,
    ): OppgaveData {
        val oppgave =
            oppgaveService.opprettOppgave(
                OpprettSøknadsoppgaveRequest(
                    personident = finnOppgaverGjelderPerson(barn),
                    saksreferanse = barn.saksnummer,
                    innhold = opprettOppgaveBeskrivelse(barn),
                    frist = finnFristForSøknadsgruppe(behandling, barn),
                    tildeltEnhetsnr = barn.behandlerEnhet,
                    tema = finnFagområdeForSøknad(barn.stønadstype),
                    oppgavetype = finnOppgavetypeForStønadstype(barn.behandlingstema),
                    søknadsid = barn.søknadsid,
                    behandlingsid = hendelse.behandlingsid,
                    sporingsdata = hendelse.sporingsdata,
                ),
            )
        val oppgaveDetaljer = behandling.oppgave ?: BehandlingOppgave(oppgaver = setOf())
        behandling.oppgave =
            oppgaveDetaljer.copy(
                oppgaver =
                    (
                        oppgaveDetaljer.oppgaver +
                            BehandlingOppgaveDetaljer(
                                saksnummer = barn.saksnummer,
                                oppgaveId = oppgave.id,
                                enhet = barn.behandlerEnhet,
                                søknadsid = barn.søknadsid,
                            )
                    ).toSet(),
            )
        secureLogger.info { "Opprettet oppgave ${oppgave.id} for sak ${barn.saksnummer} og søknadsid ${hendelse.søknadsid} og behandlingsid ${hendelse.behandlingsid}" }
        return oppgave
    }

    private fun ferdigstillOppgaver(åpneOppgaver: List<OppgaveData>) {
        åpneOppgaver.forEach { ferdigstillOppgave ->
            try {
                oppgaveService.oppdaterOppgave(
                    OppdaterOppgave(ferdigstillOppgave)
                        .ferdigstill(),
                )
                secureLogger.info { "Ferdigstilte søknadsoppgave ${ferdigstillOppgave.id} for sak ${ferdigstillOppgave.saksreferanse} og søknadsid ${ferdigstillOppgave.søknadsid} og behandlingsid ${ferdigstillOppgave.behandlingsid}" }
            } catch (e: Exception) {
                LOGGER.error(e) { "Det skjedde en feil ved ferdigstillelse av søknadsoppgave ${ferdigstillOppgave.id}" }
            }
        }
    }

    private fun oppdaterNormDatoOgMottattdato(
        hendelse: BehandlingHendelse,
        behandling: Behandling,
    ) {
        if (behandling.status == BehandlingStatusType.ÅPEN && hendelse.status == BehandlingStatusType.UNDER_BEHANDLING) {
            secureLogger.info { "Oppdaterer norm dato på hendelse for søknad ${hendelse.søknadsid} og behandling ${hendelse.behandlingsid} fordi status gikk fra å være åpen til under behandling" }
            behandling.normDato = LocalDate.now()
        }
        behandling.mottattDato = hendelse.mottattDato
    }

    private fun hentHendelse(hendelse: BehandlingHendelse): Behandling =
        behandlingService.finnForBehandlingsidEllerSøknadsid(hendelse.behandlingsid, hendelse.søknadsid!!)?.let {
            secureLogger.info { "Behandler hendelse $hendelse. Fant eksisterende behandling i databasen med id $it.id" }
            it
        }
            ?: run {
                secureLogger.info { "Behandler hendelse $hendelse. Fant ikke behandling i databsen. Oppretter ny behandling" }
                Behandling(
                    barn = BehandlingBarn(barn = hendelse.barn),
                    hendelse = hendelse,
                    opprettetTidspunkt = hendelse.opprettetTidspunkt,
                    søknadsid = hendelse.søknadsid,
                    behandlingsid = hendelse.behandlingsid,
                    status = hendelse.status,
                    mottattDato = hendelse.mottattDato,
                    enhet = hendelse.behandlerEnhet,
                    behandlesAvFlereSøknader =
                        hendelse.barn
                            .map { it.søknadsid }
                            .distinct()
                            .size > 1,
                )
            }

    private fun oppdaterOgLagreBehandling(
        hendelse: BehandlingHendelse,
        behandling: Behandling,
    ): Behandling {
        val behandlesAvFlereSøknader =
            hendelse.barn
                .map { it.søknadsid }
                .distinct()
                .size > 1
        behandling.status = hendelse.status
        behandling.endretTidspunkt = hendelse.endretTidspunkt
        behandling.behandlesAvFlereSøknader = behandlesAvFlereSøknader
        behandling.hendelse = hendelse
        behandling.barn =
            behandling.barn?.copy(
                barn = hendelse.barn,
            ) ?: BehandlingBarn(barn = hendelse.barn)

        return behandlingService.lagre(behandling)
    }

    fun slettÅpneOppgaverUtenSøknadsreferanse(saksnr: String) {
        val åpneOppgaver =
            oppgaveService
                .finnOppgaverForSøknad(
                    saksnr = saksnr,
                    tema = Fagomrade.BIDRAG,
                    oppgaveType = OppgaveType.BEH_SAK,
                ).dataForHendelse

        åpneOppgaver.forEach {
            if (it.søknadsid == null) {
                secureLogger.info { "Sletter søknadsoppgave ${it.id}" }
                try {
                    oppgaveService.oppdaterOppgave(
                        OppdaterOppgave(it)
                            .ferdigstill(),
                    )
                } catch (e: Exception) {
                    LOGGER.error(e) { "Det skjedde en feil ved ferdigstillelse av søknadsoppgave ${it.id}" }
                }
            }
        }
    }

    fun finnOppgaverGjelderPerson(
        barn: BehandlingHendelseBarn,
    ): String? {
        val sak = sakConsumer.hentSak(barn.saksnummer)
        if (listOf(Behandlingstema.SAKSOMKOSTNINGER, Behandlingstema.ERSTATNING).contains(barn.behandlingstema) && barn.søktAv == SøktAvType.BIDRAGSPLIKTIG) {
            return sak.bidragspliktig?.fødselsnummer?.verdi
        }

        return sak.bidragsmottaker?.fødselsnummer?.verdi ?: barn.ident
    }

    fun opprettOppgaveBeskrivelse(barn: BehandlingHendelseBarn): String {
        val beskrivelseBehandlingstema = barn.behandlingstema.tilBeskrivelse(barn.medInnkreving)
        val behandlingstemaMedSærbidragKategori =
            if (barn.særbidragskategori != null) {
                "$beskrivelseBehandlingstema, ${barn.særbidragskategori!!.visningsnavn.intern}"
            } else {
                beskrivelseBehandlingstema
            }
        return "${barn.behandlingstype.bisysDekode} - $behandlingstemaMedSærbidragKategori"
    }

    fun finnFristForSøknadsgruppe(
        behandling: Behandling,
        barn: BehandlingHendelseBarn,
    ): LocalDate {
        val behandlingstype = barn.behandlingstype
        val fristFraDato = behandling.normDato ?: behandling.mottattDato
        if (behandlingstype.erKlage) {
            return fristFraDato.plusDays(180)
        }

        if (barn.stønadstype == Stønadstype.FORSKUDD) {
            return if (listOf(Behandlingstype.SØKNAD, Behandlingstype.ENDRING).contains(barn.behandlingstype)) {
                fristFraDato.plusDays(23)
            } else {
                fristFraDato.plusDays(30)
            }
        }
        if (barn.stønadstype == Stønadstype.BIDRAG || barn.stønadstype == Stønadstype.BIDRAG18AAR) {
            return if (Behandlingstype.SØKNAD == barn.behandlingstype) {
                fristFraDato.plusDays(85)
            } else {
                fristFraDato.plusDays(100)
            }
        }
        return fristFraDato.plusDays(90)
    }

    fun finnOppgavetypeForStønadstype(søktOm: Behandlingstema?): OppgaveType =
        when (søktOm) {
            Behandlingstema.MOTREGNING -> OppgaveType.IN
            Behandlingstema.AVSKRIVNING -> OppgaveType.GEN
            else -> OppgaveType.BEH_SAK
        }

    fun finnFagområdeForSøknad(stønadstype: Stønadstype?): String =
        when (stønadstype) {
            Stønadstype.MOTREGNING -> Fagomrade.BIDRAGINNKREVING
            else -> Fagomrade.BIDRAG
        }
}
