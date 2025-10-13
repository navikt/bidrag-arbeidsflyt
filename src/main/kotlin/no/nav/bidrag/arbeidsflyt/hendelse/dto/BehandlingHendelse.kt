package no.nav.bidrag.arbeidsflyt.hendelse.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.særbidrag.Særbidragskategori
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.transport.dokument.Sporingsdata
import java.time.LocalDate
import java.time.LocalDateTime

data class BehandlingHendelse(
    val type: BehandlingHendelseType,
    val status: BehandlingStatusType,
    val vedtakstype: Vedtakstype,
    val opprettetTidspunkt: LocalDateTime,
    val endretTidspunkt: LocalDateTime,
    val mottattDato: LocalDate,
    val barn: List<BehandlingHendelseBarn> = emptyList(),
    val sporingsdata: Sporingsdata,
    val behandlingsid: Long? = null,
    val behandlerEnhet: String,
    @Schema(description = "Vil bare settes for søknad som behandles av Bisys")
    val søknadsid: Long? = null,
)

data class BehandlingHendelseBarn(
    val saksnummer: String,
    val behandlingstype: Behandlingstype = Behandlingstype.ENDRING,
    val behandlingstema: Behandlingstema = Behandlingstema.BIDRAG,
    val status: Behandlingstatus = Behandlingstatus.UNDER_BEHANDLING,
    val stønadstype: Stønadstype? = null,
    val engangsbeløptype: Engangsbeløptype? = null,
    val medInnkreving: Boolean = true,
    val søktAv: SøktAvType,
    val søktFraDato: LocalDate,
    val ident: String,
    @Schema(description = "Kan være ulik i samme behandling hvis det er barnebidragsbehandling som er slått ut til forholdsmessig fordeling")
    val søknadsid: Long? = null,
    val behandlerEnhet: String,
    val særbidragskategori: Særbidragskategori? = null,
)

enum class BehandlingStatusType {
    ÅPEN,
    UNDER_BEHANDLING,
    AVBRUTT,
    VEDTAK_FATTET,
}

enum class BehandlingHendelseType {
    OPPRETTET,
    ENDRET,
    AVSLUTTET,
}
