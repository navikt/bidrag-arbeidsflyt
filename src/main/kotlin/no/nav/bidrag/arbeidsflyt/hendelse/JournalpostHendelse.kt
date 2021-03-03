package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.model.Detalj.FAGOMRADE
import no.nav.bidrag.arbeidsflyt.model.Detalj.JP_ADM_ENHETSNUMMER
import no.nav.bidrag.arbeidsflyt.model.DetaljVerdi.FAGOMRADE_BIDRAG
import no.nav.bidrag.arbeidsflyt.model.DetaljVerdi.FAGOMRADE_FARSKAP

data class JournalpostHendelse(
    var journalpostId: String = "",
    var hendelse: String = "",
    var sporing: Sporingsdata? = null,
    var detaljer: Map<String, String> = emptyMap()
) {
    fun hentHendelse(): JournalpostHendelser {
        for (journalpostHendelse in JournalpostHendelser.values()) {
            if (hendelse == journalpostHendelse.name) {
                return JournalpostHendelser.valueOf(hendelse)
            }
        }

        return JournalpostHendelser.NO_SUPPORT
    }

    internal fun erBytteTilInterntFagomrade() = detaljer[FAGOMRADE] == FAGOMRADE_BIDRAG || detaljer[FAGOMRADE] == FAGOMRADE_FARSKAP
    internal fun hentIdUtenPrefix() = journalpostId.split('-')[1]
    internal fun hentFagomradeFraId() = journalpostId.split('-')[0]
    internal fun hentOppgaveSokRequestsMedOgUtenPrefix() = Pair(
        OppgaveSokRequest(journalpostId, hentFagomradeFraId(), detaljer[JP_ADM_ENHETSNUMMER]),
        OppgaveSokRequest(hentIdUtenPrefix(), hentFagomradeFraId(), detaljer[JP_ADM_ENHETSNUMMER])
    )
}

data class Sporingsdata(var correlationId: String? = null, var opprettet: String? = null)
enum class JournalpostHendelser {
    AVVIK_ENDRE_FAGOMRADE,
    JOURNALFOR_JOURNALPOST,
    NO_SUPPORT
}
