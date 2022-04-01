package no.nav.bidrag.arbeidsflyt.utils

import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.arbeidsflyt.persistence.repository.JournalpostRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.OppgaveRepository
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

    fun initTestData(){
        journalpostRepository.save(createJournalpost(JOURNALPOST_ID_1, gjelderId = PERSON_IDENT_1))
        journalpostRepository.save(createJournalpost(JOURNALPOST_ID_2, gjelderId = PERSON_IDENT_2))
        journalpostRepository.save(createJournalpost(JOURNALPOST_ID_3, status = "J", gjelderId = PERSON_IDENT_3))
        journalpostRepository.save(createJournalpost(BID_JOURNALPOST_ID_1, gjelderId = PERSON_IDENT_3))
        journalpostRepository.save(createJournalpost(BID_JOURNALPOST_ID_2, status = "J", gjelderId = PERSON_IDENT_3))
    }

    fun deleteAll(){
        journalpostRepository.deleteAll()
    }
    
}