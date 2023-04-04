package no.nav.bidrag.arbeidsflyt.dto

data class HentPersonResponse(
    val ident: String? = null,
    val aktoerId: String? = null
)

data class PersonRequestDto(
    val ident: String? = null
)