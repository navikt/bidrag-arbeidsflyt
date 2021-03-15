package no.nav.bidrag.arbeidsflyt.model

// Detaljer (keys) i en JournalpostHendelse
object Detalj {
    const val FAGOMRADE = "fagomrade"
    const val ENHETSNUMMER = "enhetsnummer"
    const val ENHETSNUMMER_NYTT = "nytt-enhetsnummer"
}

// Verdier til Detaljer i en JournalpostHendelse
object DetaljVerdi {
    const val FAGOMRADE_BIDRAG = "BID"
    const val FAGOMRADE_FARSKAP = "FAR"
}

// sproringsdata fra hendelse json
const val CORRELATION_ID = "correlationId"
