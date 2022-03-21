package no.nav.bidrag.arbeidsflyt.persistence.repository

import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface JournalpostRepository: CrudRepository<Journalpost, Long> {

    fun findByJournalpostId(journalpostId: String): Optional<Journalpost>
}