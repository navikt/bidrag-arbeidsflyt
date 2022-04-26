package no.nav.bidrag.arbeidsflyt.utils

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.dto.OppgaveIdentType
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.dto.Oppgavestatuskategori
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.model.Sporingsdata
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import java.time.ZonedDateTime

var JOURNALPOST_ID_1 = "124123"
var JOURNALPOST_ID_2 = "142312"
var JOURNALPOST_ID_3 = "5125125"
var JOURNALPOST_ID_4_NEW = "6125125"
var BID_JOURNALPOST_ID_1 = "BID-8125125"
var BID_JOURNALPOST_ID_2 = "BID-9125125"
var BID_JOURNALPOST_ID_3_NEW = "BID-19125125"

var PERSON_IDENT_1 = "12345678910"
var PERSON_IDENT_2 = "22345678910"
var PERSON_IDENT_3 = "32345678910"

var OPPGAVE_ID_1 = 1L
var OPPGAVE_ID_2 = 2L
var OPPGAVE_ID_3 = 3L
var OPPGAVE_ID_4 = 4L
var OPPGAVE_ID_5 = 5L

var AKTOER_ID = "55345678910"
var BNR = "12321331233"
var OPPGAVETYPE_JFR = "JFR"
var OPPGAVETYPE_BEH_SAK = "BEH_SAK"
var CREATED_TIME = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]")


fun createOppgave(oppgaveId: Long,
                  journalpostId: String = JOURNALPOST_ID_1,
                  status: String = OppgaveStatus.OPPRETTET.name,
                  oppgaveType: String = OPPGAVETYPE_JFR,
                  tema: String = "BID"
): Oppgave {
    return Oppgave(
        oppgaveId = oppgaveId,
        journalpostId = journalpostId,
        status = status,
        tema = tema,
        oppgavetype = oppgaveType,
        statuskategori = "AAPEN"
    )
}

fun  createJournalpost(journalpostId: String, status: String = "M", enhet: String = "4833", gjelderId: String? = PERSON_IDENT_1, tema: String = "BID"): Journalpost {
    return Journalpost(
        journalpostId = journalpostId,
        status = status,
        enhet = enhet,
        gjelderId = gjelderId,
        tema = tema
    )
}

fun createOppgaveHendelse(
    id: Long,
    journalpostId: String = "123213",
    tildeltEnhetsnr: String = "9999",
    statuskategori: Oppgavestatuskategori = Oppgavestatuskategori.AAPEN,
    status: OppgaveStatus = OppgaveStatus.OPPRETTET,
    oppgavetype: String = OPPGAVETYPE_JFR,
    tema: String = "BID",
    identVerdi: String = AKTOER_ID,
    fnr: String = PERSON_IDENT_1,
    identType: OppgaveIdentType = OppgaveIdentType.AKTOERID
): OppgaveHendelse {
    val ident = OppgaveHendelse.Ident(verdi = identVerdi, identType = identType, folkeregisterident = if (identType == OppgaveIdentType.AKTOERID) fnr else null)
    return OppgaveHendelse(
        id = id,
        versjon = 1,
        journalpostId = journalpostId,
        tildeltEnhetsnr = tildeltEnhetsnr,
        status = status,
        oppgavetype = oppgavetype,
        statuskategori = statuskategori,
        tema = tema,
        ident = ident,
        opprettetTidspunkt = CREATED_TIME,
        endretTidspunkt = CREATED_TIME,
        endretAv = "test"
    )
}

fun createJournalpostHendelse(
    journalpostId: String,
    status: String = "M",
    enhet: String = "4833",
    fagomrade: String = "BID",
    aktorId: String = AKTOER_ID,
    sporingEnhet: String = "4833"
): JournalpostHendelse {
    return JournalpostHendelse(
        journalpostId = journalpostId,
        aktorId = aktorId,
        fagomrade = fagomrade,
        enhet = enhet,
        journalstatus = status,
        sporing = Sporingsdata("test", enhetsnummer = sporingEnhet)
    )
}

fun oppgaveDataResponse(): List<OppgaveData> {
    return listOf(  OppgaveData(
        id = OPPGAVE_ID_1,
        versjon = 1,
        journalpostId = JOURNALPOST_ID_1,
        aktoerId = AKTOER_ID,
        oppgavetype = "JFR",
        tema = "BID",
        tildeltEnhetsnr = "4833"
    ),
        OppgaveData(
            id = OPPGAVE_ID_2,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_2,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        ),
        OppgaveData(
            id = OPPGAVE_ID_4,
            journalpostId = BID_JOURNALPOST_ID_1,
            versjon = 1,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        ),
        OppgaveData(
            id = OPPGAVE_ID_5,
            versjon = 1,
            journalpostId = BID_JOURNALPOST_ID_2,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        ))
}