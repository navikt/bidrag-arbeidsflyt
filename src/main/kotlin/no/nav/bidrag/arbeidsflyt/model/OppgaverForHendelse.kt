package no.nav.bidrag.arbeidsflyt.model

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

    fun harIkkeJournalforingsoppgaveForJournalpost(journalpostHendelse: JournalpostHendelse): Boolean {
        return dataForHendelse.isEmpty() || dataForHendelse.stream()
            .filter { it.oppgavetype == JOURNALFORINGSOPPGAVE }
            .filter { it.journalpostId == journalpostHendelse.hentJournalpostIdUtenPrefix() || it.journalpostId == journalpostHendelse.journalpostId }
            .findAny().isEmpty
    }

    fun harJournalforingsoppgaver() = dataForHendelse
        .find { it.oppgavetype == JOURNALFORINGSOPPGAVE } != null

    fun hentJournalforingsOppgaver() = dataForHendelse.filter { it.oppgavetype == JOURNALFORINGSOPPGAVE }
}
