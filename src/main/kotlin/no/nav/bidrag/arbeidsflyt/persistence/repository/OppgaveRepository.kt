package no.nav.bidrag.arbeidsflyt.persistence.repository

import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import org.springframework.data.repository.CrudRepository

interface OppgaveRepository: CrudRepository<Oppgave, Long> {

    fun findAllByJournalpostIdContainingAndStatuskategoriAndOppgavetype(journalpostId: String, statusKategori: String, oppgaveType: String): List<Oppgave>
}