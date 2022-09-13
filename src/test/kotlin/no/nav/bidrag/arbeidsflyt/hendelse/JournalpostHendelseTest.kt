package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.formatterDatoForOppgave
import no.nav.bidrag.arbeidsflyt.model.HentArbeidsfordelingFeiletTekniskException
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.utils.*
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDate

internal class JournalpostHendelseTest: AbstractBehandleHendelseTest() {

    @Autowired
    lateinit var behandleHendelseService: BehandleHendelseService
    @Test
    fun `skal hente journalpostId fra prefikset journalpostId`() {
        val journalpostHendelse = JournalpostHendelse(journalpostId = "BID-101")

        assertThat(journalpostHendelse.journalpostIdUtenPrefix).isEqualTo("101")
    }

    @Test
    fun `skal lagre journalpost`() {
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW)

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val journalpostOptional = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_3_NEW)
        assertThat(journalpostOptional.isPresent).isTrue

        assertThat(journalpostOptional).hasValueSatisfying { journalpost ->
            assertThat(journalpost.journalpostId).isEqualTo(BID_JOURNALPOST_ID_3_NEW)
            assertThat(journalpost.status).isEqualTo("M")
            assertThat(journalpost.tema).isEqualTo("BID")
            assertThat(journalpost.enhet).isEqualTo("4833")
        }
        verifyOppgaveNotOpprettet()
    }


    @Test
    fun `skal endre journalpost`() {
        stubHentOppgave(listOf(OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_1,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        )))
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_1, enhet = "1234", sporingEnhet = "1234")

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val journalpostOptional = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_1)
        assertThat(journalpostOptional.isPresent).isTrue

        assertThat(journalpostOptional).hasValueSatisfying { journalpost ->
            assertThat(journalpost.journalpostId).isEqualTo(BID_JOURNALPOST_ID_1)
            assertThat(journalpost.status).isEqualTo("M")
            assertThat(journalpost.tema).isEqualTo("BID")
            assertThat(journalpost.enhet).isEqualTo("1234")
        }

        verifyOppgaveNotOpprettet()
        verifyOppgaveEndretWith(1, "\"tildeltEnhetsnr\":\"1234\"", "\"endretAvEnhetsnr\":\"1234\"")
    }

    @Test
    fun `skal slette journalpost fra databasen hvis status ikke er M`() {
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
        assertThat(journalpostOptionalBefore.isPresent).isTrue

        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_1, status="J", sporingEnhet = "1234")

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val journalpostOptionalAfter = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_1)
        assertThat(journalpostOptionalAfter.isPresent).isFalse

        verifyOppgaveNotOpprettet()
        verifyOppgaveEndretWith(1, "\"status\":\"FERDIGSTILT\"", "\"endretAvEnhetsnr\":\"1234\"")
    }

    @Test
    fun `skal ikke lagre eller journalpost hvis status ikke er M og ikke finnes`() {
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

        val journalpostOptionalAfter = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_1)
        assertThat(journalpostOptionalAfter.isPresent).isFalse

        verifyOppgaveNotOpprettet()
        verifyOppgaveEndretWith(1, "\"status\":\"FERDIGSTILT\"", "\"endretAvEnhetsnr\":\"1234\"")
    }

    @Test
    fun `skal opprette oppgave med geografisk enhet og aktorid`(){
        val enhet = "4812"
        val aktorid = "123213123213213"
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3, aktorId = aktorid)
        stubHentGeografiskEnhet(enhet)
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW)
        journalpostHendelse.aktorId = null
        journalpostHendelse.fnr = "123213"

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val journalpostOptional = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_3_NEW)
        assertThat(journalpostOptional.isPresent).isTrue

        assertThat(journalpostOptional).hasValueSatisfying { journalpost ->
            assertThat(journalpost.journalpostId).isEqualTo(BID_JOURNALPOST_ID_3_NEW)
            assertThat(journalpost.status).isEqualTo("M")
            assertThat(journalpost.tema).isEqualTo("BID")
            assertThat(journalpost.enhet).isEqualTo("4833")
        }

        verifyOppgaveOpprettetWith("\"aktoerId\":\"$aktorid\"","\"tildeltEnhetsnr\":\"$enhet\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${BID_JOURNALPOST_ID_3_NEW}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
        verifyHentGeografiskEnhetKalt()
        verifyHentPersonKalt()
    }

    @Test
    fun `skal ikke opprette oppgave hvis hent geografisk enhet feiler`(){
        val enhet = "4812"
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        stubHentGeografiskEnhet(enhet, HttpStatus.INTERNAL_SERVER_ERROR)
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW)

        assertThrows<HentArbeidsfordelingFeiletTekniskException> { behandleHendelseService.behandleHendelse(journalpostHendelse)  }

        verifyOppgaveNotOpprettet()
        verifyOppgaveNotEndret()
        verifyHentGeografiskEnhetKalt()
    }

    @Test
    fun `skal opprette oppgave med BID prefix nar journalpost mottat uten oppgave`(){
        val enhet = "4812"
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        stubHentGeografiskEnhet(enhet)
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW)

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val journalpostOptional = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_3_NEW)
        assertThat(journalpostOptional.isPresent).isTrue

        assertThat(journalpostOptional).hasValueSatisfying { journalpost ->
            assertThat(journalpost.journalpostId).isEqualTo(BID_JOURNALPOST_ID_3_NEW)
            assertThat(journalpost.status).isEqualTo("M")
            assertThat(journalpost.tema).isEqualTo("BID")
            assertThat(journalpost.enhet).isEqualTo("4833")
        }

        verifyOppgaveOpprettetWith("\"tildeltEnhetsnr\":\"$enhet\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${BID_JOURNALPOST_ID_3_NEW}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
    }

    @Test
    fun `skal opprette oppgave med uten prefix nar Joark journalpost mottat uten oppgave`(){
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        stubHentGeografiskEnhet()
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val journalpostOptional = testDataGenerator.hentJournalpost(JOURNALPOST_ID_4_NEW)
        assertThat(journalpostOptional.isPresent).isTrue

        assertThat(journalpostOptional).hasValueSatisfying { journalpost ->
            assertThat(journalpost.journalpostId).isEqualTo(JOURNALPOST_ID_4_NEW)
            assertThat(journalpost.status).isEqualTo("M")
            assertThat(journalpost.tema).isEqualTo("BID")
            assertThat(journalpost.enhet).isEqualTo("4833")
        }

        verifyOppgaveOpprettetWith(
            "\"tildeltEnhetsnr\":\"$ENHET_4806\"",
            "\"fristFerdigstillelse\":\"${formatterDatoForOppgave(DateUtils.finnNesteArbeidsdag())}\"",
            "\"oppgavetype\":\"JFR\"",
            "\"journalpostId\":\"${JOURNALPOST_ID_4_NEW}\"",
            "\"opprettetAvEnhetsnr\":\"9999\"",
            "\"prioritet\":\"HOY\"",
            "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
    }

    @Test
    fun `skal opprette oppgave med tema BID selv om journalpost har tema FAR`(){
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        stubHentGeografiskEnhet()
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)
        journalpostHendelse.fagomrade = "FAR"

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val journalpostOptional = testDataGenerator.hentJournalpost(JOURNALPOST_ID_4_NEW)
        assertThat(journalpostOptional.isPresent).isTrue

        assertThat(journalpostOptional).hasValueSatisfying { journalpost ->
            assertThat(journalpost.journalpostId).isEqualTo(JOURNALPOST_ID_4_NEW)
            assertThat(journalpost.status).isEqualTo("M")
            assertThat(journalpost.tema).isEqualTo("FAR")
            assertThat(journalpost.enhet).isEqualTo("4833")
        }

        verifyOppgaveOpprettetWith(
            "\"tildeltEnhetsnr\":\"$ENHET_4806\"",
            "\"fristFerdigstillelse\":\"${formatterDatoForOppgave(DateUtils.finnNesteArbeidsdag())}\"",
            "\"oppgavetype\":\"JFR\"",
            "\"journalpostId\":\"${JOURNALPOST_ID_4_NEW}\"",
            "\"opprettetAvEnhetsnr\":\"9999\"",
            "\"prioritet\":\"HOY\"",
            "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
    }

    @Test
    fun `skal ferdigstille oppgave nar endret til ekstern fagomrade`(){
        stubHentOppgave(listOf(OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_4_NEW,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        )))
        stubHentPerson(PERSON_IDENT_3)
        val journalpostIdMedJoarkPrefix = "BID-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)
        journalpostHendelse.fagomrade = "EKSTERN"

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveEndretWith(1, "\"status\":\"FERDIGSTILT\"", "\"endretAvEnhetsnr\":\"4833\"")
        verifyOppgaveEndretWith(0, "\"tema\":\"EKSTERN\"", "\"endretAvEnhetsnr\":\"4833\"")
    }

    @Test
    fun `skal ferdigstille oppgave nar endret til ekstern fagomrade for Joark journalpost men JFR oppgave finnes fra for`(){
        stubHentOppgaveContaining(listOf(OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_4_NEW,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        )), Pair("tema", "BID"))
        stubHentOppgaveContaining(listOf(OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_4_NEW,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "EKSTERN",
            tildeltEnhetsnr = "4833"
        )), Pair("tema", "EKSTERN"))
        stubHentPerson(PERSON_IDENT_3)
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)
        journalpostHendelse.fagomrade = "EKSTERN"

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyOppgaveEndretWith(1, "\"status\":\"FERDIGSTILT\"", "\"endretAvEnhetsnr\":\"4833\"")
        verifyOppgaveEndretWith(0, "\"tema\":\"EKSTERN\"", "\"endretAvEnhetsnr\":\"4833\"")
    }

    @Test
    fun `skal hente aktorid hvis mangler`(){
        val aktorId = "123213213213123"
        stubHentOppgaveContaining(listOf())
        stubHentPerson(aktorId = aktorId)
        stubHentGeografiskEnhet(enhet = "1234")
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)
        journalpostHendelse.aktorId = null
        journalpostHendelse.fnr = "123123123"

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt()
        verifyHentGeografiskEnhetKalt()
        verifyOppgaveOpprettetWith( "\"aktoerId\":\"$aktorId\"")
    }

    @Test
    fun `skal opprette oppgave med aktorid null hvis personid ikke funnet`(){
        stubHentOppgaveContaining(listOf())
        stubHentPerson(status = HttpStatus.NO_CONTENT)
        stubHentGeografiskEnhet(enhet = "1234")
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)
        journalpostHendelse.aktorId = null
        journalpostHendelse.fnr = "123123123"

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt()
        verifyHentGeografiskEnhetKalt(0)
        verifyOppgaveOpprettetWith( "\"aktoerId\":null")
    }

    @Test
    fun `skal feile behandle hendelse hvis hentperson feiler nar aktorid mangler`(){
        val aktorId = "123213213213123"
        stubHentOppgaveContaining(listOf())
        stubHentPerson(aktorId = aktorId, status = HttpStatus.INTERNAL_SERVER_ERROR)
        stubHentGeografiskEnhet(enhet = "1234")
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)
        journalpostHendelse.aktorId = null
        journalpostHendelse.fnr = "123123123"

        assertThrows<HentArbeidsfordelingFeiletTekniskException> { behandleHendelseService.behandleHendelse(journalpostHendelse) }
    }

    @Test
    fun `skal ikke hente person hvis aktorid ikke mangler`(){
        stubHentOppgaveContaining(listOf())
        stubHentPerson()
        stubHentGeografiskEnhet(enhet = "1234")
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)
        journalpostHendelse.aktorId = "123213213"
        journalpostHendelse.fnr = "123123123"

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt(0)
    }

    @Test
    fun `skal oppdatere og opprette behandle dokument oppgave`(){
        val sakMedOppgave = "123123"
        val sakUtenOppgave = "3344444"
        stubHentOppgaveContaining(emptyList())
        stubHentOppgaveContaining(listOf(
            OppgaveData(
                id = OPPGAVE_ID_5,
                versjon = 1,
                journalpostId = JOURNALPOST_ID_4_NEW,
                aktoerId = AKTOER_ID,
                oppgavetype = "BEH_SAK",
                beskrivelse = "Behandle dokument (tittel) mottatt 05.05.2020",
                tema = "BID",
                tildeltEnhetsnr = "4806",
                saksreferanse = sakMedOppgave
            )
        ), Pair("oppgavetype", "BEH_SAK"))

        stubHentPerson()
        stubOpprettOppgave(OPPGAVE_ID_5)
        stubEndreOppgave()
        stubHentGeografiskEnhet(enhet = "1234")

        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_2"
        val journalpostHendelse = createJournalpostHendelse(
            journalpostId = journalpostIdMedJoarkPrefix
        )
        journalpostHendelse.journalstatus = "J"
        journalpostHendelse.tittel = "Ny tittel"
        journalpostHendelse.journalfortDato = LocalDate.now()
        journalpostHendelse.dokumentDato = LocalDate.parse("2020-01-02")
        journalpostHendelse.sakstilknytninger = listOf(sakMedOppgave, sakUtenOppgave)
        journalpostHendelse.aktorId = "123213213"
        journalpostHendelse.fnr = "123123123"

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt(0)

        verifyOppgaveEndretWith(1, "$OPPGAVE_ID_5", "Nytt dokument (Ny tittel) mottatt 02.01.2020")
        verifyOppgaveEndretWith(1, "Dokumenter vedlagt: JOARK-$JOURNALPOST_ID_2")
        verifyOppgaveEndretWith(1, "Behandle dokument (tittel) mottatt 05.05.2020")
        verifyOppgaveOpprettetWith("BEH_SAK",
            "Behandle dokument (Ny tittel) mottatt 02.01.2020",
            "\"saksreferanse\":\"3344444\"",
            "\"tilordnetRessurs\":\"Z12312312\"",
            "\"journalpostId\":\"$JOURNALPOST_ID_2\""
        )
    }

    @Test
    fun `skal oppdatere og opprette behandle dokument oppgave med BID journalpost`(){
        val sakMedOppgave = "123123"
        val sakUtenOppgave = "3344444"
        stubHentOppgaveContaining(emptyList())
        stubHentOppgaveContaining(listOf(
            OppgaveData(
                id = OPPGAVE_ID_1,
                versjon = 1,
                journalpostId = "BID-$JOURNALPOST_ID_1",
                aktoerId = AKTOER_ID,
                oppgavetype = "BEH_SAK",
                beskrivelse = "Behandle dokument (tittel) mottatt 05.05.2020",
                tema = "BID",
                tildeltEnhetsnr = "4806",
                saksreferanse = sakMedOppgave
            )
        ), Pair("oppgavetype", "BEH_SAK"))

        stubHentPerson()
        stubOpprettOppgave(OPPGAVE_ID_1)
        stubEndreOppgave()
        stubHentGeografiskEnhet(enhet = "1234")

        val journalpostIdMedJoarkPrefix = "BID-$JOURNALPOST_ID_2"
        val journalpostHendelse = createJournalpostHendelse(
            journalpostId = journalpostIdMedJoarkPrefix
        )
        journalpostHendelse.journalstatus = "J"
        journalpostHendelse.tittel = "Ny tittel"
        journalpostHendelse.journalfortDato = LocalDate.now()
        journalpostHendelse.dokumentDato = LocalDate.parse("2020-01-02")
        journalpostHendelse.sakstilknytninger = listOf(sakMedOppgave, sakUtenOppgave)
        journalpostHendelse.aktorId = "123213213"
        journalpostHendelse.fnr = "123123123"

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt(0)

        verifyOppgaveEndretWith(1, "$OPPGAVE_ID_1", "Nytt dokument (Ny tittel) mottatt 02.01.2020")
        verifyOppgaveEndretWith(1, "Dokumenter vedlagt: BID-$JOURNALPOST_ID_2")
        verifyOppgaveEndretWith(1, "Behandle dokument (tittel) mottatt 05.05.2020")
        verifyOppgaveOpprettetWith("BEH_SAK",
            "Behandle dokument (Ny tittel) mottatt 02.01.2020",
            "\"saksreferanse\":\"3344444\"",
            "\"tilordnetRessurs\":\"Z12312312\"",
            "\"journalpostId\":\"BID-$JOURNALPOST_ID_2\""
        )
    }

    @Test
    fun `skal ikke oppdatere behandle dokument oppgave hvis inneholder journalpost`(){
        val sakMedOppgave = "123123"
        val sakMedOppgave2 = "3344444"
        stubHentOppgaveContaining(emptyList())
        stubHentOppgaveContaining(listOf(
            OppgaveData(
                id = OPPGAVE_ID_2,
                versjon = 1,
                journalpostId = JOURNALPOST_ID_4_NEW,
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
                journalpostId = JOURNALPOST_ID_2,
                aktoerId = AKTOER_ID,
                oppgavetype = "BEH_SAK",
                beskrivelse = "Behandle dokument (tittel) mottatt 05.05.2020",
                tema = "BID",
                tildeltEnhetsnr = "4806",
                saksreferanse = sakMedOppgave2
            )
        ), Pair("oppgavetype", "BEH_SAK"))

        stubHentPerson()
        stubOpprettOppgave()
        stubEndreOppgave()
        stubHentGeografiskEnhet(enhet = "1234")

        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_2"
        val journalpostHendelse = createJournalpostHendelse(
            journalpostId = journalpostIdMedJoarkPrefix
        )
        journalpostHendelse.journalstatus = "J"
        journalpostHendelse.tittel = "Ny tittel"
        journalpostHendelse.journalfortDato = LocalDate.now()
        journalpostHendelse.dokumentDato = LocalDate.parse("2020-01-02")
        journalpostHendelse.sakstilknytninger = listOf(sakMedOppgave, sakMedOppgave2)
        journalpostHendelse.aktorId = "123213213"
        journalpostHendelse.fnr = "123123123"

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt(0)

        verifyOppgaveNotEndret()
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal ikke oppdatere behandle dokument oppgave hvis inneholder BID journalpost`(){
        val sakMedOppgave = "123123"
        val sakMedOppgave2 = "3344444"
        stubHentOppgaveContaining(emptyList())
        stubHentOppgaveContaining(listOf(
            OppgaveData(
                id = OPPGAVE_ID_2,
                versjon = 1,
                journalpostId = "BID-$JOURNALPOST_ID_1",
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
                journalpostId = "BID-$JOURNALPOST_ID_2",
                aktoerId = AKTOER_ID,
                oppgavetype = "BEH_SAK",
                beskrivelse = "Behandle dokument (tittel) mottatt 05.05.2020",
                tema = "BID",
                tildeltEnhetsnr = "4806",
                saksreferanse = sakMedOppgave2
            )
        ), Pair("oppgavetype", "BEH_SAK"))

        stubHentPerson()
        stubOpprettOppgave()
        stubEndreOppgave()
        stubHentGeografiskEnhet(enhet = "1234")

        val journalpostIdMedJoarkPrefix = "BID-$JOURNALPOST_ID_2"
        val journalpostHendelse = createJournalpostHendelse(
            journalpostId = journalpostIdMedJoarkPrefix
        )
        journalpostHendelse.journalstatus = "J"
        journalpostHendelse.tittel = "Ny tittel"
        journalpostHendelse.journalfortDato = LocalDate.now()
        journalpostHendelse.dokumentDato = LocalDate.parse("2020-01-02")
        journalpostHendelse.sakstilknytninger = listOf(sakMedOppgave, sakMedOppgave2)
        journalpostHendelse.aktorId = "123213213"
        journalpostHendelse.fnr = "123123123"

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verifyHentPersonKalt(0)

        verifyOppgaveNotEndret()
        verifyOppgaveNotOpprettet()
    }
}
