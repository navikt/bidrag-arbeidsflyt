package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.transport.dokument.Fagomrade
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import no.nav.bidrag.transport.dokument.JournalpostStatus
import org.apache.commons.lang3.Range
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate

val BID_JP_RANGE: Range<Long> = Range.between(18900000L, 40000000L)

fun isBidJournalpostId(jpId: String) = (StringUtils.isNumeric(jpId) && BID_JP_RANGE.contains(jpId.toLong()))

fun JournalpostHendelse.erJournalstatusEndretTilIkkeMottatt() = status != null && !erMottattStatus

val JournalpostHendelse.harSaker get() = sakstilknytninger?.isNotEmpty() == true
val JournalpostHendelse.erMottattStatus get() = status == JournalpostStatus.MOTTATT

fun JournalpostHendelse.hentTema() = fagomrade ?: tema

val JournalpostHendelse.erEksterntFagomrade get() = hentTema() != null && (hentTema() != Fagomrade.BIDRAG && hentTema() != Fagomrade.FARSKAP)
val JournalpostHendelse.harSporingsdataEnhet get() = sporing?.enhetsnummer != null
val JournalpostHendelse.harTittel get() = tittel != null
val JournalpostHendelse.harDokumentDato get() = dokumentDato != null
val JournalpostHendelse.saker get() = sakstilknytninger ?: emptyList()
val JournalpostHendelse.erJournalfort get() = status == JournalpostStatus.JOURNALFØRT
val JournalpostHendelse.journalpostIdUtenPrefix get() = if (harJournalpostIdPrefix()) journalpostId.split('-')[1] else journalpostId
val JournalpostHendelse.journalpostMedBareBIDPrefix get() = if (erBidragJournalpost()) journalpostId else journalpostIdUtenPrefix
val JournalpostHendelse.journalpostMedPrefix get() =
    if (harJournalpostIdPrefix()) {
        journalpostId
    } else if (isBidJournalpostId(journalpostId)) {
        "BID-$journalpostId"
    } else {
        "JOARK-$journalpostId"
    }
val JournalpostHendelse.erJournalfortIdag get(): Boolean = erJournalfort && journalfortDato?.equals(LocalDate.now()) == true
val JournalpostHendelse.hasEnhet get(): Boolean = !enhet.isNullOrEmpty()
