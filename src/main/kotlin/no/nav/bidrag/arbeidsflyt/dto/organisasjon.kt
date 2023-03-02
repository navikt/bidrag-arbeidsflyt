package no.nav.bidrag.arbeidsflyt.dto

data class HentEnhetRequest(
    val ident: String,
    val tema: String? = null,
    val behandlingstema: String? = null
)