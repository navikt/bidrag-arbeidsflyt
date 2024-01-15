package no.nav.bidrag.arbeidsflyt.utils

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.dto.Oppgavestatuskategori
import no.nav.bidrag.arbeidsflyt.hendelse.dto.OppgaveKafkaHendelse
import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.transport.dokument.HendelseType
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.bidrag.transport.dokument.JournalpostStatus
import no.nav.bidrag.transport.dokument.Sporingsdata
import no.nav.bidrag.transport.organisasjon.EnhetDto
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

val SAKSBEHANDLER_ID = "Z994999"

fun createOppgave(
    oppgaveId: Long,
    journalpostId: String = JOURNALPOST_ID_1,
    status: String = OppgaveStatus.OPPRETTET.name,
    oppgaveType: String = OPPGAVETYPE_JFR,
): Oppgave {
    return Oppgave(
        oppgaveId = oppgaveId,
        journalpostId = journalpostId,
        status = status,
        oppgavetype = oppgaveType,
    )
}

fun createDLQKafka(
    payload: String,
    topicName: String = "topic_journalpost",
    retry: Boolean = false,
    retryCount: Int = 0,
    messageKey: String = "JOARK-$JOURNALPOST_ID_1",
    timestamp: LocalDateTime = LocalDateTime.now(),
): DLQKafka {
    return DLQKafka(
        topicName = topicName,
        messageKey = messageKey,
        payload = payload,
        retry = retry,
        retryCount = retryCount,
        createdTimestamp = timestamp,
    )
}

fun createOppgaveData(
    id: Long,
    journalpostId: String? = "123213",
    tildeltEnhetsnr: String? = "4806",
    statuskategori: Oppgavestatuskategori = Oppgavestatuskategori.AAPEN,
    status: OppgaveStatus? = null,
    oppgavetype: String = OPPGAVETYPE_JFR,
    tema: String = "BID",
    aktoerId: String = AKTOER_ID,
    beskrivelse: String? = null,
    tilordnetRessurs: String? = null,
    fristFerdigstillelse: LocalDate? = LocalDate.of(2020, 2, 1),
) = OppgaveData(
    id = id,
    versjon = 1,
    journalpostId = journalpostId,
    tildeltEnhetsnr = tildeltEnhetsnr,
    status =
        status ?: when (statuskategori) {
            Oppgavestatuskategori.AAPEN -> OppgaveStatus.OPPRETTET
            Oppgavestatuskategori.AVSLUTTET -> OppgaveStatus.FERDIGSTILT
        },
    oppgavetype = oppgavetype,
    tema = tema,
    tilordnetRessurs = tilordnetRessurs,
    fristFerdigstillelse = fristFerdigstillelse,
    beskrivelse = beskrivelse,
    aktoerId = aktoerId,
)

fun OppgaveData.toHendelse(type: OppgaveKafkaHendelse.Hendelse.Hendelsestype? = null) =
    OppgaveKafkaHendelse(
        hendelse =
            OppgaveKafkaHendelse.Hendelse(
                type ?: when (status) {
                    OppgaveStatus.FERDIGSTILT -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_FERDIGSTILT
                    OppgaveStatus.OPPRETTET -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_OPPRETTET
                    OppgaveStatus.UNDER_BEHANDLING -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_ENDRET
                    OppgaveStatus.FEILREGISTRERT -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_FEILREGISTRERT
                    else -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_ENDRET
                },
                LocalDateTime.now(),
            ),
        utfortAv = OppgaveKafkaHendelse.UtfortAv(tilordnetRessurs, tildeltEnhetsnr),
        oppgave =
            OppgaveKafkaHendelse.Oppgave(
                id,
                1,
                kategorisering =
                    OppgaveKafkaHendelse.Kategorisering(
                        tema ?: "BID",
                        oppgavetype = oppgavetype ?: "JFR",
                    ),
                bruker =
                    OppgaveKafkaHendelse.Bruker(
                        aktoerId,
                        OppgaveKafkaHendelse.Bruker.IdentType.FOLKEREGISTERIDENT,
                    ),
            ),
    )

fun createJournalpostHendelse(
    journalpostId: String,
    status: JournalpostStatus = JournalpostStatus.MOTTATT,
    enhet: String? = "4833",
    fagomrade: String = "BID",
    aktorId: String = AKTOER_ID,
    sporingEnhet: String = "4833",
): JournalpostHendelse {
    return JournalpostHendelse(
        journalpostId = journalpostId,
        aktorId = aktorId,
        fagomrade = fagomrade,
        enhet = enhet,
        status = status,
        sporing =
            Sporingsdata(
                "test",
                enhetsnummer = sporingEnhet,
                brukerident = SAKSBEHANDLER_ID,
                saksbehandlersNavn = "Navn Navnesen",
            ),
        hendelseType = HendelseType.ENDRING,
        journalposttype = "I",
    )
}

fun journalpostResponse(
    journalpostId: String = JOURNALPOST_ID_1,
    journalStatus: JournalpostStatus = JournalpostStatus.JOURNALFØRT,
    journalforendeEnhet: String = "4833",
    tema: String = "BID",
): JournalpostResponse {
    return JournalpostResponse(
        journalpost =
            JournalpostDto(
                journalpostId = journalpostId,
                status = journalStatus,
                journalforendeEnhet = journalforendeEnhet,
                fagomrade = tema,
            ),
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
            tildeltEnhetsnr = "4833",
        ),
        OppgaveData(
            id = OPPGAVE_ID_2,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_2,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833",
        ),
        OppgaveData(
            id = OPPGAVE_ID_4,
            journalpostId = BID_JOURNALPOST_ID_1,
            versjon = 1,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833",
        ),
        OppgaveData(
            id = OPPGAVE_ID_5,
            versjon = 1,
            journalpostId = BID_JOURNALPOST_ID_2,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833",
        ),
    )
}

fun createJournalforendeEnheterResponse(): List<EnhetDto> {
    return arrayListOf<EnhetDto>(
        EnhetDto(Enhetsnummer("2103"), "Nav vikafossen"),
        EnhetDto(Enhetsnummer("4817"), "NAV Familie- og pensjonsytelser Steinkjer"),
        EnhetDto(Enhetsnummer("4833"), "NAV Familie- og pensjonsytelser Oslo 1"),
        EnhetDto(Enhetsnummer("4806"), "NAV Familie- og pensjonsytelser Drammen"),
        EnhetDto(Enhetsnummer("4812"), "NAV Familie- og pensjonsytelser Bergen"),
    )
}

fun createJournalpost(
    journalpostId: String,
    status: String = "M",
    enhet: String = "4833",
    tema: String = "BID",
): Journalpost {
    return Journalpost(
        journalpostId = journalpostId,
        status = status,
        enhet = enhet,
        tema = tema,
    )
}
