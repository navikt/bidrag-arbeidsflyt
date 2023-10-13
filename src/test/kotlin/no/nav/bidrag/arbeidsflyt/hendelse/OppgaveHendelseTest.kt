package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.dto.Oppgavestatuskategori
import no.nav.bidrag.arbeidsflyt.dto.formatterDatoForOppgave
import no.nav.bidrag.arbeidsflyt.hendelse.dto.OppgaveKafkaHendelse
import no.nav.bidrag.arbeidsflyt.service.BehandleOppgaveHendelseService
import no.nav.bidrag.arbeidsflyt.utils.AKTOER_ID
import no.nav.bidrag.arbeidsflyt.utils.BID_JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_3
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVETYPE_JFR
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_1
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_3
import no.nav.bidrag.arbeidsflyt.utils.createOppgave
import no.nav.bidrag.arbeidsflyt.utils.createOppgaveData
import no.nav.bidrag.arbeidsflyt.utils.journalpostResponse
import no.nav.bidrag.arbeidsflyt.utils.toHendelse
import no.nav.bidrag.commons.util.VirkedagerProvider
import no.nav.bidrag.transport.dokument.JournalpostStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDate

class OppgaveHendelseTest : AbstractBehandleHendelseTest() {
    @Autowired
    lateinit var behandleOppgaveHendelseService: BehandleOppgaveHendelseService

    @Test
    fun `skal lagre oppgave`() {
        val journalpostId = "213213123"
        val oppgaveId = 10123L
        val oppgaveData = createOppgaveData(oppgaveId, journalpostId = journalpostId, status = OppgaveStatus.OPPRETTET)
        stubHentOppgave(oppgaveData.id, oppgaveData)

        assertThat(testDataGenerator.hentOppgave(oppgaveId).isPresent).isFalse
        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse(OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_OPPRETTET))

        val opprettetOppgaveOptional = testDataGenerator.hentOppgave(oppgaveId)
        assertThat(opprettetOppgaveOptional.isPresent).isTrue

        assertThat(opprettetOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(oppgaveId)
            assertThat(oppgave.journalpostId).isEqualTo(journalpostId)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_JFR)
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.OPPRETTET.name)
        }
    }

    @Test
    fun `skal lagre journalforingsoppgave ved endring hvis ikke finnes`() {
        val oppgaveData = createOppgaveData(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_3, status = OppgaveStatus.OPPRETTET)
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        val endretOppgaveOptional = testDataGenerator.hentOppgave(OPPGAVE_ID_3)
        assertThat(endretOppgaveOptional.isPresent).isTrue

        assertThat(endretOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(OPPGAVE_ID_3)
            assertThat(oppgave.journalpostId).isEqualTo(JOURNALPOST_ID_3)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_JFR)
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.OPPRETTET.name)
        }

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal endre oppgave`() {
        testDataGenerator.opprettOppgave(createOppgave(OPPGAVE_ID_3, journalpostId = "UKJENT"))
        val oppgaveData = createOppgaveData(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_3, status = OppgaveStatus.OPPRETTET)
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        val endretOppgaveOptional = testDataGenerator.hentOppgave(OPPGAVE_ID_3)
        assertThat(endretOppgaveOptional.isPresent).isTrue

        assertThat(endretOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(OPPGAVE_ID_3)
            assertThat(oppgave.journalpostId).isEqualTo(JOURNALPOST_ID_3)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_JFR)
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.OPPRETTET.name)
        }

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal slette oppgave fra databasen nar oppgave lukket`() {
        stubHentJournalpost()
        val oppgaveData = createOppgaveData(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_3, statuskategori = Oppgavestatuskategori.AVSLUTTET)
        stubHentOppgave(oppgaveData.id, oppgaveData)
        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse(OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_FERDIGSTILT))

        val endretOppgaveOptional = testDataGenerator.hentOppgave(OPPGAVE_ID_3)
        assertThat(endretOppgaveOptional.isPresent).isFalse

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal opprette oppgave med BID prefix nar det ikke finnes noen aapne jfr oppgaver men journalpost status er mottatt`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet()
        stubHentJournalpost(journalpostResponse(BID_JOURNALPOST_ID_1, journalStatus = JournalpostStatus.MOTTATT))
        val oppgaveData = createOppgaveData(OPPGAVE_ID_1, journalpostId = BID_JOURNALPOST_ID_1, aktoerId = AKTOER_ID, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET, tildeltEnhetsnr = "4812")
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyOppgaveOpprettetWith("\"tildeltEnhetsnr\":\"4812\"", "\"aktoerId\":\"$AKTOER_ID\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${BID_JOURNALPOST_ID_1}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
        verifyHentGeografiskEnhetKalt(0)
    }

    @Test
    fun `skal hente geografisk enhet hvis oppgave ikke har tildelt enhetsnr`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet("4816")
        stubHentJournalpost(journalpostResponse(BID_JOURNALPOST_ID_1, journalStatus = JournalpostStatus.MOTTATT))
        val oppgaveData = createOppgaveData(OPPGAVE_ID_1, journalpostId = BID_JOURNALPOST_ID_1, aktoerId = AKTOER_ID, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET, tildeltEnhetsnr = null)
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyOppgaveOpprettetWith("\"tildeltEnhetsnr\":\"4816\"", "\"aktoerId\":\"$AKTOER_ID\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${BID_JOURNALPOST_ID_1}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
        verifyHentGeografiskEnhetKalt(1)
    }

    @Test
    fun `skal opprette oppgave nar det ikke finnes noen aapne jfr oppgaver men journalpost status er mottatt`() {
        stubHentOppgaveSok(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    journalpostId = JOURNALPOST_ID_1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "BEH_SAK",
                    status = OppgaveStatus.AAPNET,
                    tema = "BID",
                    tildeltEnhetsnr = "4833"
                )
            )
        )
        stubHentGeografiskEnhet()
        stubHentJournalpost(journalpostResponse(JOURNALPOST_ID_1, journalStatus = JournalpostStatus.MOTTATT))
        val oppgaveData = createOppgaveData(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET, fristFerdigstillelse = LocalDate.of(2020, 2, 1))
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyOppgaveOpprettetWith("\"fristFerdigstillelse\":\"2020-02-01\"", "\"aktoerId\":\"$AKTOER_ID\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"$JOURNALPOST_ID_1\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyHentGeografiskEnhetKalt(0)
    }

    @Test
    fun `skal opprette oppgave med frist neste dag hvis JFR lukket og journalpost har status MOTTATT`() {
        stubHentGeografiskEnhet()
        stubHentJournalpost(journalpostResponse(JOURNALPOST_ID_1, journalStatus = JournalpostStatus.MOTTATT))
        stubHentOppgaveSok(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    journalpostId = JOURNALPOST_ID_1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "BEH_SAK",
                    status = OppgaveStatus.AAPNET,
                    tema = "BID",
                    tildeltEnhetsnr = "4833"
                )
            )
        )
        val oppgaveData = createOppgaveData(
            OPPGAVE_ID_1,
            journalpostId = JOURNALPOST_ID_1,
            aktoerId = AKTOER_ID,
            status = OppgaveStatus.FERDIGSTILT,
            statuskategori = Oppgavestatuskategori.AVSLUTTET,
            fristFerdigstillelse = null,
            oppgavetype = OPPGAVETYPE_JFR
        )
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyOppgaveOpprettetWith(
            "\"fristFerdigstillelse\":\"${formatterDatoForOppgave(VirkedagerProvider.nesteVirkedag())}\"",
            "\"aktoerId\":\"$AKTOER_ID\"",
            "\"oppgavetype\":\"JFR\"",
            "\"journalpostId\":\"$JOURNALPOST_ID_1\"",
            "\"opprettetAvEnhetsnr\":\"9999\"",
            "\"prioritet\":\"HOY\"",
            "\"tema\":\"BID\""
        )
        verifyHentGeografiskEnhetKalt(0)
    }

    @Test
    fun `skal ikke opprette oppgave hvis JFR lukket av fagpost selv om journalpost har status MOTTATT`() {
        stubHentGeografiskEnhet()
        stubHentJournalpost(journalpostResponse(JOURNALPOST_ID_1, journalStatus = JournalpostStatus.MOTTATT))
        stubHentOppgaveSok(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    journalpostId = JOURNALPOST_ID_1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "BEH_SAK",
                    status = OppgaveStatus.AAPNET,
                    tema = "BID",
                    tildeltEnhetsnr = "4833"
                )
            )
        )
        val oppgaveData = createOppgaveData(
            OPPGAVE_ID_1,
            journalpostId = JOURNALPOST_ID_1,
            aktoerId = AKTOER_ID,
            status = OppgaveStatus.FERDIGSTILT,
            statuskategori = Oppgavestatuskategori.AVSLUTTET,
            fristFerdigstillelse = null,
            oppgavetype = OPPGAVETYPE_JFR,
            tildeltEnhetsnr = "2950"
        )
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyOppgaveNotOpprettet()
        verifyHentGeografiskEnhetKalt(0)
    }

    @Test
    fun `skal ikke opprette oppgave nar det ikke finnes noen aapne jfr oppgaver men journalpost status er mottatt men ikke har tema bidrag`() {
        stubHentOppgaveSok(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    journalpostId = JOURNALPOST_ID_1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "BEH_SAK",
                    status = OppgaveStatus.AAPNET,
                    tema = "BID",
                    tildeltEnhetsnr = "4833"
                )
            )
        )
        stubHentJournalpost(journalpostResponse(JOURNALPOST_ID_1, journalStatus = JournalpostStatus.MOTTATT, tema = "BAR"))
        val oppgaveData = createOppgaveData(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET)
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal ikke opprette oppgave nar oppgave ferdigstilt og journalpost status er mottatt men har allerede aapen JFR oppgave`() {
        stubHentOppgaveSok(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    journalpostId = JOURNALPOST_ID_1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "JFR",
                    tema = "BID",
                    tildeltEnhetsnr = "4833"
                )
            )
        )
        stubHentJournalpost(journalpostResponse(JOURNALPOST_ID_1, journalStatus = JournalpostStatus.MOTTATT))
        val oppgaveData = createOppgaveData(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET)
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal ikke opprette oppgave nar oppgave ferdigstilt og det ikke finnes noen journalposter lagret i databasen`() {
        stubHentOppgaveSok(emptyList())
        stubHentJournalpost(status = HttpStatus.NOT_FOUND)

        val oppgaveData = createOppgaveData(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET)
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal opprette oppgave nar oppgave endret fra JFR til BEH_SAK og journalpost er mottatt`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet()
        stubHentJournalpost(journalpostResponse(JOURNALPOST_ID_1, journalStatus = JournalpostStatus.MOTTATT))
        testDataGenerator.opprettOppgave(createOppgave(OPPGAVE_ID_1))

        val oppgaveData = createOppgaveData(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "BEH_SAK", status = OppgaveStatus.UNDER_BEHANDLING)
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyOppgaveOpprettetWith("\"aktoerId\":\"$AKTOER_ID\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"$JOURNALPOST_ID_1\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
    }

    @Test
    fun `skal ikke opprette oppgave nar oppgave endret fra JFR til BEH_SAK og journalpost ikke mottatt`() {
        stubHentOppgaveSok(emptyList())
        testDataGenerator.opprettOppgave(createOppgave(OPPGAVE_ID_1))
        stubHentJournalpost(journalpostResponse(BID_JOURNALPOST_ID_1, journalStatus = JournalpostStatus.JOURNALFØRT))
        val oppgaveData = createOppgaveData(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "BEH_SAK")
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal ikke opprette oppgave nar oppgave endret fra JFR til BEH_SAK og oppgave ikke er lagret databasen`() {
        stubHentOppgaveSok(emptyList())
        val oppgaveData = createOppgaveData(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "BEH_SAK")
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal overfore journalforingsoppgave til journalforende enhet hvis oppgave tildelt enhet ikke er knyttet til journalforende enhet`() {
        stubHentOppgaveSok(emptyList())
        stubHentJournalpost()
        stubHentGeografiskEnhet("4806")
        val oppgaveData = createOppgaveData(12323213, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "JFR", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN, beskrivelse = "En annen beskrivelse")
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveEndretWith(1, "Oppgave overført fra enhet 9999 til 4806")
        verifyOppgaveEndretWith(1, "En annen beskrivelse")
        verifyOppgaveEndretWith(0, "Saksbehandler endret fra z99123 til ikke valgt")
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal overfore journalforingsoppgave til journalforende enhet og fjerne tilordnetressurs hvis oppgave ikke er knyttet til journalforende enhet`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet("4806")
        val oppgaveData = createOppgaveData(12323213, tilordnetRessurs = "z99123", journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "JFR", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN, beskrivelse = "En annen beskrivelse")
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveEndretWith(1, "Oppgave overført fra enhet 9999 til 4806")
        verifyOppgaveEndretWith(1, "En annen beskrivelse")
        verifyOppgaveEndretWith(1, "Saksbehandler endret fra z99123 til ikke valgt")
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal ikke overfore journalforingsoppgave til journalforende enhet hvis oppgave tildelt journalforende enhet`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet()
        val oppgaveData = createOppgaveData(12323213, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "JFR", tildeltEnhetsnr = "4806", statuskategori = Oppgavestatuskategori.AAPEN)
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveNotEndret()
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal ikke overfore oppgave til journalforende enhet hvis oppgavetype ikke tilhorer journalforende enhet (er ikke JFR eller VUR)`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet("4806")
        val oppgaveData = createOppgaveData(12323213, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "BEH_SAK", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN)
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveNotEndret()
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal ikke overfore journalforingsoppgave til journalforende enhet hvis oppgave tildelt enhet er fagpost`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet()
        val oppgaveData = createOppgaveData(12323213, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "JFR", tildeltEnhetsnr = "2950", statuskategori = Oppgavestatuskategori.AAPEN)
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyOppgaveNotEndret()
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal overfore retur oppgave til farskap enhet hvis journalpost har tema FAR`() {
        val oppgaveData = createOppgaveData(12323213, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "RETUR", tildeltEnhetsnr = "9999", status = OppgaveStatus.AAPNET, beskrivelse = "En annen beskrivelse")
        stubHentOppgave(oppgaveData.id, oppgaveData)
        stubHentGeografiskEnhet("4806")
        stubHentJournalpost(journalpostResponse(tema = "FAR"))

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse(OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_OPPRETTET))

        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveEndretWith(1, "Oppgave overført fra enhet 9999 til 4860")
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal overfore vurderdokument oppgave til journalforende enhet hvis oppgave ikke er knyttet til journalforende enhet nar opprettet`() {
        val oppgaveData = createOppgaveData(12323213, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "VUR", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN, beskrivelse = "En annen beskrivelse")
        stubHentOppgave(oppgaveData.id, oppgaveData)
        stubHentGeografiskEnhet("4806")
        stubHentJournalpost()

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse(OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_OPPRETTET))

        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveEndretWith(1, "Oppgave overført fra enhet 9999 til 4806")
        verifyOppgaveEndretWith(1, "En annen beskrivelse")
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal overfore vurderdokument oppgave til journalforende enhet hvis oppgave ikke er knyttet til journalforende enhet nar endret`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet("4806")
        stubHentJournalpost()
        val oppgaveData = createOppgaveData(12323213, tilordnetRessurs = "z99123", journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "VUR", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN, beskrivelse = "En annen beskrivelse")
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveEndretWith(1, "Oppgave overført fra enhet 9999 til 4806")
        verifyOppgaveEndretWith(1, "En annen beskrivelse")
        verifyOppgaveEndretWith(1, "Saksbehandler endret fra z99123 til ikke valgt")
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal ikke overfore vurderdokument oppgave til journalforende enhet hvis oppgave tildelt journalforende enhet ved opprettet`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet()
        stubHentJournalpost()
        val oppgaveData = createOppgaveData(12323213, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "VUR", tildeltEnhetsnr = "4806", statuskategori = Oppgavestatuskategori.AAPEN)
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse(OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_OPPRETTET))

        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveNotEndret()
        verifyOppgaveNotOpprettet()
        verifyDokumentHentet()
    }

    @Test
    fun `Skal ikke overfore vurderdokument oppgave til journalforende enhet hvis oppgave tildelt journalforende enhet`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet()
        stubHentJournalpost()
        val oppgaveData = createOppgaveData(12323213, journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "VUR", tildeltEnhetsnr = "4806", statuskategori = Oppgavestatuskategori.AAPEN)
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveNotEndret()
        verifyOppgaveNotOpprettet()
        verifyDokumentHentet()
    }

    @Test
    fun `Skal endre vurderdokument oppgavetype til journalforing hvis journalpost status mottatt etter oppgave opprettet`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet("4806")
        stubHentJournalpost(journalpostResponse(journalStatus = JournalpostStatus.MOTTATT, tema = "BAR"))
        val oppgaveData = createOppgaveData(12323213, tilordnetRessurs = "z99123", journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "VUR", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN, beskrivelse = "En annen beskrivelse")
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse(OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_OPPRETTET))

        verifyOppgaveEndretWith(null, "Automatisk jobb ---\\r\\n· Oppgavetype endret fra Vurder dokument til Journalføring\\r\\n· Oppgave overført fra enhet 9999 til 4806\\r\\n· Saksbehandler endret fra z99123 til ikke valgt\\r\\n\\r\\n\\r\\nEn annen beskrivelse")
        verifyDokumentHentet()
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal endre vurderdokument oppgavetype til vurder henvendelse hvis oppgave ikke har journalpost`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet("4806")
        stubHentJournalpost()
        val oppgaveData = createOppgaveData(12323213, tilordnetRessurs = "z99123", journalpostId = null, aktoerId = AKTOER_ID, oppgavetype = "VUR", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN, beskrivelse = "En annen beskrivelse")
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse(OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_OPPRETTET))

        verifyOppgaveEndretWith(null, "Automatisk jobb ---\\r\\n· Oppgavetype endret fra Vurder dokument til Vurder henvendelse\\r\\n· Oppgave overført fra enhet 9999 til 4806\\r\\n· Saksbehandler endret fra z99123 til ikke valgt\\r\\n\\r\\n\\r\\nEn annen beskrivelse")
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal ikke hente journalpost hvis vurderdokument er avsluttet`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet("4806")
        val oppgaveData = createOppgaveData(12323213, tilordnetRessurs = "z99123", journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "VUR", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AVSLUTTET, beskrivelse = "En annen beskrivelse")
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse(OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_OPPRETTET))

        verifyOppgaveNotEndret()
        verifyDokumentHentet(0)
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal endre vurderdokument oppgavetype til journalforing hvis journalpost status mottatt ved endring`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet("4806")
        stubHentJournalpost(journalpostResponse(journalStatus = JournalpostStatus.MOTTATT))
        val oppgaveData = createOppgaveData(12323213, tilordnetRessurs = "z99123", journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "VUR", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN, beskrivelse = "En annen beskrivelse")
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse())

        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveEndretWith(null, "Oppgave overført fra enhet 9999 til 4806")
        verifyOppgaveEndretWith(null, "En annen beskrivelse")
        verifyOppgaveEndretWith(null, "Saksbehandler endret fra z99123 til ikke valgt")
        verifyOppgaveEndretWith(null, "Oppgavetype endret fra Vurder dokument til Journalføring")
        verifyDokumentHentet()
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal ferdigstille vurder dokument oppgave hvis journalpost status mottatt og har journalforingopppgave`() {
        stubHentOppgaveSok(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    journalpostId = JOURNALPOST_ID_1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "JFR",
                    tema = "BID",
                    tildeltEnhetsnr = "4833"
                )
            )
        )
        stubHentGeografiskEnhet("4806")
        stubHentJournalpost(journalpostResponse(journalStatus = JournalpostStatus.MOTTATT))
        val oppgaveData = createOppgaveData(12323213, tilordnetRessurs = "z99123", journalpostId = JOURNALPOST_ID_1, aktoerId = AKTOER_ID, oppgavetype = "VUR", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN, beskrivelse = "En annen beskrivelse")
        stubHentOppgave(oppgaveData.id, oppgaveData)

        behandleOppgaveHendelseService.behandleOppgaveHendelse(oppgaveData.toHendelse(OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_OPPRETTET))

        verifyOppgaveEndretWith(null, "\"status\":\"FERDIGSTILT\"")
        verifyDokumentHentet()
        verifyOppgaveNotOpprettet()
    }
}
