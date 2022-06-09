package no.nav.bidrag.arbeidsflyt.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeografiskTilknytningResponse(var enhetIdent: String, var enhetNavn: String)