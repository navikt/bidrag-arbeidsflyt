package no.nav.bidrag.arbeidsflyt.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelseBarn
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "behandling")
data class Behandling(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,
    @Column(name = "behandlingsid")
    var behandlingsid: Long? = null,
    @Column(name = "søknadsid")
    var søknadsid: Long? = null,
    @Column(columnDefinition = "jsonb", name = "oppgave")
    @JdbcTypeCode(SqlTypes.JSON)
    var oppgave: BehandlingOppgave? = null,
    @Column(name = "behandles_av_flere_søknader")
    var behandlesAvFlereSøknader: Boolean = false,
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: BehandlingStatusType,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "barn")
    var barn: BehandlingBarn? = null,
    var mottattDato: LocalDate,
    var normDato: LocalDate? = null,
    val enhet: String,
    @Column(name = "opprettet_tidspunkt")
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "endret_tidspunkt")
    var endretTidspunkt: LocalDateTime = LocalDateTime.now(),
)

data class BehandlingOppgave(
    var oppgaver: Set<BehandlingOppgaveDetaljer> = emptySet(),
)

data class BehandlingOppgaveDetaljer(
    val saksnummer: String,
    val oppgaveId: Long,
    val enhet: String,
    val søknadsid: Long?,
    var ferdigstilt: Boolean = false,
)

data class BehandlingBarn(
    var barn: List<BehandlingHendelseBarn> = emptyList(),
)
