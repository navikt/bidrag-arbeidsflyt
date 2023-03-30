package no.nav.bidrag.arbeidsflyt.utils

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.dto.OppgaveIdentType
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.dto.Oppgavestatuskategori
import no.nav.bidrag.arbeidsflyt.model.EnhetResponse
import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.dokument.dto.HendelseType
import no.nav.bidrag.dokument.dto.JournalpostDto
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import no.nav.bidrag.dokument.dto.JournalpostResponse
import no.nav.bidrag.dokument.dto.Journalstatus
import no.nav.bidrag.dokument.dto.Sporingsdata
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
val DEFAULT_TIME = LocalDateTime.of(2020, 1, 1, 12, 0)

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

var ENHET_4833 = "4833"
var ENHET_4806 = "4806"
fun createOppgave(
    oppgaveId: Long,
    journalpostId: String = JOURNALPOST_ID_1,
    status: String = OppgaveStatus.OPPRETTET.name,
    oppgaveType: String = OPPGAVETYPE_JFR
): Oppgave {
    return Oppgave(
        oppgaveId = oppgaveId,
        journalpostId = journalpostId,
        status = status,
        oppgavetype = oppgaveType
    )
}

fun createDLQKafka(payload: String, topicName: String = "topic_journalpost", retry: Boolean = false, retryCount: Int = 0, messageKey: String = "JOARK-$JOURNALPOST_ID_1", timestamp: LocalDateTime = LocalDateTime.now()): DLQKafka {
    return DLQKafka(
        topicName = topicName,
        messageKey = messageKey,
        payload = payload,
        retry = retry,
        retryCount = retryCount,
        createdTimestamp = timestamp
    )
}

fun createOppgaveHendelse(
    id: Long,
    journalpostId: String? = "123213",
    tildeltEnhetsnr: String? = "4806",
    statuskategori: Oppgavestatuskategori = Oppgavestatuskategori.AAPEN,
    status: OppgaveStatus = OppgaveStatus.OPPRETTET,
    oppgavetype: String = OPPGAVETYPE_JFR,
    tema: String = "BID",
    identVerdi: String = AKTOER_ID,
    beskrivelse: String? = null,
    fnr: String = PERSON_IDENT_1,
    tilordnetRessurs: String? = null,
    identType: OppgaveIdentType = OppgaveIdentType.AKTOERID,
    fristFerdigstillelse: LocalDate? = LocalDate.of(2020, 2, 1)
): OppgaveHendelse {
    val ident = OppgaveHendelse.Ident(verdi = identVerdi, identType = identType, folkeregisterident = if (identType == OppgaveIdentType.AKTOERID) fnr else null)
    return OppgaveHendelse(
        id = id,
        versjon = 1,
        tilordnetRessurs = tilordnetRessurs,
        journalpostId = journalpostId,
        tildeltEnhetsnr = tildeltEnhetsnr,
        status = status,
        oppgavetype = oppgavetype,
        statuskategori = statuskategori,
        tema = tema,
        beskrivelse = beskrivelse,
        ident = ident,
        opprettetTidspunkt = CREATED_TIME,
        endretTidspunkt = CREATED_TIME,
        endretAv = "test",
        fristFerdigstillelse = fristFerdigstillelse
    )
}

fun createJournalpostHendelse(
    journalpostId: String,
    status: String = "M",
    enhet: String? = "4833",
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
        sporing = Sporingsdata("test", enhetsnummer = sporingEnhet, brukerident = "Z994977", saksbehandlersNavn = "Navn Navnesen"),
        hendelseType = HendelseType.ENDRING,
        journalposttype = "I"
    )
}

fun journalpostResponse(journalpostId: String = JOURNALPOST_ID_1, journalStatus: String = Journalstatus.JOURNALFORT, journalforendeEnhet: String = "4833", tema: String = "BID"): JournalpostResponse {
    return JournalpostResponse(
        journalpost = JournalpostDto(
            journalpostId = journalpostId,
            journalstatus = journalStatus,
            journalforendeEnhet = journalforendeEnhet,
            fagomrade = tema
        )
    )
}
fun oppgaveDataResponse(): List<OppgaveData> {
    return listOf(
        OppgaveData(
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
        )
    )
}

fun createJournalforendeEnheterResponse(): List<EnhetResponse> {
    return arrayListOf<EnhetResponse>(
        EnhetResponse("2103", "Nav vikafossen"),
        EnhetResponse("4817", "NAV Familie- og pensjonsytelser Steinkjer"),
        EnhetResponse("4833", "NAV Familie- og pensjonsytelser Oslo 1"),
        EnhetResponse("4806", "NAV Familie- og pensjonsytelser Drammen"),
        EnhetResponse("4812", "NAV Familie- og pensjonsytelser Bergen")
    )
}