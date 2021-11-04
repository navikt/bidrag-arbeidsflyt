package no.nav.bidrag.arbeidsflyt.model

data class JournalpostHendelse(
    var journalpostId: String = "na",
    var aktorId: String? = null,
    var fagomrade: String? = null,
    var enhet: String? = null,
    var journalstatus: String? = null,
    var sporing: Sporingsdata? = null
) {
    private val erMottattStatus = Journalstatus.MOTTATT == journalstatus

    internal val erEksterntFagomrade = fagomrade != null && (fagomrade != Fagomrade.BIDRAG && fagomrade != Fagomrade.FARSKAP)
    internal val erMottaksregistrert = erMottattStatus

    internal fun erJournalstatusEndretTilIkkeMottatt() = journalstatus != null && !erMottattStatus
    internal fun harEnhet() = enhet != null
    internal fun harAktorId() = aktorId != null
    internal fun hentJournalpostIdUtenPrefix() = if (harJournalpostIdPrefix()) journalpostId.split('-')[1] else journalpostId
    internal fun harJournalpostIdPrefix() = journalpostId.contains("-")
    internal fun hentSaksbehandlerInfo() = if (sporing != null) sporing!!.lagSaksbehandlerInfo() else "ukjent saksbehandler"
}

data class Sporingsdata(
    var correlationId: String? = null,
    var brukerident: String? = null,
    var saksbehandlersNavn: String? = null
) {
    internal fun lagSaksbehandlerInfo() = if (brukerident == null) "ukjent saksbehandler" else hentBrukeridentMedSaksbehandler()
    private fun hentBrukeridentMedSaksbehandler() = if (saksbehandlersNavn == null) brukerident!! else "$brukerident, $saksbehandlersNavn"
}
