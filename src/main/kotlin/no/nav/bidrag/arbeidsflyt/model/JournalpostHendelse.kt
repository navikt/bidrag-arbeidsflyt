package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.arbeidsflyt.model.Hendelse.AVVIK_ENDRE_FAGOMRADE
import no.nav.bidrag.arbeidsflyt.model.Hendelse.AVVIK_OVERFOR_TIL_ANNEN_ENHET
import no.nav.bidrag.arbeidsflyt.model.Hendelse.JOURNALFOR_JOURNALPOST
import no.nav.bidrag.arbeidsflyt.model.Hendelse.OPPRETT_OPPGAVE
import no.nav.bidrag.arbeidsflyt.model.Hendelse.values

data class JournalpostHendelse(
    var journalpostId: String = "",
    var hendelse: String = "",
    var sporing: Sporingsdata? = null,
    private var detaljer: Map<String, String> = emptyMap()
) {
    internal fun erBytteTilInterntFagomrade() = detaljer[Detalj.FAGOMRADE_NYTT] == DetaljVerdi.FAGOMRADE_BIDRAG ||
            detaljer[Detalj.FAGOMRADE_NYTT] == DetaljVerdi.FAGOMRADE_FARSKAP

    internal fun hentHendelse() = values().find { it.name == hendelse }
    internal fun hentAktoerId() = detaljer[Detalj.AKTOER_ID]
    internal fun hentEnhetsnummer() = detaljer[Detalj.ENHETSNUMMER] ?: doThrow("Mangler ${Detalj.ENHETSNUMMER} blant hendelsedata")
    internal fun hentFagomradeFraDetaljer() = detaljer[Detalj.FAGOMRADE] ?: hentGammeltFagomradeFraDetaljer()
    internal fun hentGammeltFagomradeFraDetaljer() = detaljer[Detalj.FAGOMRADE_GAMMELT] ?: DetaljVerdi.FAGOMRADE_BIDRAG
    internal fun hentJournalpostIdUtenPrefix() = if (harJournalpostIdPrefix()) journalpostId.split('-')[1] else journalpostId
    internal fun harJournalpostIdPrefix() = journalpostId.contains("-")

    internal fun hentNyttJournalforendeEnhetsnummer() = detaljer[Detalj.ENHETSNUMMER_NYTT] ?: doThrow(
        "Mangler ${Detalj.ENHETSNUMMER_NYTT} blant hendelsedata"
    )

    private fun doThrow(message: String): String = throw IllegalStateException(message)

    internal fun sjekkDetaljerForHendelse() {
        when (hentHendelse()) {
            AVVIK_ENDRE_FAGOMRADE -> sjekkAtDetaljerPaHendelse(Detalj.FAGOMRADE_GAMMELT, Detalj.FAGOMRADE_NYTT, Detalj.ENHETSNUMMER)
            AVVIK_OVERFOR_TIL_ANNEN_ENHET -> sjekkAtDetaljerPaHendelse(Detalj.ENHETSNUMMER_GAMMELT, Detalj.ENHETSNUMMER_NYTT, Detalj.FAGOMRADE)
            OPPRETT_OPPGAVE -> sjekkAtDetaljerPaHendelse(Detalj.FAGOMRADE)
            JOURNALFOR_JOURNALPOST -> sjekkAtDetaljerPaHendelse(Detalj.FAGOMRADE)
            null -> doThrow("Ugyldig hendelse $hendelse!")
        }
    }

    private fun sjekkAtDetaljerPaHendelse(vararg detaljer: String) {
        detaljer.forEach {
            if (!detaljer.contains(it)) {
                throw IllegalStateException("Mangler detalj '$it' på hendelse: $hendelse")
            }
        }
    }
}

data class Sporingsdata(val correlationId: String? = null, val opprettet: String? = null, val saksbehandlersNavn: String? = null)
