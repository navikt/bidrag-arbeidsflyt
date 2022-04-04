package no.nav.bidrag.arbeidsflyt.utils

import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.repository.JournalpostRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class TestDataGenerator {

    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

    fun hentJournalpost(journalpostId: String): Optional<Journalpost> {
        return journalpostRepository.findByJournalpostId(journalpostId)
    }

    fun opprettJournalpost(journalpost: Journalpost){
        journalpostRepository.save(journalpost)
    }

    fun deleteAll(){
        journalpostRepository.deleteAll()
    }
    
}