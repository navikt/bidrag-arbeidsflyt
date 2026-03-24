package no.nav.bidrag.arbeidsflyt.persistence.repository

import no.nav.bidrag.arbeidsflyt.persistence.entity.Behandling
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface BehandlingRepository : CrudRepository<Behandling, Long> {
    @Query("select b from Behandling b where b.søknadsid = :søknadId")
    fun finnForSøknadId(
        søknadId: Long,
    ): Behandling?

    @Query("select b from Behandling b where b.behandlingsid = :behandlingId")
    fun finnForBehandlingId(
        behandlingId: Long,
    ): Behandling?

    @Query("select b from Behandling b where b.behandlingsid = :behandlingId")
    fun finnBehandlingerSomHarÅpenOppgave(
        behandlingId: Long,
    ): List<Behandling>

    @Query(
        value = """
    SELECT DISTINCT b.* FROM behandling b,
    jsonb_array_elements(b.oppgave->'oppgaver') AS oppgave_elem
    WHERE oppgave_elem->>'oppgaveId' = :oppgaveId
    """,
        nativeQuery = true,
    )
    fun finnBehandlingSomHarOppgave(
        @Param("oppgaveId") oppgaveId: String,
    ): List<Behandling>

    @Query(
        value = """
            SELECT b.*
            FROM behandling b
            WHERE b.barn @> CAST('{"barn":[{"status":"UNDER_BEHANDLING"}]}' AS jsonb)
              AND (b.status_sjekket_tidspunkt is null or b.status_sjekket_tidspunkt < :cutoff)
            """,
        nativeQuery = true,
    )
    fun finnBehandlingerMedSøknadUnderBehandlingStatusSjekketEldreEnn(
        @Param("cutoff") cutoff: LocalDateTime,
    ): List<Behandling>
}
