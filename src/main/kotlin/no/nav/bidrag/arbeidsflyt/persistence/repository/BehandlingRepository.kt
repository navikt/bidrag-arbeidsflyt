package no.nav.bidrag.arbeidsflyt.persistence.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.persistence.entity.Behandling
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger {}

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
            SELECT b.id
            FROM behandling b
            WHERE b.barn @> CAST('{"barn":[{"status":"UNDER_BEHANDLING"}]}' AS jsonb)
              AND (b.status_sjekket_tidspunkt is null or b.status_sjekket_tidspunkt < :cutoff)
            """,
        nativeQuery = true,
    )
    fun finnBehandlingIdsMedSøknadUnderBehandlingStatusSjekketEldreEnn(
        @Param("cutoff") cutoff: LocalDateTime,
    ): List<Long>

    @Query("select b from Behandling b where b.id in :ids")
    fun finnBehandlingerByIds(
        @Param("ids") ids: List<Long>,
    ): List<Behandling>

    /**
     * Safely fetches behandlinger that have barn under treatment status and haven't been checked recently.
     * Skips rows with invalid JSON that cannot be deserialized.
     */
    fun finnBehandlingerMedSøknadUnderBehandlingStatusSjekketEldreEnn(
        cutoff: LocalDateTime,
    ): List<Behandling> {
        val ids = finnBehandlingIdsMedSøknadUnderBehandlingStatusSjekketEldreEnn(cutoff)
        if (ids.isEmpty()) return emptyList()

        return ids.mapNotNull { id ->
            try {
                findById(id).orElse(null)
            } catch (ex: Exception) {
                LOGGER.warn(ex) { "Skipping behandling with id=$id due to invalid JSONB data that cannot be deserialized" }
                null
            }
        }
    }
}
