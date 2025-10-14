package no.nav.bidrag.arbeidsflyt.persistence.repository

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.bidrag.arbeidsflyt.persistence.entity.Behandling
import no.nav.bidrag.transport.felles.commonObjectmapper
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

    @Modifying
    @Transactional
    fun oppdaterStatusPåBehandlingOppgaveH2(oppgaveId: Long): Int {
        val behandlinger = findAll()
        var count = 0
        behandlinger.forEach { behandling ->
            behandling.oppgave =
                behandling.oppgave?.copy(
                    oppgaver =
                        behandling.oppgave
                            ?.oppgaver
                            ?.map { oppgave ->
                                if (oppgave.oppgaveId == oppgaveId) {
                                    oppgave.ferdigstilt = true
                                    count++
                                }
                                oppgave
                            }?.toSet() ?: emptySet(),
                )
        }
        return count
    }
}
