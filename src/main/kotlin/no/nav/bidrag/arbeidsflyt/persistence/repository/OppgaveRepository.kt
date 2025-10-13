package no.nav.bidrag.arbeidsflyt.persistence.repository

import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.time.LocalDateTime

interface OppgaveRepository : CrudRepository<Oppgave, Long> {
    fun deleteByOppgaveId(oppgaveId: Long)

    fun findByOppgaveId(oppgaveId: Long): Oppgave?

    @Query("SELECT o FROM Oppgave o WHERE o.opprettetTidspunkt < :date")
    fun finnOppgaverEldreEnnDato(date: LocalDateTime): List<Oppgave>
}
