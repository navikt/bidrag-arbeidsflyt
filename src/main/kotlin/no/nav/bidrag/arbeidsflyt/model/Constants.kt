package no.nav.bidrag.arbeidsflyt.model

// Fagområder på en journalpost
object Fagomrade {
    const val BIDRAG = "BID"
    const val BIDRAGINNKREVING = "BII"
    const val FARSKAP = "FAR"
}

fun tilFagområdeBeskrivelse(fagomrade: String): String =
    if (fagomrade == Fagomrade.FARSKAP) {
        "Farskap"
    } else if (fagomrade == Fagomrade.BIDRAG) {
        "Bidrag"
    } else {
        fagomrade
    }

// misc const
const val CORRELATION_ID = "correlationId"
const val JOURNALFORINGSOPPGAVE = "JFR"

const val ENHET_FAGPOST = "2950"
const val ENHET_IT_AVDELINGEN = "2990"
const val ENHET_YTELSE = "2830"
const val ENHET_KLAGE_BIDRAG_VAKT = "4254"
const val ENHET_FARSKAP = "4860"
