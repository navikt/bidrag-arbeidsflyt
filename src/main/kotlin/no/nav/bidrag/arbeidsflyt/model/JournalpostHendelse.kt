package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.dokument.dto.JournalpostHendelse
import no.nav.bidrag.dokument.dto.Journalstatus
import java.time.LocalDate

data class JournalpostHendelseIntern(val journalpostHendelse: JournalpostHendelse) {

    val harTittel get() = journalpostHendelse.tittel != null
    val harDokumentDato get() = journalpostHendelse.dokumentDato != null
    val harSporingsdataEnhet get() = journalpostHendelse.sporing?.enhetsnummer != null
    val erJournalfort get() = Journalstatus.JOURNALFORT == journalpostHendelse.journalstatus
    val harSaker get() = journalpostHendelse.sakstilknytninger?.isNotEmpty() == true
    fun erJournalstatusEndretTilIkkeMottatt() = journalpostHendelse.journalstatus != null && !journalpostHendelse.erMottattStatus

    val saker get() = journalpostHendelse.sakstilknytninger ?: emptyList()
    val journalpostId get() = journalpostHendelse.journalpostId

    val erJournalfortIdag get(): Boolean = erJournalfort && journalpostHendelse.journalfortDato?.equals(LocalDate.now()) == true
}