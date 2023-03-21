package no.nav.bidrag.arbeidsflyt.persistence.repository

import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import org.springframework.data.repository.CrudRepository

interface OppgaveRepository: CrudRepository<Oppgave, Long> {
    fun deleteByOppgaveId(oppgaveId: Long)
    fun findByOppgaveId(oppgaveId: Long): Oppgave?
}