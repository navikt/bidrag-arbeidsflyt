package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.model.Detalj
import no.nav.bidrag.arbeidsflyt.model.Detalj.FAGOMRADE
import no.nav.bidrag.arbeidsflyt.model.DetaljVerdi.FAGOMRADE_BIDRAG
import no.nav.bidrag.arbeidsflyt.model.DetaljVerdi.FAGOMRADE_FARSKAP

data class JournalpostHendelse(
    var journalpostId: String = "",
    var hendelse: String = "",
    var sporing: Sporingsdata? = null,
    private var detaljer: Map<String, String> = emptyMap(),
    var journalforendeEnhet: JournalforendeEnhet? = null
) {
    fun hentHendelse(): Hendelse {
        val hendelsen = Hendelse.values().find { it.name == hendelse } ?: Hendelse.NO_SUPPORT

        if (hendelsen == Hendelse.AVVIK_OVERFOR_TIL_ANNEN_ENHET) {
            journalforendeEnhet = JournalforendeEnhet(hentGammeltEnhetsnummer(), hentNyttEnhetsnummer())
        }

        return hendelsen
    }

    private fun hentGammeltEnhetsnummer() = detaljer[Detalj.ENHETSNUMMER_GAMMELT] ?: doThrow("Mangler (gammelt)enhetsnummer blant hendelsedata")
    private fun hentNyttEnhetsnummer() = detaljer[Detalj.ENHETSNUMMER_NYTT] ?: doThrow("Mangler${Detalj.ENHETSNUMMER_NYTT} blant hendelsedata")
    private fun doThrow(message: String): String = throw IllegalStateException(message)

    internal fun hentEnhetsnummer() = detaljer[Detalj.ENHETSNUMMER] ?: hentGammeltEnhetsnummer()
    internal fun erBytteTilInterntFagomrade() = detaljer[FAGOMRADE] == FAGOMRADE_BIDRAG || detaljer[FAGOMRADE] == FAGOMRADE_FARSKAP
    internal fun hentIdUtenPrefix() = journalpostId.split('-')[1]
    internal fun hentFagomradeFraId() = journalpostId.split('-')[0]
    internal fun hentOppgaveSokRequestsMedOgUtenPrefix(): Pair<OppgaveSokRequest, OppgaveSokRequest> {
        val fagomradeFraId = hentFagomradeFraId()
        val journalpostIdUtenPrefix = hentIdUtenPrefix()

        return Pair(
            OppgaveSokRequest(journalpostId, fagomradeFraId, this),
            OppgaveSokRequest(journalpostIdUtenPrefix, fagomradeFraId, this)
        )
    }
}

data class JournalforendeEnhet(val gammeltEnhetsnummer: String, val nyttEnhetsnummer: String)
data class Sporingsdata(var correlationId: String? = null, var opprettet: String? = null)
enum class Hendelse {
    AVVIK_ENDRE_FAGOMRADE,
    AVVIK_OVERFOR_TIL_ANNEN_ENHET,
    JOURNALFOR_JOURNALPOST,
    NO_SUPPORT
}
