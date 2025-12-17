package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.arbeidsflyt.dto.GjenopprettSøknadsoppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveType
import no.nav.bidrag.arbeidsflyt.dto.normDatoFormatter
import no.nav.bidrag.arbeidsflyt.persistence.entity.Behandling
import no.nav.bidrag.domene.enums.behandling.tilStønadstype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadResponse
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelse
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelseBarn
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelseType
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import no.nav.bidrag.transport.dokument.Sporingsdata
import java.time.LocalDate
import java.time.LocalDateTime

val BehandlingStatusType.erAvsluttet get() =
    listOf(
        BehandlingStatusType.AVBRUTT,
        BehandlingStatusType.VEDTAK_FATTET,
    ).contains(this)
val HentSøknad.erAvsluttet get() = behandlingStatusType.erAvsluttet

fun HentSøknadResponse.mapTilBehandling(endretAv: String) =
    Behandling(
        status = søknad.behandlingStatusType,
        mottattDato = søknad.søknadMottattDato,
        enhet = søknad.behandlerenhet ?: "9999",
        hendelse =
            BehandlingHendelse(
                type =
                    when (søknad.behandlingStatusType) {
                        BehandlingStatusType.AVBRUTT, BehandlingStatusType.VEDTAK_FATTET -> BehandlingHendelseType.AVSLUTTET
                        else -> BehandlingHendelseType.ENDRET
                    },
                status = søknad.behandlingStatusType,
                vedtakstype = Vedtakstype.ENDRING,
                opprettetTidspunkt = LocalDateTime.now(),
                endretTidspunkt = LocalDateTime.now(),
                mottattDato = søknad.søknadMottattDato,
                behandlerEnhet = søknad.behandlerenhet ?: "9999",
                søknadsid = søknad.søknadsid,
                behandlingsid = søknad.behandlingsid,
                barn =
                    søknad.partISøknadListe.map { b ->
                        BehandlingHendelseBarn(
                            saksnummer = søknad.saksnummer,
                            stønadstype = søknad.behandlingstema.tilStønadstype(),
                            søktFraDato = søknad.søknadFomDato,
                            ident = b.personident!!,
                            søktAv = SøktAvType.BIDRAGSPLIKTIG,
                            behandlerEnhet = søknad.behandlerenhet ?: "9999",
                            status = b.behandlingstatus!!,
                            behandlingstema = søknad.behandlingstema,
                        )
                    },
                sporingsdata =
                    Sporingsdata(brukerident = endretAv),
            ),
    )

fun OppgaveData.mapTilOpprettOppgaveDetaljert() =
    GjenopprettSøknadsoppgaveRequest(
        personident = personIdent,
        saksreferanse = saksreferanse!!,
        beskrivelse = beskrivelse!!,
        frist = fristFerdigstillelse!!,
        tildeltEnhetsnr = tildeltEnhetsnr,
        behandlingstype = behandlingstype,
        tema = tema!!,
        ignorerNormDatoHvisIkkeFinnes = true,
        oppgavetype = OppgaveType.valueOf(oppgavetype!!),
        søknadsid = søknadsid?.toLong(),
        behandlingsid = behandlingsid?.toLong(),
        opprettetAvEnhetsnr = opprettetAvEnhetsnr!!,
        normDato = normDato?.let { LocalDate.from(normDatoFormatter.parse(it)) },
        tilordnetRessurs = tilordnetRessurs,
        behandlesAvApplikasjon = behandlesAvApplikasjon,
        aktivDato = aktivDato,
    )

fun OppgaveData.mapTilOpprettOppgave() =
    GjenopprettSøknadsoppgaveRequest(
        personident = personIdent,
        saksreferanse = saksreferanse!!,
        beskrivelse = beskrivelse!!,
        frist = fristFerdigstillelse!!,
        tildeltEnhetsnr = tildeltEnhetsnr,
        behandlingstype = behandlingstype,
        tema = tema!!,
        oppgavetype = OppgaveType.valueOf(oppgavetype!!),
        søknadsid = søknadsid?.toLong(),
        behandlingsid = behandlingsid?.toLong(),
        opprettetAvEnhetsnr = opprettetAvEnhetsnr!!,
        normDato = normDato?.let { LocalDate.from(normDatoFormatter.parse(it)) },
        tilordnetRessurs = tilordnetRessurs,
        behandlesAvApplikasjon = behandlesAvApplikasjon,
    )
