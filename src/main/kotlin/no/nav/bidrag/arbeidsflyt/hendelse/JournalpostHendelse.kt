package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveSokRequest

data class JournalpostHendelse(
    var journalpostId: String = "",
    var hendelse: String = "",
    var sporing: Sporingsdata? = null
) {
    fun hentHendelse() = JournalpostHendelser.valueOf(hendelse)
    internal fun hentIdUtenPrefix() = journalpostId.split('-')[1]
    internal fun hentFagomradeFraId() = journalpostId.split('-')[0]
    fun hentOppgaveSokRequestsMedOgUtenPrefix() = Pair(
        OppgaveSokRequest(journalpostId, hentFagomradeFraId()),
        OppgaveSokRequest(hentIdUtenPrefix(), hentFagomradeFraId())
    )
}

data class Sporingsdata(var correlationId: String? = null, var opprettet: String? = null)
enum class JournalpostHendelser {
    JOURNALFOR_JOURNALPOST
}
