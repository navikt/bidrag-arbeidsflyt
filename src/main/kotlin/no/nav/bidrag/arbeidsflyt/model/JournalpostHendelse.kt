package no.nav.bidrag.arbeidsflyt.model

data class JournalpostHendelse(
    var journalpostId: String = "na",
    var bnr: String? = null,
    var aktorId: String? = null,
    var fnr: String? = null,
    var fagomrade: String? = null,
    var enhet: String? = null,
    var journalstatus: String? = null,
    var sporing: Sporingsdata? = null
) {
    private val erMottattStatus get() = Journalstatus.MOTTATT == journalstatus

    internal val erEksterntFagomrade get() = fagomrade != null && (fagomrade != Fagomrade.BIDRAG && fagomrade != Fagomrade.FARSKAP)
    internal val erMottaksregistrert get() = erMottattStatus
    internal val journalpostIdUtenPrefix get() = if (harJournalpostIdPrefix()) journalpostId.split('-')[1] else journalpostId
    internal val journalpostMedBareBIDprefix get() = if (harJournalpostIdBIDPrefix()) journalpostId else journalpostIdUtenPrefix

    internal fun erJournalstatusEndretTilIkkeMottatt() = journalstatus != null && !erMottattStatus
    internal fun harEnhet() = enhet != null
    internal fun harAktorId() = aktorId != null
    internal fun harBnr() = bnr != null
    internal fun harJournalpostIdPrefix() = journalpostId.contains("-")
    internal fun harJournalpostIdBIDPrefix() = harJournalpostIdPrefix() && journalpostId.startsWith("BID")
    internal fun erJoarkJournalpost() = harJournalpostIdPrefix() && journalpostId.startsWith("JOARK")
    internal fun hentEndretAvEnhetsnummer() = if (sporing?.enhetsnummer != null) sporing!!.enhetsnummer else enhet
    internal fun hentSaksbehandlerInfo() = if (sporing != null) sporing!!.lagSaksbehandlerInfo() else "ukjent saksbehandler"

    fun printSummary() = "{journalpostId=$journalpostId,fagomrade=$fagomrade,enhet=$enhet,saksbehandlerEnhet=${sporing?.enhetsnummer},journalstatus=$journalstatus....}"
}

data class Sporingsdata(
    var correlationId: String? = null,
    var brukerident: String? = null,
    var saksbehandlersNavn: String? = null,
    var enhetsnummer: String? = null
) {
    internal fun lagSaksbehandlerInfo() = if (brukerident == null) "ukjent saksbehandler" else hentBrukeridentMedSaksbehandler()
    private fun hentBrukeridentMedSaksbehandler() = if (saksbehandlersNavn == null) brukerident!! else "$brukerident, $saksbehandlersNavn"
}
