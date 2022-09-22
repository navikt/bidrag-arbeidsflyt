package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.dokument.dto.Fagomrade
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import no.nav.bidrag.dokument.dto.Journalstatus
import java.time.LocalDate
fun JournalpostHendelse.erJournalstatusEndretTilIkkeMottatt() = journalstatus != null && !erMottattStatus
val JournalpostHendelse.harSaker get() = sakstilknytninger?.isNotEmpty() == true
val JournalpostHendelse.erMottattStatus get() = Journalstatus.MOTTATT == journalstatus
val JournalpostHendelse.erEksterntFagomrade get() = fagomrade != null && (fagomrade != Fagomrade.BIDRAG && fagomrade != Fagomrade.FARSKAP)
val JournalpostHendelse.harSporingsdataEnhet get() = sporing?.enhetsnummer != null
val JournalpostHendelse.harTittel get() = tittel != null
val JournalpostHendelse.harDokumentDato get() = dokumentDato != null
val JournalpostHendelse.saker get() = sakstilknytninger ?: emptyList()
val JournalpostHendelse.erJournalfort get() = Journalstatus.JOURNALFORT == journalstatus
val JournalpostHendelse.journalpostIdUtenPrefix get() = if (harJournalpostIdPrefix()) journalpostId.split('-')[1] else journalpostId
val JournalpostHendelse.journalpostMedBareBIDPrefix get() = if (erBidragJournalpost()) journalpostId else journalpostIdUtenPrefix
val JournalpostHendelse.erJournalfortIdag get(): Boolean = erJournalfort && journalfortDato?.equals(LocalDate.now()) == true
