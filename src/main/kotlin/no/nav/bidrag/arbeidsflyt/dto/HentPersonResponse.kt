package no.nav.bidrag.arbeidsflyt.dto

data class HentPersonResponse(
    val ident: String? = null,
    val aktørId: String? = null
)

data class PersonRequestDto(
    val verdi: String? = null
)