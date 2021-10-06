package no.nav.bidrag.arbeidsflyt.model

// Detaljer (keys) i en JournalpostHendelse
object Detalj {
    const val FAGOMRADE = "fagomrade"
    const val ENHETSNUMMER = "enhetsnummer"
    const val ENHETSNUMMER_GAMMELT = "gammeltEnhetsnummer"
    const val ENHETSNUMMER_NYTT = "nyttEnhetsnummer"
    const val AKTOER_ID = "aktoerId"
}

// Verdier til Detaljer i en JournalpostHendelse
object DetaljVerdi {
    const val FAGOMRADE_BIDRAG = "BID"
    const val FAGOMRADE_FARSKAP = "FAR"
}

object MiljoVariabler {
    const val NAIS_APP_NAME = "NAIS_APP_NAME"
    const val OPPGAVE_URL = "OPPGAVE_URL"
}

object Token {
    const val OPPGAVE_CLIENT_REGISTRATION_ID = "oppgave"
}

// sporingsdata fra hendelse json
const val CORRELATION_ID = "correlationId"

// OAUTH2 JWT client registration
const val AOUTH2_JWT_REGISTRATION = "bidrag-arbeidsflyt"
