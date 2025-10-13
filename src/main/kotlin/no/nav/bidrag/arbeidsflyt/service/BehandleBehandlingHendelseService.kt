package no.nav.bidrag.arbeidsflyt.service

import mu.KotlinLogging
import no.nav.bidrag.arbeidsflyt.consumer.BidragSakConsumer
import no.nav.bidrag.arbeidsflyt.dto.OppdaterOppgave
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveType
import no.nav.bidrag.arbeidsflyt.dto.OpprettSøknadsoppgaveRequest
import no.nav.bidrag.arbeidsflyt.hendelse.dto.BehandlingHendelse
import no.nav.bidrag.arbeidsflyt.hendelse.dto.BehandlingHendelseBarn
import no.nav.bidrag.arbeidsflyt.hendelse.dto.BehandlingStatusType
import no.nav.bidrag.arbeidsflyt.model.Fagomrade
import no.nav.bidrag.arbeidsflyt.persistence.entity.Behandling
import no.nav.bidrag.arbeidsflyt.persistence.entity.BehandlingBarn
import no.nav.bidrag.arbeidsflyt.persistence.entity.BehandlingOppgave
import no.nav.bidrag.arbeidsflyt.persistence.entity.BehandlingOppgaveDetaljer
import no.nav.bidrag.arbeidsflyt.persistence.repository.BehandlingRepository
import no.nav.bidrag.commons.service.forsendelse.bidragsmottaker
import no.nav.bidrag.commons.service.forsendelse.bidragspliktig
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.behandling.tilBeskrivelseBehandlingstema
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.util.visningsnavn
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

@Service
class BehandleBehandlingHendelseService(
    var oppgaveService: OppgaveService,
    var sakConsumer: BidragSakConsumer,
    var behandlingRepository: BehandlingRepository,
) {
    @Transactional
    fun behandleHendelse(hendelse: BehandlingHendelse) {
        val behandling = hentHendelse(hendelse)
        hendelse.barn.groupBy { it.saksnummer }.forEach { (saksnummer, barnliste) ->
            val førsteBarn = barnliste.find { !it.status.lukketStatus } ?: barnliste.first()
            val kreverOppgave = barnliste.any { it.status.kreverOppgave }
            slettÅpneOppgaverUtenSøknadsreferanse(saksnummer)
            val åpneOppgaver =
                oppgaveService
                    .finnOppgaverForSøknad(
                        søknadId = førsteBarn.søknadsid,
                        behandlingId = hendelse.behandlingsid,
                        saksnr = saksnummer,
                        tema = finnFagområdeForSøknad(førsteBarn.stønadstype),
                        oppgaveType = finnOppgavetypeForStønadstype(førsteBarn.behandlingstema),
                    ).dataForHendelse
            oppdaterNormDatoOgMottattdato(hendelse, behandling)
            if (kreverOppgave && åpneOppgaver.isEmpty()) {
                opprettOppgave(behandling, førsteBarn, hendelse)
            } else if (!kreverOppgave) {
                ferdigstillOppgaver(åpneOppgaver)
            } else {
                oppdaterOppgaveDetaljer(behandling, åpneOppgaver)
            }
        }
        oppdaterOgLagreBehandling(hendelse, behandling)
    }

    private fun oppdaterOppgaveDetaljer(
        behandling: Behandling,
        åpneOppgaver: List<OppgaveData>,
    ) {
        val oppgaveDetaljer = behandling.oppgave ?: BehandlingOppgave(oppgaver = setOf())
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
        behandling.oppgave = oppgaveDetaljer.copy(oppgaver = (oppgaveDetaljer.oppgaver + oppdaterteOppgaver).toSet())
    }

    private fun opprettOppgave(
        behandling: Behandling,
        barn: BehandlingHendelseBarn,
        hendelse: BehandlingHendelse,
    ) {
        val oppgave =
            oppgaveService.opprettOppgave(
                OpprettSøknadsoppgaveRequest(
                    personident = finnOppgaverGjelderPerson(barn),
                    saksreferanse = barn.saksnummer,
                    beskrivelse = opprettOppgaveBeskrivelse(barn),
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
    }

    private fun ferdigstillOppgaver(åpneOppgaver: List<OppgaveData>) {
        åpneOppgaver.forEach { ferdigstillOppgave ->
            try {
                oppgaveService.oppdaterOppgave(
                    OppdaterOppgave(ferdigstillOppgave)
                        .ferdigstill(),
                )
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
            behandling.normDato = LocalDate.now()
        }
        behandling.mottattDato = hendelse.mottattDato
    }

    private fun hentHendelse(hendelse: BehandlingHendelse): Behandling =
        behandlingRepository.finnForBehandlingEllerSøknadId(hendelse.behandlingsid, hendelse.søknadsid)?.let {
            secureLogger.info { "Behandler hendelse $hendelse. Fant eksisterende behandling i databasen med id $it.id" }
            it
        }
            ?: run {
                secureLogger.info { "Behandler hendelse $hendelse. Fant ikke behandling i databsen. Oppretter ny behandling" }
                Behandling(
                    barn = BehandlingBarn(barn = hendelse.barn),
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
        behandling.barn =
            behandling.barn?.copy(
                barn = hendelse.barn,
            ) ?: BehandlingBarn(barn = hendelse.barn)

        return behandlingRepository.save(behandling)
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
                LOGGER.info { "Sletter søknadsoppgave ${it.id}" }
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
        val beskrivelseBehandlingstema = tilBeskrivelseBehandlingstema(barn.stønadstype, barn.engangsbeløptype, barn.behandlingstema)
        val behandlingstemaMedSærbidragKategori =
            if (barn.særbidragskategori != null) {
                "$beskrivelseBehandlingstema, ${barn.særbidragskategori.visningsnavn.intern}"
            } else {
                beskrivelseBehandlingstema
            }
        return "${barn.behandlingstype.visningsnavn.intern} - $behandlingstemaMedSærbidragKategori"
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
