package no.nav.bidrag.arbeidsflyt.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.Optional


@JsonIgnoreProperties(ignoreUnknown = true)
class OppgaveEndretHendelse {
    val id: Long? = null
    val tildeltEnhetsnr: String? = null
    val journalpostId: String? = null
    val tilordnetRessurs: String? = null
    val temagruppe: String? = null
    val tema: String? = null
    val behandlingstema: String? = null
    val oppgavetype: String? = null
    val behandlingstype: String? = null
    val versjon: Int? = null
    val status: String? = null
    val statuskategori: String? = null
    val behandlesAvApplikasjon: String? = null
    val ident: Ident? = null
    val metadata: Map<String, String>? = null

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Ident {
        val identType: String? = null
        val verdi: String? = null

        val folkeregisterident: String? = null
    }

    @JsonIgnore
    fun erÅpen(): Boolean {
        return "AAPEN".equals(statuskategori, ignoreCase = true)
    }

    @JsonIgnore
    fun harAktørID(): Boolean {
        return ident != null && "AKTOERID".equals(ident.identType, ignoreCase = true) && ident.verdi != null
    }

    @JsonIgnore
    fun hentAktørID(): String? {
        return Optional.ofNullable(ident)
            .map(Ident::verdi)
            .orElseThrow { NoSuchElementException("Finner ikke aktørID") }
    }

    @JsonIgnore
    fun harSammeVersjon(versjon: Int): Boolean {
        return this.versjon == versjon
    }
}