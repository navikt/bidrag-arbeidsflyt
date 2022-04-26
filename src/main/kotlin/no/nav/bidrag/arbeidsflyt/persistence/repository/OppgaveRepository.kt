package no.nav.bidrag.arbeidsflyt.persistence.repository

import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface OppgaveRepository: CrudRepository<Oppgave, Long> {
    fun deleteByOppgaveId(oppgaveId: Long)
    fun findByOppgaveId(oppgaveId: Long): Optional<Oppgave>
}