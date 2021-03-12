package no.nav.bidrag.arbeidsflyt.model

// Detaljer (keys) i en JournalpostHendelse
object Detalj {
    const val FAGOMRADE = "fagomrade"
    const val ENHETSNUMMER = "enhetsnummer"
    const val ENHETSNUMMER_GAMMELT = "gammelt-enhetsnummer"
}

// Detaljer (values) i en JournalpostHendelse
object DetaljVerdi {
    const val FAGOMRADE_BIDRAG = "BID"
    const val FAGOMRADE_FARSKAP = "FAR"
}

// sproringsdata fra hendelse json
const val CORRELATION_ID = "correlationId"
