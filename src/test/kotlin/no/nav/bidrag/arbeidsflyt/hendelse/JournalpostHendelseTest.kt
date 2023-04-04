package no.nav.bidrag.arbeidsflyt.hendelse

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.formatterDatoForOppgave
import no.nav.bidrag.arbeidsflyt.model.ENHET_FARSKAP
import no.nav.bidrag.arbeidsflyt.model.HentArbeidsfordelingFeiletTekniskException
import no.nav.bidrag.arbeidsflyt.model.journalpostIdUtenPrefix
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.utils.AKTOER_ID
import no.nav.bidrag.arbeidsflyt.utils.BID_JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.BID_JOURNALPOST_ID_3_NEW
import no.nav.bidrag.arbeidsflyt.utils.DateUtils
import no.nav.bidrag.arbeidsflyt.utils.ENHET_4806
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_2
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_4_NEW
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_1
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_2
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_5
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_3
import no.nav.bidrag.arbeidsflyt.utils.SAKSBEHANDLER_ID
import no.nav.bidrag.arbeidsflyt.utils.createJournalpost
import no.nav.bidrag.arbeidsflyt.utils.createJournalpostHendelse
import no.nav.bidrag.dokument.dto.HendelseType
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import no.nav.bidrag.dokument.dto.Sporingsdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDate

internal class JournalpostHendelseTest : AbstractBehandleHendelseTest() {

    @Autowired
    lateinit var behandleHendelseService: BehandleHendelseService

    @Test
    fun `skal hente journalpostId fra prefikset journalpostId`() {
        val journalpostHendelse = JournalpostHendelse(journalpostId = "BID-101")

        assertThat(journalpostHendelse.journalpostIdUtenPrefix).isEqualTo("101")
    }

    @Test
    fun `skal behandle hendelse med ugyldig fnr`() {
        val enhet = "4812"
        val aktorid = "123213123213213"
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3, aktorId = aktorid)
        stubHentGeografiskEnhet(enhet)
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW)
            .copy(
                aktorId = null,
                enhet = null,
                fnr = "123213!!!??+++ 444"
            )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveOpprettetWith()
        verifyOppgaveNotEndret()
        verifyHentGeografiskEnhetKalt()
        verifyHentPersonKaltMedFnr("123213444")
    }

    @Test
    fun `skal opprett oppgave med arbeidsfordeling enhet hvis journalforendenhet er nedlagt`() {
        val enhet = "2101"
        val geografiskEnhet = "4806"
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        stubHentGeografiskEnhet(geografiskEnhet)
        stubHentEnhet(enhet, true)
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW, enhet = enhet)

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveOpprettetWith("\"tildeltEnhetsnr\":\"$geografiskEnhet\"")
        verifyOppgaveNotEndret()
    }

    @Test
    fun `skal opprett oppgave med arbeidsfordeling enhet hvis journalforendenhet ikke finnes`() {
        val enhet = "2101"
        val geografiskEnhet = "4806"
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        stubHentGeografiskEnhet(geografiskEnhet)
        stubHentEnhet(enhet, status = HttpStatus.NO_CONTENT)
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW, enhet = enhet)

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveOpprettetWith("\"tildeltEnhetsnr\":\"$geografiskEnhet\"")
        verifyOppgaveNotEndret()
    }

    @Test
    fun `skal opprette journalforingsoppgave med geografisk enhet og aktorid for mottatt journalpost`() {
        val enhet = "4812"
        val aktorid = "123213123213213"
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3, aktorId = aktorid)
        stubHentGeografiskEnhet(enhet)
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW)
            .copy(
                aktorId = null,
                enhet = null,
                fnr = "123213"
            )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveOpprettetWith("\"aktoerId\":\"$aktorid\"", "\"tildeltEnhetsnr\":\"$enhet\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${BID_JOURNALPOST_ID_3_NEW}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
        verifyHentGeografiskEnhetKalt()
        verifyHentPersonKalt()
    }

    @Test
    fun `skal ikke opprette journalforingsoppgave hvis hent geografisk enhet feiler`() {
        val enhet = "4812"
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        stubHentGeografiskEnhet(enhet, HttpStatus.INTERNAL_SERVER_ERROR)
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW).copy(enhet = null)

        assertThrows<HentArbeidsfordelingFeiletTekniskException> { behandleHendelseService.behandleHendelse(journalpostHendelse) }

        verifyOppgaveNotOpprettet()
        verifyOppgaveNotEndret()
        verifyHentGeografiskEnhetKalt()
    }

    @Test
    fun `skal opprette journalforingsoppgave med BID prefix nar journalpost med status mottatt ikke har jfr oppgave og enhet er null`() {
        val enhet = "4812"
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        stubHentGeografiskEnhet(enhet)
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW).copy(enhet = null)

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveOpprettetWith("\"tildeltEnhetsnr\":\"$enhet\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${BID_JOURNALPOST_ID_3_NEW}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
    }

    @Test
    fun `skal opprette journalforingsoppgave med journalpost enhet hvis BID journalpost`() {
        val enhet = "4833"
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        stubHentJournalforendeEnheter()
        stubHentGeografiskEnhet("1111")
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW).copy(enhet = enhet)

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveOpprettetWith("\"tildeltEnhetsnr\":\"$enhet\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${BID_JOURNALPOST_ID_3_NEW}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
        verifyHentGeografiskEnhetKalt(0)
    }

    @Test
    fun `skal opprette journalforingsoppgave med journalpost enhet hvis Joark journalpost`() {
        val enhet = "4833"
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        stubHentJournalforendeEnheter()
        stubHentGeografiskEnhet("1111")
        stubHentEnhet()
        val journalpostHendelse = createJournalpostHendelse("JOARK-$JOURNALPOST_ID_4_NEW", enhet = enhet)

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveOpprettetWith("\"tildeltEnhetsnr\":\"$enhet\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${JOURNALPOST_ID_4_NEW}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
        verifyHentGeografiskEnhetKalt(0)
    }

    @Test
    fun `skal opprette journalforingsoppgave med arbeidsfordeling hvis hendelse enhet ikke er journalførende`() {
        val enhet = "4343"
        val arbeisfordelingEnhet = "4833"
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        stubHentJournalforendeEnheter()
        stubHentGeografiskEnhet(arbeisfordelingEnhet)
        stubHentEnhet()
        val journalpostHendelse = createJournalpostHendelse("JOARK-$JOURNALPOST_ID_4_NEW", enhet = enhet)

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveOpprettetWith("\"tildeltEnhetsnr\":\"$arbeisfordelingEnhet\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${JOURNALPOST_ID_4_NEW}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
        verifyHentGeografiskEnhetKalt(1)
    }

    @Test
    fun `skal opprette oppgave med uten prefix nar Joark journalpost med status mottatt ikke har jfr oppgave`() {
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        stubHentGeografiskEnhet()
        stubHentEnhet()
        stubHentJournalforendeEnheter()
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix, enhet = ENHET_4806)

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveOpprettetWith(
            "\"tildeltEnhetsnr\":\"$ENHET_4806\"",
            "\"fristFerdigstillelse\":\"${formatterDatoForOppgave(DateUtils.finnNesteArbeidsdag())}\"",
            "\"oppgavetype\":\"JFR\"",
            "\"journalpostId\":\"${JOURNALPOST_ID_4_NEW}\"",
            "\"opprettetAvEnhetsnr\":\"9999\"",
            "\"prioritet\":\"HOY\"",
            "\"tema\":\"BID\""
        )
        verifyOppgaveNotEndret()
    }

    @Test
    fun `skal ikke opprette journalforingsoppgave nar Bidrag journalpost med status mottatt har jfr oppgave`() {
        stubHentOppgave(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    journalpostId = "BID-$JOURNALPOST_ID_4_NEW",
                    aktoerId = AKTOER_ID,
                    oppgavetype = "JFR",
                    tema = "BID",
                    tildeltEnhetsnr = "4833"
                )
            )
        )
        stubHentPerson(PERSON_IDENT_3)
        stubHentGeografiskEnhet()
        val journalpostIdMedJoarkPrefix = "BID-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveNotOpprettet()
        verifyOppgaveNotEndret()
    }

    @Test
    fun `skal ikke opprette oppgave nar Joark journalpost med status mottatt har jfr oppgave`() {
        stubHentOppgave(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    journalpostId = JOURNALPOST_ID_4_NEW,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "JFR",
                    tema = "BID",
                    tildeltEnhetsnr = "4833"
                )
            )
        )
        stubHentPerson(PERSON_IDENT_3)
        stubHentGeografiskEnhet()
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveNotOpprettet()
        verifyOppgaveNotEndret()
    }

    @Test
    fun `skal opprette oppgave på enhet 4860 hvis journalpost har tema FAR`() {
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        stubHentGeografiskEnhet()
        stubHentEnhet()
        stubHentJournalforendeEnheter()
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix, enhet = ENHET_4806).copy(fagomrade = "FAR")

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveOpprettetWith(
            "\"tildeltEnhetsnr\":\"$ENHET_FARSKAP\"",
            "\"fristFerdigstillelse\":\"${formatterDatoForOppgave(DateUtils.finnNesteArbeidsdag())}\"",
            "\"oppgavetype\":\"JFR\"",
            "\"journalpostId\":\"${JOURNALPOST_ID_4_NEW}\"",
            "\"opprettetAvEnhetsnr\":\"9999\"",
            "\"prioritet\":\"HOY\"",
            "\"tema\":\"BID\""
        )
        verifyOppgaveNotEndret()
    }

    @Test
    fun `skal ferdigstille oppgave nar endret til ekstern fagomrade`() {
        stubHentOppgave(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    journalpostId = JOURNALPOST_ID_4_NEW,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "JFR",
                    tema = "BID",
                    tildeltEnhetsnr = "4833"
                )
            )
        )
        stubHentPerson(PERSON_IDENT_3)
        val journalpostIdMedJoarkPrefix = "BID-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix).copy(fagomrade = "EKSTERN")

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveEndretWith(1, "\"status\":\"FERDIGSTILT\"", "\"endretAvEnhetsnr\":\"4833\"")
        verifyOppgaveEndretWith(0, "\"tema\":\"EKSTERN\"", "\"endretAvEnhetsnr\":\"4833\"")
    }

    @Test
    fun `skal ferdigstille oppgave nar endret til ekstern fagomrade for Joark journalpost men JFR oppgave finnes fra for`() {
        stubHentOppgaveContaining(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    journalpostId = JOURNALPOST_ID_4_NEW,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "JFR",
                    tema = "BID",
                    tildeltEnhetsnr = "4833"
                )
            ),
            Pair("tema", "BID")
        )
        stubHentOppgaveContaining(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    journalpostId = JOURNALPOST_ID_4_NEW,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "JFR",
                    tema = "EKSTERN",
                    tildeltEnhetsnr = "4833"
                )
            ),
            Pair("tema", "EKSTERN")
        )
        stubHentPerson(PERSON_IDENT_3)
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix).copy(fagomrade = "EKSTERN")

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveEndretWith(1, "\"status\":\"FERDIGSTILT\"", "\"endretAvEnhetsnr\":\"4833\"")
        verifyOppgaveEndretWith(0, "\"tema\":\"EKSTERN\"", "\"endretAvEnhetsnr\":\"4833\"")
    }

    @Test
    fun `skal hente aktorid hvis mangler`() {
        val aktorId = "123213213213123"
        stubHentOppgaveContaining(listOf())
        stubHentPerson(aktorId = aktorId)
        stubHentEnhet()
        stubHentGeografiskEnhet(enhet = "1234")
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix, enhet = null)
            .copy(
                aktorId = null,
                fnr = "123123123"
            )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt()
        verifyHentGeografiskEnhetKalt()
        verifyOppgaveOpprettetWith("\"aktoerId\":\"$aktorId\"")
    }

    @Test
    fun `skal opprette oppgave med aktorid null hvis personid ikke funnet`() {
        stubHentOppgaveContaining(listOf())
        stubHentPerson(status = HttpStatus.NO_CONTENT)
        stubHentGeografiskEnhet(enhet = "1234")
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)
            .copy(
                aktorId = null,
                fnr = "123123123"
            )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt()
        verifyHentGeografiskEnhetKalt(0)
        verifyOppgaveOpprettetWith("\"aktoerId\":null")
    }

    @Test
    fun `skal feile behandle hendelse hvis hentperson feiler nar aktorid mangler`() {
        val aktorId = "123213213213123"
        stubHentOppgaveContaining(listOf())
        stubHentPerson(aktorId = aktorId, status = HttpStatus.INTERNAL_SERVER_ERROR)
        stubHentGeografiskEnhet(enhet = "1234")
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)
            .copy(
                aktorId = null,
                fnr = "123123123"
            )

        assertThrows<HentArbeidsfordelingFeiletTekniskException> { behandleHendelseService.behandleHendelse(journalpostHendelse) }
    }

    @Test
    fun `skal ikke hente person hvis aktorid ikke mangler`() {
        stubHentOppgaveContaining(listOf())
        stubHentPerson()
        stubHentGeografiskEnhet(enhet = "1234")
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)
            .copy(
                aktorId = "123213213",
                fnr = "123123123"
            )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt(0)
    }

    @Test
    fun `skal oppdatere og opprette behandle dokument oppgave`() {
        val sakMedOppgave = "123123"
        val sakUtenOppgave = "3344444"
        stubHentOppgaveContaining(emptyList())
        stubHentOppgaveContaining(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_5,
                    versjon = 1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "BEH_SAK",
                    beskrivelse = "Behandle dokument (tittel) mottatt 05.05.2020\r\nDokumenter vedlagt: BID-12312321",
                    tema = "BID",
                    tildeltEnhetsnr = "4806",
                    saksreferanse = sakMedOppgave
                )
            ),
            Pair("oppgavetype", "BEH_SAK")
        )

        stubHentPerson()
        stubOpprettOppgave(OPPGAVE_ID_5)
        stubEndreOppgave()
        stubHentGeografiskEnhet(enhet = "1234")

        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_2"
        val journalpostHendelse = createJournalpostHendelse(
            journalpostId = journalpostIdMedJoarkPrefix
        ).copy(
            aktorId = "123213213",
            journalstatus = "J",
            tittel = "Ny tittel",
            journalfortDato = LocalDate.now(),
            dokumentDato = LocalDate.parse("2020-01-02"),
            sakstilknytninger = listOf(sakMedOppgave, sakUtenOppgave),
            fnr = "123123123",
            hendelseType = HendelseType.JOURNALFORING
        )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt(0)

        verifyOppgaveEndretWith(1, "$OPPGAVE_ID_5", "Nytt dokument (Ny tittel) mottatt 02.01.2020")
        verifyOppgaveEndretWith(1, "Dokumenter vedlagt: JOARK-$JOURNALPOST_ID_2")
        verifyOppgaveEndretWith(1, "Behandle dokument (tittel) mottatt 05.05.2020")
        verifyOppgaveOpprettetWith(
            "BEH_SAK",
            "Behandle dokument (Ny tittel) mottatt 02.01.2020",
            "Dokumenter vedlagt: JOARK-$JOURNALPOST_ID_2",
            "\"saksreferanse\":\"3344444\"",
            "\"tilordnetRessurs\":\"$SAKSBEHANDLER_ID\"",
            "\"journalpostId\":null"
        )
    }

    @Test
    fun `skal oppdatere og opprette behandle dokument oppgave med BID journalpost`() {
        val sakMedOppgave = "123123"
        val sakUtenOppgave = "3344444"
        stubHentOppgaveContaining(emptyList())
        stubHentOppgaveContaining(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "BEH_SAK",
                    beskrivelse = "Behandle dokument (tittel) mottatt 05.05.2020\r\nDokumenter vedlagt: BID-$JOURNALPOST_ID_1",
                    tema = "BID",
                    tildeltEnhetsnr = "4806",
                    saksreferanse = sakMedOppgave
                )
            ),
            Pair("oppgavetype", "BEH_SAK")
        )

        stubHentPerson()
        stubOpprettOppgave(OPPGAVE_ID_1)
        stubEndreOppgave()
        stubHentGeografiskEnhet(enhet = "1234")

        val journalpostIdMedJoarkPrefix = "BID-$JOURNALPOST_ID_2"
        val journalpostHendelse = createJournalpostHendelse(
            journalpostId = journalpostIdMedJoarkPrefix
        ).copy(
            aktorId = "123213213",
            journalstatus = "J",
            tittel = "Ny tittel",
            journalfortDato = LocalDate.now(),
            dokumentDato = LocalDate.parse("2020-01-02"),
            sakstilknytninger = listOf(sakMedOppgave, sakUtenOppgave),
            fnr = "123123123",
            hendelseType = HendelseType.JOURNALFORING
        )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt(0)

        verifyOppgaveEndretWith(1, "$OPPGAVE_ID_1", "Nytt dokument (Ny tittel) mottatt 02.01.2020")
        verifyOppgaveEndretWith(1, "Dokumenter vedlagt: BID-$JOURNALPOST_ID_2")
        verifyOppgaveEndretWith(1, "Behandle dokument (tittel) mottatt 05.05.2020")
        verifyOppgaveOpprettetWith(
            "BEH_SAK",
            "Behandle dokument (Ny tittel) mottatt 02.01.2020",
            "Dokumenter vedlagt: BID-142312",
            "\"saksreferanse\":\"3344444\"",
            "\"tilordnetRessurs\":\"$SAKSBEHANDLER_ID\"",
            "\"journalpostId\":null"
        )
    }

    @Test
    fun `skal ikke sette tilordenetressurs hvis lengre enn 7 tegn`() {
        val sakMedOppgave = "123123"
        val sakUtenOppgave = "3344444"
        stubHentOppgaveContaining(emptyList())
        stubHentOppgaveContaining(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "BEH_SAK",
                    beskrivelse = "Behandle dokument (tittel) mottatt 05.05.2020\r\nDokumenter vedlagt: BID-$JOURNALPOST_ID_1",
                    tema = "BID",
                    tildeltEnhetsnr = "4806",
                    saksreferanse = sakMedOppgave
                )
            ),
            Pair("oppgavetype", "BEH_SAK")
        )

        stubHentPerson()
        stubOpprettOppgave(OPPGAVE_ID_1)
        stubEndreOppgave()
        stubHentGeografiskEnhet(enhet = "1234")

        val journalpostIdMedJoarkPrefix = "BID-$JOURNALPOST_ID_2"
        val journalpostHendelse = createJournalpostHendelse(
            journalpostId = journalpostIdMedJoarkPrefix
        ).copy(
            aktorId = "123213213",
            journalstatus = "J",
            sporing = Sporingsdata("test", enhetsnummer = "4833", brukerident = "Z9949772", saksbehandlersNavn = "Navn Navnesen"),
            tittel = "Ny tittel",
            journalfortDato = LocalDate.now(),
            dokumentDato = LocalDate.parse("2020-01-02"),
            sakstilknytninger = listOf(sakMedOppgave, sakUtenOppgave),
            fnr = "123123123",
            hendelseType = HendelseType.JOURNALFORING
        )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt(0)

        verifyOppgaveEndretWith(1, "$OPPGAVE_ID_1", "Nytt dokument (Ny tittel) mottatt 02.01.2020")
        verifyOppgaveEndretWith(1, "Dokumenter vedlagt: BID-$JOURNALPOST_ID_2")
        verifyOppgaveEndretWith(1, "Behandle dokument (tittel) mottatt 05.05.2020")
        verifyOppgaveOpprettetWith(
            "BEH_SAK",
            "Behandle dokument (Ny tittel) mottatt 02.01.2020",
            "Dokumenter vedlagt: BID-142312",
            "\"saksreferanse\":\"3344444\"",
            "\"tilordnetRessurs\":null",
            "\"journalpostId\":null"
        )
    }

    @Test
    fun `skal ikke oppdatere behandle dokument oppgave hvis hendelsetype ENDRING`() {
        val sakMedOppgave = "123123"
        val sakMedOppgave2 = "3344444"
        stubHentOppgaveContaining(emptyList())
        stubHentOppgaveContaining(listOf(), Pair("oppgavetype", "BEH_SAK"))

        stubHentPerson()
        stubOpprettOppgave()
        stubEndreOppgave()
        stubHentGeografiskEnhet(enhet = "1234")

        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_2"
        val journalpostHendelse = createJournalpostHendelse(
            journalpostId = journalpostIdMedJoarkPrefix
        ).copy(
            aktorId = "123213213",
            journalstatus = "J",
            tittel = "Ny tittel",
            journalfortDato = LocalDate.now(),
            dokumentDato = LocalDate.parse("2020-01-02"),
            sakstilknytninger = listOf(sakMedOppgave, sakMedOppgave2),
            fnr = "123123123",
            hendelseType = HendelseType.ENDRING
        )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt(0)

        verifyOppgaveNotEndret()
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal ikke oppdatere behandle dokument oppgave hvis beskrivelse inneholder journalpostid`() {
        val sakMedOppgave = "123123"
        val sakMedOppgave2 = "3344444"
        stubHentOppgaveContaining(emptyList())
        stubHentOppgaveContaining(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_2,
                    versjon = 1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "BEH_SAK",
                    beskrivelse = "Dokumenter vedlagt: JOARK-$JOURNALPOST_ID_2 \r\n\r\n Behandle dokument (tittel) mottatt 05.05.2020",
                    tema = "BID",
                    tildeltEnhetsnr = "4806",
                    saksreferanse = sakMedOppgave
                ),
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "BEH_SAK",
                    beskrivelse = "Behandle dokument (tittel) mottatt 05.05.2020\r\nDokumenter vedlagt: JOARK-$JOURNALPOST_ID_2",
                    tema = "BID",
                    tildeltEnhetsnr = "4806",
                    saksreferanse = sakMedOppgave2
                )
            ),
            Pair("oppgavetype", "BEH_SAK")
        )

        stubHentPerson()
        stubOpprettOppgave()
        stubEndreOppgave()
        stubHentGeografiskEnhet(enhet = "1234")

        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_2"
        val journalpostHendelse = createJournalpostHendelse(
            journalpostId = journalpostIdMedJoarkPrefix
        ).copy(
            aktorId = "123213213",
            journalstatus = "J",
            tittel = "Ny tittel",
            journalfortDato = LocalDate.now(),
            dokumentDato = LocalDate.parse("2020-01-02"),
            sakstilknytninger = listOf(sakMedOppgave, sakMedOppgave2),
            fnr = "123123123",
            hendelseType = HendelseType.JOURNALFORING
        )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt(0)

        verifyOppgaveNotEndret()
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal ikke oppdatere behandle dokument oppgave hvis beskrivelse inneholder BID journalpostId`() {
        val sakMedOppgave = "123123"
        val sakMedOppgave2 = "3344444"
        stubHentOppgaveContaining(emptyList())
        stubHentOppgaveContaining(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_2,
                    versjon = 1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "BEH_SAK",
                    beskrivelse = "Dokumenter vedlagt: BID-$JOURNALPOST_ID_2 \r\n\r\n Behandle dokument (tittel) mottatt 05.05.2020",
                    tema = "BID",
                    tildeltEnhetsnr = "4806",
                    saksreferanse = sakMedOppgave
                ),
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "BEH_SAK",
                    beskrivelse = "Behandle dokument (tittel) mottatt 05.05.2020\r\nDokumenter vedlagt: BID-$JOURNALPOST_ID_2 \r\n",
                    tema = "BID",
                    tildeltEnhetsnr = "4806",
                    saksreferanse = sakMedOppgave2
                )
            ),
            Pair("oppgavetype", "BEH_SAK")
        )

        stubHentPerson()
        stubOpprettOppgave()
        stubEndreOppgave()
        stubHentGeografiskEnhet(enhet = "1234")

        val journalpostIdMedJoarkPrefix = "BID-$JOURNALPOST_ID_2"
        val journalpostHendelse = createJournalpostHendelse(
            journalpostId = journalpostIdMedJoarkPrefix
        ).copy(
            aktorId = "123213213",
            journalstatus = "J",
            tittel = "Ny tittel",
            journalfortDato = LocalDate.now(),
            dokumentDato = LocalDate.parse("2020-01-02"),
            sakstilknytninger = listOf(sakMedOppgave, sakMedOppgave2),
            fnr = "123123123",
            hendelseType = HendelseType.JOURNALFORING
        )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt(0)

        verifyOppgaveNotEndret()
        verifyOppgaveNotOpprettet()
    }


    @Test
    fun `skal lagre journalpost fra hendelse hvis status mottatt`() {
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW)

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val journalpost = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_3_NEW)
        journalpost shouldNotBe null

        assertSoftly {
            journalpost!!.journalpostId shouldBe BID_JOURNALPOST_ID_3_NEW
            journalpost.status shouldBe "MOTTATT"
            journalpost.tema shouldBe "BID"
            journalpost.enhet shouldBe "4833"
        }
        verifyOppgaveNotOpprettet()
    }


    @Test
    fun `skal oppdatere journalpost lagret i databasen`() {
        stubHentOppgave(listOf(OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_1,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        )))
        testDataGenerator.opprettJournalpost(Journalpost(
            journalpostId = BID_JOURNALPOST_ID_1,
            enhet = "4444",
            status = "MOTTATT",
            tema = "BID"
        ))
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_1, enhet = "1234", sporingEnhet = "1234", fagomrade = "FAR")

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val journalpost = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_1)
        journalpost shouldNotBe null

        assertSoftly {
            journalpost!!.journalpostId shouldBe BID_JOURNALPOST_ID_1
            journalpost.status shouldBe "MOTTATT"
            journalpost.tema shouldBe "FAR"
            journalpost.enhet shouldBe "1234"
        }

        verifyOppgaveNotOpprettet()
        verifyOppgaveEndretWith(1, "\"tildeltEnhetsnr\":\"1234\"", "\"endretAvEnhetsnr\":\"1234\"")
    }

    @Test
    fun `skal slette journalpost fra databasen hvis status ikke lenger er mottatt`() {
        stubHentOppgave(listOf(OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_1,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        )))
        testDataGenerator.opprettJournalpost(createJournalpost(BID_JOURNALPOST_ID_1))
        val journalpostOptionalBefore = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_1)
        assertThat(journalpostOptionalBefore).isNotNull

        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_1, status="J", sporingEnhet = "1234")

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val journalpostOptionalAfter = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_1)
        assertThat(journalpostOptionalAfter).isNull()

        verifyOppgaveNotOpprettet()
        verifyOppgaveEndretWith(1, "\"status\":\"FERDIGSTILT\"", "\"endretAvEnhetsnr\":\"1234\"")
    }

    @Test
    fun `skal ikke lagre journalpost hvis status ikke er M og ikke finnes`() {
        stubHentOppgave(listOf(OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_1,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        )))

        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_1, status="J", sporingEnhet = "1234")

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val journalpost = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_1)
        journalpost shouldBe null

        verifyOppgaveNotOpprettet()
        verifyOppgaveEndretWith(1, "\"status\":\"FERDIGSTILT\"", "\"endretAvEnhetsnr\":\"1234\"")
    }

    @Nested
    inner class EndreMellomBidragFagområder {
        @Test
        fun `skal ikke gjøre endring hvis journalpost ikke har ikke-bidrag fagområde`() {
            val journalpostId = "BID-$JOURNALPOST_ID_2"
            stubHentOppgave(listOf(OppgaveData(
                id = OPPGAVE_ID_1,
                versjon = 1,
                journalpostId = JOURNALPOST_ID_1,
                aktoerId = AKTOER_ID,
                oppgavetype = "JFR",
                tema = "BID",
                tildeltEnhetsnr = "4860"
            )))

            testDataGenerator.opprettJournalpost(
                Journalpost(
                    tema = "FAR",
                    journalpostId = journalpostId,
                    status = "MOTTATT",
                    enhet = "4860"
                )
            )

            stubHentTemaTilgang(true)

            val journalpostHendelse = createJournalpostHendelse(
                sporingEnhet = "4806",
                journalpostId = journalpostId,
                fagomrade = "BAR",
                enhet = "4860",
                status = "M"
            )

            behandleHendelseService.behandleHendelse(journalpostHendelse)

            verifySjekkTematilgangKalt(0, SAKSBEHANDLER_ID)

            verifyOppgaveEndretWith(1)
            verifyOppgaveNotOpprettet()
        }
        @Test
        fun `skal ikke gjøre endring hvis journalpost ikke har status mottatt`() {
            val journalpostId = "BID-$JOURNALPOST_ID_2"
            stubHentOppgave(listOf(OppgaveData(
                id = OPPGAVE_ID_1,
                versjon = 1,
                journalpostId = JOURNALPOST_ID_1,
                aktoerId = AKTOER_ID,
                oppgavetype = "BEH_SAK",
                tema = "BID",
                tildeltEnhetsnr = "4860"
            )))

            testDataGenerator.opprettJournalpost(
                Journalpost(
                    tema = "FAR",
                    journalpostId = journalpostId,
                    status = "MOTTATT",
                    enhet = "4860"
                )
            )

            stubHentTemaTilgang(true)

            val journalpostHendelse = createJournalpostHendelse(
                sporingEnhet = "4806",
                journalpostId = journalpostId,
                fagomrade = "BID",
                enhet = "4860",
                status = "J"
            )

            behandleHendelseService.behandleHendelse(journalpostHendelse)

            verifySjekkTematilgangKalt(0, SAKSBEHANDLER_ID)

            verifyOppgaveNotEndret()
            verifyOppgaveNotOpprettet()
        }
        @Test
        fun `skal oppdatere beskrivelse hvis journalpost tema endret fra FAR til BID`() {
            val journalpostId = "BID-$JOURNALPOST_ID_2"
            stubHentOppgave(
                listOf(
                    OppgaveData(
                        id = OPPGAVE_ID_1,
                        versjon = 1,
                        journalpostId = journalpostId,
                        aktoerId = AKTOER_ID,
                        oppgavetype = "JFR",
                        tema = "BID",
                        tildeltEnhetsnr = "4860",
                        tilordnetRessurs = "Z9999"
                    )
                )
            )

            testDataGenerator.opprettJournalpost(
                Journalpost(
                    tema = "FAR",
                    journalpostId = journalpostId,
                    status = "MOTTATT",
                    enhet = "4860"
                )
            )

            stubHentTemaTilgang(true)

            val journalpostHendelse = createJournalpostHendelse(
                sporingEnhet = "4806",
                journalpostId = journalpostId,
                fagomrade = "BID",
                enhet = "4860"
            )

            behandleHendelseService.behandleHendelse(journalpostHendelse)

            verifySjekkTematilgangKalt(1, SAKSBEHANDLER_ID)

            verifyOppgaveEndretWith(
                1,
                "Fagområde endret til Bidrag fra Foreldreskap\\r\\n\\r\\n\""
            )
            verifyOppgaveNotOpprettet()
        }

        @Test
        fun `skal ikke fjerne tilordnetressurs hvis saksbehandler har tilgang og oppdatere beskrivelse hvis journalpost tema endret fra BID til FAR`() {
            val journalpostId = "BID-$JOURNALPOST_ID_2"
            stubHentOppgave(
                listOf(
                    OppgaveData(
                        id = OPPGAVE_ID_1,
                        versjon = 1,
                        journalpostId = journalpostId,
                        aktoerId = AKTOER_ID,
                        oppgavetype = "JFR",
                        tema = "BID",
                        tildeltEnhetsnr = "4860",
                        tilordnetRessurs = "Z9999"
                    )
                )
            )

            testDataGenerator.opprettJournalpost(
                Journalpost(
                    tema = "BID",
                    journalpostId = journalpostId,
                    status = "MOTTATT",
                    enhet = "4860"
                )
            )

            stubHentTemaTilgang(true)

            val journalpostHendelse = createJournalpostHendelse(
                sporingEnhet = "4806",
                journalpostId = journalpostId,
                fagomrade = "FAR",
                enhet = "4860"
            )

            behandleHendelseService.behandleHendelse(journalpostHendelse)

            verifySjekkTematilgangKalt(1, SAKSBEHANDLER_ID)

            verifyOppgaveEndretWith(
                1,
                "Fagområde endret til Foreldreskap fra Bidrag\\r\\n\\r\\n\""
            )
            verifyOppgaveNotOpprettet()
        }

        @Test
        fun `skal fjerne tilordnetressurs og oppdatere beskrivelse hvis journalpost tema endret fra BID til FAR og saksbehandler ikke har tilgang`() {
            val journalpostId = "BID-$JOURNALPOST_ID_2"
            stubHentOppgave(
                listOf(
                    OppgaveData(
                        id = OPPGAVE_ID_1,
                        versjon = 1,
                        journalpostId = journalpostId,
                        aktoerId = AKTOER_ID,
                        oppgavetype = "JFR",
                        tema = "BID",
                        tildeltEnhetsnr = "4860",
                        tilordnetRessurs = "Z9999"
                    )
                )
            )

            testDataGenerator.opprettJournalpost(
                Journalpost(
                    tema = "BID",
                    journalpostId = journalpostId,
                    status = "MOTTATT",
                    enhet = "4860"
                )
            )

            stubHentTemaTilgang(false)

            val journalpostHendelse = createJournalpostHendelse(
                sporingEnhet = "4806",
                journalpostId = journalpostId,
                fagomrade = "FAR",
                enhet = "4860"
            )

            behandleHendelseService.behandleHendelse(journalpostHendelse)

            verifySjekkTematilgangKalt(1, SAKSBEHANDLER_ID)

            verifyOppgaveEndretWith(
                1,
                "\"tilordnetRessurs\":\"\"",
                "Fagområde endret til Foreldreskap fra Bidrag\\r\\n\\r\\nSaksbehandler endret fra Navn Navnesen ($SAKSBEHANDLER_ID, 4806) til ikke valgt\\r\\n\\r\\n\""
            )
            verifyOppgaveNotOpprettet()
        }

        @Test
        fun `skal fjerne tilordnetressurs og oppdatere beskrivelse hvis journalpost tema endret til FAR og saksbehandler ikke har tilgang`() {
            val journalpostId = "BID-$JOURNALPOST_ID_2"
            stubHentOppgave(
                listOf(
                    OppgaveData(
                        id = OPPGAVE_ID_1,
                        versjon = 1,
                        journalpostId = journalpostId,
                        aktoerId = AKTOER_ID,
                        oppgavetype = "JFR",
                        tema = "BID",
                        tildeltEnhetsnr = "4860",
                        tilordnetRessurs = "Z9999"
                    )
                )
            )

            stubHentTemaTilgang(false)

            val journalpostHendelse = createJournalpostHendelse(
                sporingEnhet = "4806",
                journalpostId = journalpostId,
                fagomrade = "FAR",
                enhet = "4860"
            )

            behandleHendelseService.behandleHendelse(journalpostHendelse)

            verifySjekkTematilgangKalt(1, SAKSBEHANDLER_ID)

            verifyOppgaveEndretWith(
                1,
                "\"tilordnetRessurs\":\"\"",
                "Fagområde endret til Foreldreskap\\r\\n\\r\\nSaksbehandler endret fra Navn Navnesen ($SAKSBEHANDLER_ID, 4806) til ikke valgt\\r\\n\\r\\n\""
            )
            verifyOppgaveNotOpprettet()
        }

        @Test
        fun `skal ikke oppdatere oppgave hvis journalpost har tema BID og journalpost ikke lagret i databasen`() {
            val journalpostId = "BID-$JOURNALPOST_ID_2"
            stubHentOppgave(
                listOf(
                    OppgaveData(
                        id = OPPGAVE_ID_1,
                        versjon = 1,
                        journalpostId = journalpostId,
                        aktoerId = AKTOER_ID,
                        oppgavetype = "JFR",
                        tema = "BID",
                        tildeltEnhetsnr = "4860",
                        tilordnetRessurs = "Z9999"
                    )
                )
            )

            stubHentTemaTilgang(true)

            val journalpostHendelse = createJournalpostHendelse(
                journalpostId = journalpostId,
                fagomrade = "BID",
                enhet = "4860"
            )

            behandleHendelseService.behandleHendelse(journalpostHendelse)

            verifySjekkTematilgangKalt(1, SAKSBEHANDLER_ID)
            verifyOppgaveEndretWith(0)
            verifyOppgaveNotOpprettet()
        }

        @Test
        fun `skal ikke oppdatere oppgave hvis journalpost har tema FAR og saksbehandler har tilgang`() {
            val journalpostId = "BID-$JOURNALPOST_ID_2"
            stubHentOppgave(
                listOf(
                    OppgaveData(
                        id = OPPGAVE_ID_1,
                        versjon = 1,
                        journalpostId = "BID-$JOURNALPOST_ID_2",
                        aktoerId = AKTOER_ID,
                        oppgavetype = "JFR",
                        tema = "FAR",
                        tildeltEnhetsnr = "4860",
                        tilordnetRessurs = "Z9999"
                    )
                )
            )

            stubHentTemaTilgang(true)

            val journalpostHendelse = createJournalpostHendelse(
                journalpostId = journalpostId,
                fagomrade = "FAR",
                enhet = "4860"
            )

            behandleHendelseService.behandleHendelse(journalpostHendelse)

            verifySjekkTematilgangKalt(1, SAKSBEHANDLER_ID)
            verifyOppgaveEndretWith(0)
            verifyOppgaveNotOpprettet()
        }

    }

}
