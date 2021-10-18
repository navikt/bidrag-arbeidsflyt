package no.nav.bidrag.arbeidsflyt.model

// Fagområder på en journalpost
object Fagomrade {
    const val BIDRAG = "BID"
    const val FARSKAP = "FAR"
}

// Journalstatus som er viktig for en JournalpostHendelse
object Journalstatus {
    const val MOTTATT = "M"
}


object MiljoVariabler {
    const val OPPGAVE_URL = "OPPGAVE_URL"
}

object Token {
    const val OPPGAVE_CLIENT_REGISTRATION_ID = "oppgave"
}

// sporingsdata fra hendelse json
const val CORRELATION_ID = "correlationId"

// OAUTH2 JWT client registration
const val AOUTH2_JWT_REGISTRATION = "bidrag-arbeidsflyt"
