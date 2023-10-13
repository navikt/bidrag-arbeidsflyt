package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveType
import no.nav.bidrag.transport.dokument.JournalpostHendelse

data class OppgaverForHendelse(val dataForHendelse: List<OppgaveData>) {

    fun erEndringAvTildeltEnhetsnummer(journalpostHendelse: JournalpostHendelse): Boolean {
        return journalpostHendelse.harEnhet() && dataForHendelse.stream()
            .filter { journalpostHendelse.enhet != it.tildeltEnhetsnr }
            .findAny().isPresent
    }

    fun erEndringAvAktoerId(journalpostHendelse: JournalpostHendelse): Boolean {
        return journalpostHendelse.harAktorId() && dataForHendelse.stream()
            .filter { journalpostHendelse.aktorId != it.aktoerId }
            .findAny().isPresent
    }

    fun harIkkeJournalforingsoppgave(): Boolean {
        return !harJournalforingsoppgaver()
    }

    fun erJournalforingsoppgaverTildeltSaksbehandler(): Boolean {
        return hentJournalforingsOppgaver().any { !it.tilordnetRessurs.isNullOrEmpty() }
    }
    fun harJournalforingsoppgaver() = dataForHendelse.isNotEmpty() && dataForHendelse
        .stream().anyMatch { it.oppgavetype == JOURNALFORINGSOPPGAVE }

    fun hentJournalforingsOppgaver() = dataForHendelse.filter { it.oppgavetype == JOURNALFORINGSOPPGAVE }

    fun hentBehandleDokumentOppgaverSomSkalOppdateresForNyttDokument(journalpostId: String): List<OppgaveData> {
        val journalpostIdUtenPrefix = journalpostId.replace("BID-", "").replace("JOARK-", "")
        return dataForHendelse.filter { it.oppgavetype == OppgaveType.BEH_SAK.name }
            .filter { it.journalpostId != journalpostIdUtenPrefix && it.journalpostId != "BID-$journalpostId" }
            .filter { it.beskrivelse != null && !it.beskrivelse.contains(journalpostIdUtenPrefix) }
    }

    fun harBehandleDokumentOppgaveForSaker(journalpostId: String, saker: List<String>): Boolean {
        val sakerSomSkalEndres = hentBehandleDokumentOppgaverSomSkalOppdateresForNyttDokument(journalpostId)

        val sakerSomSkalOpprettesNyBehandleDokumentOppgave = hentSakerSomKreverNyBehandleDokumentOppgave(saker)

        return sakerSomSkalEndres.isEmpty() && sakerSomSkalOpprettesNyBehandleDokumentOppgave.isEmpty()
    }

    fun skalOppdatereEllerOppretteBehandleDokumentOppgaver(journalpostId: String, saker: List<String>): Boolean {
        return !harBehandleDokumentOppgaveForSaker(journalpostId, saker)
    }

    fun hentSakerSomKreverNyBehandleDokumentOppgave(saker: List<String>): List<String> {
        val sakerEndretForNyttDokument = dataForHendelse.filter { it.oppgavetype == OppgaveType.BEH_SAK.name }.map { it.saksreferanse }
        return saker.filter { !sakerEndretForNyttDokument.contains(it) }
    }
}
