package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.utils.AKTOER_ID
import no.nav.bidrag.arbeidsflyt.utils.BID_JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.BID_JOURNALPOST_ID_3_NEW
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_4_NEW
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_1
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_1
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_3
import no.nav.bidrag.arbeidsflyt.utils.createJournalpost
import no.nav.bidrag.arbeidsflyt.utils.createJournalpostHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

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
    fun `skal opprette oppgave med BID prefix nar journalpost mottat uten oppgave`(){
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
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

        verifyOppgaveOpprettetWith("\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${BID_JOURNALPOST_ID_3_NEW}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
    }

    @Test
    fun `skal opprette oppgave med uten prefix nar Joark journalpost mottat uten oppgave`(){
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
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

        verifyOppgaveOpprettetWith("\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${JOURNALPOST_ID_4_NEW}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
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
    fun `skal endre tema oppgave nar endret til ekstern fagomrade for Joark journalpost`(){
        val defaultLocalDateTime = LocalDateTime.of(2020, 1, 1, 12, 0)
        val mockStatic: MockedStatic<LocalDateTime> = Mockito.mockStatic(LocalDateTime::class.java)

        try {
            mockStatic.`when`<Any>(LocalDateTime::now).thenReturn(defaultLocalDateTime)
            stubHentOppgave(emptyList())
            stubHentOppgaveContaining(listOf(OppgaveData(
                id = OPPGAVE_ID_1,
                versjon = 1,
                journalpostId = JOURNALPOST_ID_4_NEW,
                aktoerId = AKTOER_ID,
                oppgavetype = "JFR",
                tema = "BID",
                beskrivelse = "test",
                tildeltEnhetsnr = "4833"
            )), Pair("tema", "BID"))
            stubHentPerson(PERSON_IDENT_3)
            val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
            val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)
            journalpostHendelse.fagomrade = "EKSTERN"
            journalpostHendelse.enhet = "4999"

            behandleHendelseService.behandleHendelse(journalpostHendelse)

            verifyOppgaveEndretWith(1,  "\"beskrivelse\":\"--- 01.01.2020 12:00 Z12312312, Navn Navnesen ---\\r\\nOppgave overført fra tema BID til EKSTERN\\r\\n\\r\\n--- 01.01.2020 12:00 Z12312312, Navn Navnesen ---\\r\\nSaksbehandler endret fra Z12312312, Navn Navnesen til ikke valgt\\r\\n\\r\\ntest\"")
            verifyOppgaveEndretWith(1,   "\"tema\":\"EKSTERN\"", "\"endretAvEnhetsnr\":\"4833\"", "\"tildeltEnhetsnr\":\"4999\"", "\"tilordnetRessurs\":null")
            verifyOppgaveEndretWith(0, "\"status\":\"FERDIGSTILT\"", "\"endretAvEnhetsnr\":\"4833\"")
        } finally {
            mockStatic.reset()
        }


    }

}
