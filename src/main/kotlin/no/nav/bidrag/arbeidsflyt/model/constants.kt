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

object Token {
    const val OPPGAVE_CLIENT_REGISTRATION_ID = "oppgave"
}

// misc const
const val CORRELATION_ID = "correlationId"
const val JOURNALFORINGSOPPGAVE = "JFR"
const val AOUTH2_JWT_REGISTRATION = "bidrag-arbeidsflyt"
