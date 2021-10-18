package no.nav.bidrag.arbeidsflyt.model

private const val JOURNALFORINGSOPPGAVE = "JFR"

data class OppgaverForHendelse(val dataForHendelse: List<OppgaveDataForHendelse>) {

    fun erEndringAvTildeltEnhetsnummer(journalpostHendelse: JournalpostHendelse): Boolean {
        if (journalpostHendelse.harEnhet()) {
            return dataForHendelse.stream()
                .filter { journalpostHendelse.enhet != it.tildeltEnhetsnr }
                .findAny().isPresent
        }

        return false
    }

    fun harIkkeJournalforingsoppgaveForAktor(journalpostHendelse: JournalpostHendelse): Boolean {
        if (journalpostHendelse.aktorId != null) {
            return dataForHendelse.isEmpty() || dataForHendelse.stream()
                .filter { it.oppgavetype == JOURNALFORINGSOPPGAVE }
                .filter { it.aktorId == journalpostHendelse.aktorId }
                .findAny().isEmpty
        }

        return false
    }

    fun harJournalforingsoppgaver() = dataForHendelse
        .find { it.oppgavetype == JOURNALFORINGSOPPGAVE } != null
}
