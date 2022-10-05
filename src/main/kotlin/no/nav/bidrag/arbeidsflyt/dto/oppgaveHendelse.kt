package no.nav.bidrag.arbeidsflyt.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.bidrag.arbeidsflyt.model.isBidJournalpostId
import java.time.LocalDate
import java.time.ZonedDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppgaveHendelse(
    val id: Long,
    val versjon: Int,
    val endretAvEnhetsnr: String? = null,
    val tildeltEnhetsnr: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val journalpostId: String? = null,
    val tilordnetRessurs: String? = null,
    val saksreferanse: String? = null,
    val beskrivelse: String? = null,
    val temagruppe: String? = null,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val behandlingstype: String? = null,
    val status: OppgaveStatus? = null,
    val statuskategori: Oppgavestatuskategori? = null,
    val endretAv: String? = null,
    val opprettetAv: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val ident: Ident? = null,
    val metadata: Map<String, String>? = null,
    val fristFerdigstillelse: LocalDate? = null,
    val aktivDato: LocalDate? = null,
    val opprettetTidspunkt: ZonedDateTime? = null,
    val ferdigstiltTidspunkt: ZonedDateTime? = null,
    val endretTidspunkt: ZonedDateTime? = null
){

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Ident(
        var identType: OppgaveIdentType? = null,
        val verdi: String? = null,
        val folkeregisterident: String? = null
    )

    fun erTemaBIDEllerFAR(): Boolean = tema == "BID" || tema == "FAR"
    fun erStatusFerdigstilt(): Boolean = status == OppgaveStatus.FERDIGSTILT
    fun erAapenJournalforingsoppgave(): Boolean = erStatusKategoriAapen && erJournalforingOppgave
    fun erAapenVurderDokumentOppgave(): Boolean = erStatusKategoriAapen && erVurderDokumentOppgave
    fun erAvsluttetJournalforingsoppgave(): Boolean = erStatusKategoriAvsluttet && erJournalforingOppgave

    internal val erStatusKategoriAapen get() = statuskategori == Oppgavestatuskategori.AAPEN
    internal val erStatusKategoriAvsluttet get() = statuskategori == Oppgavestatuskategori.AVSLUTTET
    internal val erJournalforingOppgave get() = oppgavetype == OppgaveType.JFR.name
    internal val erVurderDokumentOppgave get() = oppgavetype == OppgaveType.VUR.name
    internal val erBehandleDokumentOppgave get() = oppgavetype == OppgaveType.BEH_SAK.name
    internal val hasJournalpostId get() = journalpostId != null
    internal val journalpostIdUtenPrefix get() = if (harJournalpostIdPrefix() && hasJournalpostId) journalpostId!!.split('-')[1] else journalpostId
    internal fun harJournalpostIdPrefix() = hasJournalpostId && journalpostId!!.contains("-")
    internal val hentIdent get() = if (ident?.identType == OppgaveIdentType.AKTOERID) ident.folkeregisterident else ident?.verdi
    internal val hentBnr get() = if (ident?.identType == OppgaveIdentType.BNR) ident.verdi else null
    internal val hentAktoerId get() = if (ident?.identType == OppgaveIdentType.AKTOERID) ident.verdi else null
//    internal val journalpostIdMedPrefix get() = if (journalpostId.isNullOrEmpty()) journalpostId else if(harJournalpostIdPrefix()) journalpostId else "JOARK-$journalpostId"

    internal val journalpostIdMedPrefix get() = if (journalpostId.isNullOrEmpty() || harJournalpostIdPrefix()) journalpostId else if(isBidJournalpostId(journalpostId)) "BID-$journalpostId" else "JOARK-$journalpostId"

    override fun toString(): String {
        return listOf(
            "journalpostId=${journalpostId}",
            "oppgaveId=${id}",
            "statuskategori=${statuskategori}",
            "tema=${tema}",
            "oppgavetype=${oppgavetype}",
            "opprettetAv=${opprettetAv}",
            "tildeltEnhetsnr=${tildeltEnhetsnr}",
            "opprettetAvEnhetsnr=${opprettetAvEnhetsnr}",
            "versjon=${versjon}",
            "saksreferanse=${saksreferanse}",
            "fristFerdigstillelse=${fristFerdigstillelse}",
            "tilordnetRessurs=${tilordnetRessurs}",
            "status=${status}"
        ).joinToString(", ")
    }
}