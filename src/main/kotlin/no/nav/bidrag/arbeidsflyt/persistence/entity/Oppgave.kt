package no.nav.bidrag.arbeidsflyt.persistence.entity

import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Oppgave (

    @Id
    @Column(name = "oppgave_id")
    var oppgaveId: Long,

    @Column(name = "journalpostId")
    var journalpostId: String?,

    @Column(name = "status")
    var status: String?,

    @Column(name = "tema")
    var tema: String?,

    @Column(name = "oppgavetype")
    var oppgavetype: String?
) {
    fun erJournalforingOppgave(): Boolean = oppgavetype == "JFR"
    fun oppdaterOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse){
        status = oppgaveHendelse.status?.name
        journalpostId = oppgaveHendelse.journalpostId
        oppgavetype = oppgaveHendelse.oppgavetype
        tema = oppgaveHendelse.tema
    }
}