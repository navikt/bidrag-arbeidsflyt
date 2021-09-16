package no.nav.bidrag.arbeidsflyt.model

data class JournalpostHendelse(
    var journalpostId: String = "",
    var hendelse: String = "",
    var sporing: Sporingsdata? = null,
    private var detaljer: Map<String, String> = emptyMap()
) {
    fun hentHendelse() = Hendelse.values().find { it.name == hendelse } ?: Hendelse.NO_SUPPORT

    internal fun hentEnhetsnummer() = detaljer[Detalj.ENHETSNUMMER] ?: doThrow("Mangler ${Detalj.ENHETSNUMMER} blant hendelsedata")
    internal fun erBytteTilInterntFagomrade() = detaljer[Detalj.FAGOMRADE] == DetaljVerdi.FAGOMRADE_BIDRAG || detaljer[Detalj.FAGOMRADE] == DetaljVerdi.FAGOMRADE_FARSKAP
    internal fun hentFagomradeFraDetaljer() = detaljer[Detalj.FAGOMRADE] ?: DetaljVerdi.FAGOMRADE_BIDRAG
    internal fun hentJournalpostIdUtenPrefix() = journalpostId.split('-')[1]
    internal fun hentNyttJournalforendeEnhetsnummer() = detaljer[Detalj.ENHETSNUMMER_NYTT] ?: doThrow("Mangler ${Detalj.ENHETSNUMMER_NYTT} blant hendelsedata")

    private fun doThrow(message: String): String = throw IllegalStateException(message)
}

data class Sporingsdata(val correlationId: String? = null, val opprettet: String? = null, val saksbehandlersNavn : String? = null)
