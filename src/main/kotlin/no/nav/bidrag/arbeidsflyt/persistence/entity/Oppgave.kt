package no.nav.bidrag.arbeidsflyt.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveType
import java.time.LocalDate
import java.time.LocalDateTime

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
    var oppgavetype: String,
    @Column(name = "opprettet_timestamp")
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "frist")
    val frist: LocalDate?,
    var søknadsoppgave: Boolean = false,
    var tildeltEnhetsnr: String? = null,
) {
    fun erJournalforingOppgave(): Boolean = oppgavetype == "JFR"

    fun erSøknadsoppgave(): Boolean = søknadsoppgave

    fun oppdaterOppgaveFraHendelse(oppgaveHendelse: OppgaveData) {
        status = oppgaveHendelse.status?.name!!
        journalpostId = oppgaveHendelse.journalpostId
        oppgavetype = oppgaveHendelse.oppgavetype!!
        tildeltEnhetsnr = oppgaveHendelse.tildeltEnhetsnr
        søknadsoppgave = oppgaveHendelse.erSøknadsoppgave
    }
}
