package no.nav.bidrag.arbeidsflyt.hendelse

data class JournalpostHendelse(
    var journalpostId: String = "",
    var hendelse: String = "",
    var sporing: Sporingsdata? = null
)

data class Sporingsdata(var correlationId: String? = null, var opprettet: String? = null)
