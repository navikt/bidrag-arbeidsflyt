package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.model.JP_ADM_ENHETSNUMMER

data class JournalpostHendelse(
    var journalpostId: String = "",
    var hendelse: String = "",
    var sporing: Sporingsdata? = null,
    var detaljer: Map<String, String> = emptyMap()
) {
    fun hentHendelse() = JournalpostHendelser.valueOf(hendelse)
    internal fun hentIdUtenPrefix() = journalpostId.split('-')[1]
    internal fun hentFagomradeFraId() = journalpostId.split('-')[0]
    fun hentOppgaveSokRequestsMedOgUtenPrefix() = Pair(
        OppgaveSokRequest(journalpostId, hentFagomradeFraId(), detaljer[JP_ADM_ENHETSNUMMER]),
        OppgaveSokRequest(hentIdUtenPrefix(), hentFagomradeFraId(), detaljer[JP_ADM_ENHETSNUMMER])
    )
}

data class Sporingsdata(var correlationId: String? = null, var opprettet: String? = null)
enum class JournalpostHendelser {
    JOURNALFOR_JOURNALPOST
}
