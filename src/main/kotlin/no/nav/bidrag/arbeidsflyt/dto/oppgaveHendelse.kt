package no.nav.bidrag.arbeidsflyt.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppgaveHendelse(
    val id: Long? = null,
    val tildeltEnhetsnr: String? = null,
    val journalpostId: String? = null,
    val tilordnetRessurs: String? = null,
    val temagruppe: String? = null,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val behandlingstype: String? = null,
    val versjon: Int? = null,
    val status: OppgaveStatus? = null,
    val statuskategori: String? = null,
    val endretAv: String? = null,
    val opprettetAv: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val ident: Ident? = null,
    val metadata: Map<String, String>? = null
){

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Ident(
        val identType: String? = null,
        val verdi: String? = null,
        val folkeregisterident: String? = null
    )

    fun erTemaBIDEllerFAR(): Boolean = tema == "BID" || tema == "FAR"
    fun erJournalforingOppgave(): Boolean = oppgavetype == "JFR"
    fun erStatusKategoriAapnet(): Boolean = statuskategori == "AAPNET"
    fun erStatusFerdigstilt(): Boolean = status == OppgaveStatus.FERDIGSTILT
    fun erStatusAapnet(): Boolean = status == OppgaveStatus.AAPNET
    fun erStatusOpprettet(): Boolean = status == OppgaveStatus.OPPRETTET
}