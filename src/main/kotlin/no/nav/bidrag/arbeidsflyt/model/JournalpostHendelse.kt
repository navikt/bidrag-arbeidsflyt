package no.nav.bidrag.arbeidsflyt.model

data class JournalpostHendelse(
    var journalpostId: String = "na",
    var bnr: String? = null,
    var aktorId: String? = null,
    var fagomrade: String? = null,
    var enhet: String? = null,
    var journalstatus: String? = null,
    var sporing: Sporingsdata? = null
) {
    private val erMottattStatus get() = Journalstatus.MOTTATT == journalstatus

    internal val erEksterntFagomrade get() = fagomrade != null && (fagomrade != Fagomrade.BIDRAG && fagomrade != Fagomrade.FARSKAP)
    internal val erMottaksregistrert get() = erMottattStatus

    internal fun erJournalstatusEndretTilIkkeMottatt() = journalstatus != null && !erMottattStatus
    internal fun harEnhet() = enhet != null
    internal fun harAktorId() = aktorId != null
    internal fun harBnr() = bnr != null
    internal fun hentJournalpostIdUtenPrefix() = if (harJournalpostIdBIDPrefix()) journalpostId.split('-')[1] else journalpostId
    internal fun harJournalpostIdBIDPrefix() = journalpostId.contains("-") && journalpostId.startsWith("BID")
    internal fun hentEndretAvEnhetsnummer() = if (sporing?.enhetsnummer != null) sporing!!.enhetsnummer else enhet
    internal fun hentSaksbehandlerInfo() = if (sporing != null) sporing!!.lagSaksbehandlerInfo() else "ukjent saksbehandler"
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
