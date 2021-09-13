package no.nav.bidrag.arbeidsflyt.model

// Detaljer (keys) i en JournalpostHendelse
object Detalj {
    const val FAGOMRADE = "fagomrade"
    const val ENHETSNUMMER = "enhetsnummer"
    const val ENHETSNUMMER_GAMMELT = "gammeltEnhetsnummer"
    const val ENHETSNUMMER_NYTT = "nyttEnhetsnummer"
}

// Verdier til Detaljer i en JournalpostHendelse
object DetaljVerdi {
    const val FAGOMRADE_BIDRAG = "BID"
    const val FAGOMRADE_FARSKAP = "FAR"
}

object MiljoVariabler {
    const val NAIS_APP_NAME = "NAIS_APP_NAME"
    const val AZURE_APP_CLIENT_ID = "AZURE_APP_CLIENT_ID"
    const val OPPGAVE_URL = "OPPGAVE_URL"
}

// sporingsdata fra hendelse json
const val CORRELATION_ID = "correlationId"

// OAUTH2 JWT client registration
const val AOUTH2_JWT_REGISTRATION = "bidrag-arbeidsflyt"
