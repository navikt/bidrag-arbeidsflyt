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


const val ENHET_FAGPOST="2950"
const val ENHET_IT_AVDELINGEN="2990"
const val ENHET_YTELSE="2830"
const val ENHET_FARSKAP="4860"