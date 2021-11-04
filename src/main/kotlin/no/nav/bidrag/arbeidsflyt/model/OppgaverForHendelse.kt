package no.nav.bidrag.arbeidsflyt.model

data class OppgaverForHendelse(val dataForHendelse: List<OppgaveDataForHendelse>) {

    fun erEndringAvTildeltEnhetsnummer(journalpostHendelse: JournalpostHendelse): Boolean {
        if (journalpostHendelse.harEnhet()) {
            return dataForHendelse.stream()
                .filter { journalpostHendelse.enhet != it.tildeltEnhetsnr }
                .findAny().isPresent
        }

        return false
    }

    fun erEndringAvAktoerId(journalpostHendelse: JournalpostHendelse): Boolean {
        if (journalpostHendelse.harAktorId()) {
            return dataForHendelse.stream()
                .filter { journalpostHendelse.aktorId != it.aktorId }
                .findAny().isPresent
        }
        return false;
    }

    fun harIkkeJournalforingsoppgaveForJournalpost(journalpostHendelse: JournalpostHendelse): Boolean {
        return dataForHendelse.isEmpty() || dataForHendelse.stream()
            .filter { it.oppgavetype == JOURNALFORINGSOPPGAVE }
            .filter { it.journalpostId == journalpostHendelse.journalpostId}
            .findAny().isEmpty
    }

    fun harJournalforingsoppgaver() = dataForHendelse
        .find { it.oppgavetype == JOURNALFORINGSOPPGAVE } != null
}
