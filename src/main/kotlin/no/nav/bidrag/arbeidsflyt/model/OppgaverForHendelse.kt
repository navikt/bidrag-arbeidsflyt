package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.arbeidsflyt.dto.OppgaveType
import no.nav.bidrag.dokument.dto.JournalpostHendelse

data class OppgaverForHendelse(val dataForHendelse: List<OppgaveDataForHendelse>) {

    fun erEndringAvTildeltEnhetsnummer(journalpostHendelse: JournalpostHendelse): Boolean {
        return journalpostHendelse.harEnhet() && dataForHendelse.stream()
            .filter { journalpostHendelse.enhet != it.tildeltEnhetsnr }
            .findAny().isPresent
    }

    fun erEndringAvAktoerId(journalpostHendelse: JournalpostHendelse): Boolean {
        return journalpostHendelse.harAktorId() && dataForHendelse.stream()
            .filter { journalpostHendelse.aktorId != it.aktorId }
            .findAny().isPresent
    }

    fun harIkkeJournalforingsoppgave(): Boolean {
        return !harJournalforingsoppgaver()
    }

    fun harJournalforingsoppgaver() = dataForHendelse.isNotEmpty() && dataForHendelse
        .stream().anyMatch{ it.oppgavetype == JOURNALFORINGSOPPGAVE }

    fun hentJournalforingsOppgaver() = dataForHendelse.filter { it.oppgavetype == JOURNALFORINGSOPPGAVE }

    fun hentBehandleDokumentOppgaverSomSkalOppdateresForNyttDokument(journalpostId: String): List<OppgaveDataForHendelse>{
        return dataForHendelse.filter { it.oppgavetype == OppgaveType.BEH_SAK.name }
            .filter { it.journalpostId != journalpostId }
            .filter { it.beskrivelse?.contains(journalpostId) == false }
    }

    fun harBehandleDokumentOppgaveForSaker(journalpostId: String, saker: List<String>): Boolean {
        val sakerSomSkalEndres = hentBehandleDokumentOppgaverSomSkalOppdateresForNyttDokument(journalpostId)

        val sakerSomSkalOpprettesNyBehandleDokumentOppgave = hentSakerSomKreverNyBehandleDokumentOppgave(saker)

        return sakerSomSkalEndres.isEmpty() && sakerSomSkalOpprettesNyBehandleDokumentOppgave.isEmpty()
    }

    fun hentSakerSomKreverNyBehandleDokumentOppgave(saker: List<String>): List<String>{
        val sakerEndretForNyttDokument = dataForHendelse.filter { it.oppgavetype == OppgaveType.BEH_SAK.name }.map { it.saksreferanse }
        return saker.filter { !sakerEndretForNyttDokument.contains(it) }
    }
}
