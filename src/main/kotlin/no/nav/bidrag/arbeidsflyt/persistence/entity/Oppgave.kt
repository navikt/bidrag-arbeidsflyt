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

    @Column(name = "journalpost_id")
    var journalpostId: String?,

    @Column(name = "status")
    var status: String,

    @Column(name = "statuskategori")
    var statuskategori: String,

    @Column(name = "tema")
    var tema: String,

    @Column(name = "oppgavetype")
    var oppgavetype: String,

    @Column(name = "ident")
    var ident: String? = null
) {
    fun erStatusFerdigstilt(): Boolean = status == OppgaveStatus.FERDIGSTILT.name
    fun erJournalforingOppgave(): Boolean = oppgavetype == "JFR"
    fun harJournalpostId(): Boolean = journalpostId != null
    fun oppdaterOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse){
        status = oppgaveHendelse.status?.name!!
        statuskategori = oppgaveHendelse.statuskategori?.name!!
        journalpostId = oppgaveHendelse.journalpostId
        oppgavetype = oppgaveHendelse.oppgavetype!!
        tema = oppgaveHendelse.tema!!
        ident = oppgaveHendelse.hentIdent
    }
}