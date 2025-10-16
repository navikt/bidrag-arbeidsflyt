package no.nav.bidrag.arbeidsflyt.utils

import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.arbeidsflyt.persistence.repository.BehandlingRepository
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
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var dlqKafkaRepository: DLQKafkaRepository

    fun hentDlKafka(): List<DLQKafka> = dlqKafkaRepository.findAll()

    fun hentOppgave(oppgaveId: Long): Optional<Oppgave> = oppgaveRepository.findById(oppgaveId)

    fun opprettDLQMelding(dlqKafka: DLQKafka) {
        dlqKafkaRepository.save(dlqKafka)
    }

    fun opprettOppgave(oppgave: Oppgave) {
        oppgaveRepository.save(oppgave)
    }

    fun deleteAll() {
        try {
            dlqKafkaRepository.deleteAll()
        } catch (e: Exception) {
        }
        journalpostRepository.deleteAll()
        oppgaveRepository.deleteAll()
        behandlingRepository.deleteAll()
    }

    fun hentJournalpost(journalpostId: String): Journalpost? = journalpostRepository.findByJournalpostId(journalpostId)

    fun opprettJournalpost(journalpost: Journalpost) {
        journalpostRepository.save(journalpost)
    }
}
