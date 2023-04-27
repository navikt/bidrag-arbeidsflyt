package no.nav.bidrag.arbeidsflyt.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse

@Entity
data class Oppgave(

    @Id
    @Column(name = "oppgave_id")
    var oppgaveId: Long,

    @Column(name = "journalpost_id")
    var journalpostId: String?,

    @Column(name = "status")
    var status: String,

    @Column(name = "oppgavetype")
    var oppgavetype: String

) {
    fun erJournalforingOppgave(): Boolean = oppgavetype == "JFR"
    fun oppdaterOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse) {
        status = oppgaveHendelse.status?.name!!
        journalpostId = oppgaveHendelse.journalpostId
        oppgavetype = oppgaveHendelse.oppgavetype!!
    }
}
