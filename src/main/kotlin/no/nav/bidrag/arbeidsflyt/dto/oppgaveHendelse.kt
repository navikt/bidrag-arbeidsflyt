package no.nav.bidrag.arbeidsflyt.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.Optional

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppgaveEndretHendelse(
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
    val status: Status? = null,
    val statuskategori: String? = null,
    val endretAv: String? = null,
    val opprettetAv: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val ident: Ident? = null,
    val metadata: Map<String, String>? = null
){

    enum class Status {
        FERDIGSTILT,
        AAPNET,
        OPPRETTET,
        FEILREGISTRERT,
        UNDER_BEHANDLING
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Ident(
        val identType: String? = null,
        val verdi: String? = null,
        val folkeregisterident: String? = null
    )

    fun erTemaBIDEllerFAR(): Boolean = tema == "BID" || tema == "FAR"
    fun erJournalforingOppgave(): Boolean = oppgavetype == "JFR"
    fun erStatusKategoriAapnet(): Boolean = statuskategori == "AAPNET"
    fun erStatusFerdigstilt(): Boolean = status == Status.FERDIGSTILT
    fun erStatusAapnet(): Boolean = status == Status.AAPNET
    fun erStatusOpprettet(): Boolean = status == Status.OPPRETTET

    fun harAktoerID(): Boolean {
        return ident != null && "AKTOERID".equals(ident.identType, ignoreCase = true) && ident.verdi != null
    }

    fun hentAktoerID(): String? {
        return Optional.ofNullable(ident)
            .map(Ident::verdi)
            .orElseThrow { NoSuchElementException("Finner ikke aktørID") }
    }

    fun harSammeVersjon(versjon: Int): Boolean {
        return this.versjon == versjon
    }
}