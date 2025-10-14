package no.nav.bidrag.arbeidsflyt.persistence.repository

import no.nav.bidrag.arbeidsflyt.persistence.entity.Behandling
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface BehandlingRepository : CrudRepository<Behandling, Long> {
    @Query("select b from Behandling b where b.behandlingsid = :behandlingId or b.søknadsid = :søknadId")
    fun finnForBehandlingEllerSøknadId(
        behandlingId: Long?,
        søknadId: Long? = null,
    ): Behandling?

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
}
