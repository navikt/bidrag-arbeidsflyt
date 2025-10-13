package no.nav.bidrag.arbeidsflyt.persistence.repository

import no.nav.bidrag.arbeidsflyt.persistence.entity.Behandling
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface BehandlingRepository : CrudRepository<Behandling, Long> {
    @Query("select b from Behandling b where b.behandlingsid = :behandlingId or b.søknadsid = :søknadId")
    fun finnForBehandlingEllerSøknadId(
        behandlingId: Long?,
        søknadId: Long? = null,
    ): Behandling?

    @Modifying
    @Query(
        value = """
        UPDATE behandling 
        SET oppgave = jsonb_set(
            oppgave, 
            '{oppgaver}', 
            (
                SELECT jsonb_agg(
                    CASE 
                        WHEN elem->>'oppgaveId' = :oppgaveId 
                        THEN jsonb_set(elem, '{ferdigstilt}', 'true'::jsonb) 
                        ELSE elem 
                    END
                ) 
                FROM jsonb_array_elements(oppgave->'oppgaver') AS elem
            )
        ) 
        WHERE jsonb_exists(oppgave->'oppgaver', :oppgaveId)
        """,
        nativeQuery = true,
    )
    fun oppdaterStatusPåBehandlingOppgave(
        @Param("oppgaveId") oppgaveId: Long,
    ): Int
}
