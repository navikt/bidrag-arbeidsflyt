package no.nav.bidrag.arbeidsflyt.utils

import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.arbeidsflyt.persistence.repository.DLQKafkaRepository
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
    @Autowired
    lateinit var dlqKafkaRepository: DLQKafkaRepository

    fun hentDlKafka(): List<DLQKafka> {
        return dlqKafkaRepository.findAll()
    }
    fun hentOppgave(oppgaveId: Long): Optional<Oppgave> {
        return oppgaveRepository.findById(oppgaveId)
    }

    fun hentJournalpost(journalpostId: String): Optional<Journalpost> {
        return journalpostRepository.findByJournalpostId(journalpostId)
    }


    fun opprettOppgave(oppgave: Oppgave){
        oppgaveRepository.save(oppgave)
    }

    fun opprettJournalpost(journalpost: Journalpost){
        journalpostRepository.save(journalpost)
    }

    fun deleteAll(){
        journalpostRepository.deleteAll()
        oppgaveRepository.deleteAll()
    }
    
}