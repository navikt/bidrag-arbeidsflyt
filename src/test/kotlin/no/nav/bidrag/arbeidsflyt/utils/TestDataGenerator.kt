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
    @Autowired
    lateinit var oppgaveRepository: OppgaveRepository

    fun hentOppgave(oppgaveId: Long): Optional<Oppgave> {
        return oppgaveRepository.findById(oppgaveId)
    }
    fun hentJournalpost(journalpostId: String): Optional<Journalpost> {
        return journalpostRepository.findByJournalpostId(journalpostId)
    }

    fun initTestData(){
        oppgaveRepository.save(createOppgave(OPPGAVE_ID_1, JOURNALPOST_ID_1, ident = PERSON_IDENT_1))
        oppgaveRepository.save(createOppgave(OPPGAVE_ID_2, JOURNALPOST_ID_2, ident = PERSON_IDENT_2))
        oppgaveRepository.save(createOppgave(OPPGAVE_ID_3, JOURNALPOST_ID_3, ident = PERSON_IDENT_3))
        oppgaveRepository.save(createOppgave(OPPGAVE_ID_4, BID_JOURNALPOST_ID_1, ident = PERSON_IDENT_3))
        oppgaveRepository.save(createOppgave(OPPGAVE_ID_5, BID_JOURNALPOST_ID_2, ident = PERSON_IDENT_3))

        journalpostRepository.save(createJournalpost(JOURNALPOST_ID_1, gjelderId = PERSON_IDENT_1))
        journalpostRepository.save(createJournalpost(JOURNALPOST_ID_2, gjelderId = PERSON_IDENT_2))
        journalpostRepository.save(createJournalpost(JOURNALPOST_ID_3, status = "J", gjelderId = PERSON_IDENT_3))
        journalpostRepository.save(createJournalpost(BID_JOURNALPOST_ID_1, gjelderId = PERSON_IDENT_3))
        journalpostRepository.save(createJournalpost(BID_JOURNALPOST_ID_2, status = "J", gjelderId = PERSON_IDENT_3))
    }

    fun deleteAll(){
        oppgaveRepository.deleteAll()
        journalpostRepository.deleteAll()
    }
    
}