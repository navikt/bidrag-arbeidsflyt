package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.model.Detalj.ENHETSNUMMER
import no.nav.bidrag.arbeidsflyt.model.Detalj.ENHETSNUMMER_NYTT
import no.nav.bidrag.arbeidsflyt.model.Detalj.FAGOMRADE
import no.nav.bidrag.arbeidsflyt.model.DetaljVerdi.FAGOMRADE_BIDRAG
import no.nav.bidrag.arbeidsflyt.model.DetaljVerdi.FAGOMRADE_FARSKAP

data class JournalpostHendelse(
    var journalpostId: String = "",
    var hendelse: String = "",
    var sporing: Sporingsdata? = null,
    private var detaljer: Map<String, String> = emptyMap()
) {
    fun hentHendelse() = Hendelse.values().find { it.name == hendelse } ?: Hendelse.NO_SUPPORT

    internal fun hentEnhetsnummer() = detaljer[ENHETSNUMMER] ?: doThrow("Mangler $ENHETSNUMMER blant hendelsedata")
    internal fun erBytteTilInterntFagomrade() = detaljer[FAGOMRADE] == FAGOMRADE_BIDRAG || detaljer[FAGOMRADE] == FAGOMRADE_FARSKAP
    internal fun hentFagomradeFraId() = journalpostId.split('-')[0]
    internal fun hentJournalpostIdUtenPrefix() = journalpostId.split('-')[1]
    internal fun hentNyttJournalforendeEnhetsnummer() = detaljer[ENHETSNUMMER_NYTT] ?: doThrow("Mangler $ENHETSNUMMER_NYTT blant hendelsedata")

    private fun doThrow(message: String): String = throw IllegalStateException(message)
}

data class Sporingsdata(var correlationId: String? = null, var opprettet: String? = null)
enum class Hendelse {
    AVVIK_ENDRE_FAGOMRADE,
    AVVIK_OVERFOR_TIL_ANNEN_ENHET,
    JOURNALFOR_JOURNALPOST,
    NO_SUPPORT
}
