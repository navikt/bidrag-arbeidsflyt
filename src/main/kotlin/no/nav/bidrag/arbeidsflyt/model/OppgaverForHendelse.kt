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

    fun harIkkeJournalforingsoppgave(): Boolean {
        return !harJournalforingsoppgaver()
    }

    fun harJournalforingsoppgaver() = dataForHendelse.isNotEmpty() && dataForHendelse
        .stream().anyMatch{ it.oppgavetype == JOURNALFORINGSOPPGAVE }

    fun hentJournalforingsOppgaver() = dataForHendelse.filter { it.oppgavetype == JOURNALFORINGSOPPGAVE }
}
