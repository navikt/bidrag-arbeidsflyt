package no.nav.bidrag.arbeidsflyt.utils

import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.arbeidsflyt.persistence.repository.JournalpostRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.OppgaveRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import kotlin.random.Random

var JOURNALPOST_ID_1 = "124123"
var JOURNALPOST_ID_2 = "142312"
var JOURNALPOST_ID_3 = "5125125"
@Component
class TestDataGenerator {

    @Autowired
    lateinit var journalpostRepository: JournalpostRepository
    @Autowired
    lateinit var oppgaveRepository: OppgaveRepository

    fun createOppgave(journalpostId: String, status: String? = "OPPRETTET", oppgaveType: String? = "JFR"): Oppgave {
        return Oppgave(
            oppgaveId = Random(5).nextLong(),
            journalpostId = journalpostId,
            status = status,
            tema = "BID",
            oppgavetype = oppgaveType
        )
    }

    fun  createJournalpost(journalpostId: String, status: String? = "M"): Journalpost {
        return Journalpost(
            journalpostId = journalpostId,
            status = status
        )
    }

    fun initTestData(){
        oppgaveRepository.save(createOppgave(JOURNALPOST_ID_1))
        oppgaveRepository.save(createOppgave(JOURNALPOST_ID_2))
        oppgaveRepository.save(createOppgave(JOURNALPOST_ID_3))

        journalpostRepository.save(createJournalpost(JOURNALPOST_ID_1))
        journalpostRepository.save(createJournalpost(JOURNALPOST_ID_2))
        journalpostRepository.save(createJournalpost(JOURNALPOST_ID_3))
    }
    
}