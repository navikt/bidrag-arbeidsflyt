package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.model.Detalj.FAGOMRADE
import no.nav.bidrag.arbeidsflyt.model.DetaljVerdi.FAGOMRADE_BIDRAG
import no.nav.bidrag.arbeidsflyt.model.DetaljVerdi.FAGOMRADE_FARSKAP

data class JournalpostHendelse(
    var journalpostId: String = "",
    var hendelse: String = "",
    var sporing: Sporingsdata? = null,
    var detaljer: Map<String, String> = emptyMap()
) {
    fun hentHendelse(): Hendelse {
        return Hendelse.values().find { it.name == hendelse } ?: Hendelse.NO_SUPPORT
    }

    internal fun erBytteTilInterntFagomrade() = detaljer[FAGOMRADE] == FAGOMRADE_BIDRAG || detaljer[FAGOMRADE] == FAGOMRADE_FARSKAP
    internal fun hentIdUtenPrefix() = journalpostId.split('-')[1]
    internal fun hentFagomradeFraId() = journalpostId.split('-')[0]
    internal fun hentOppgaveSokRequestsMedOgUtenPrefix(): Pair<OppgaveSokRequest, OppgaveSokRequest> {
        val fagomradeFraId = hentFagomradeFraId()
        val journalpostIdUtenPrefix = hentIdUtenPrefix()

        return Pair(
            OppgaveSokRequest(journalpostId, fagomradeFraId, detaljer),
            OppgaveSokRequest(journalpostIdUtenPrefix, fagomradeFraId, detaljer)
        )
    }
}

data class Sporingsdata(var correlationId: String? = null, var opprettet: String? = null)
enum class Hendelse {
    AVVIK_ENDRE_FAGOMRADE,
    AVVIK_OVERFOR_TIL_ANNEN_ENHET,
    JOURNALFOR_JOURNALPOST,
    NO_SUPPORT
}
