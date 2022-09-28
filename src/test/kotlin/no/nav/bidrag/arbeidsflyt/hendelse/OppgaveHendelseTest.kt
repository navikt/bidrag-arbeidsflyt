package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveIdentType
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.dto.Oppgavestatuskategori
import no.nav.bidrag.arbeidsflyt.dto.formatterDatoForOppgave
import no.nav.bidrag.arbeidsflyt.service.BehandleOppgaveHendelseService
import no.nav.bidrag.arbeidsflyt.utils.AKTOER_ID
import no.nav.bidrag.arbeidsflyt.utils.BID_JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.DateUtils
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_3
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_4_NEW
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVETYPE_BEH_SAK
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVETYPE_JFR
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_1
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_3
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_5
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_1
import no.nav.bidrag.arbeidsflyt.utils.createJournalpost
import no.nav.bidrag.arbeidsflyt.utils.createOppgave
import no.nav.bidrag.arbeidsflyt.utils.createOppgaveHendelse
import no.nav.bidrag.arbeidsflyt.utils.journalpostResponse
import no.nav.bidrag.dokument.dto.Journalstatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class OppgaveHendelseTest: AbstractBehandleHendelseTest() {

    @Autowired
    lateinit var behandleOppgaveHendelseService: BehandleOppgaveHendelseService

    @Test
    fun `skal lagre oppgave`(){
        val journalpostId = "213213123"
        val oppgaveId = 10123L
        val oppgaveHendelse = createOppgaveHendelse(oppgaveId, journalpostId = journalpostId)
        assertThat(testDataGenerator.hentOppgave(oppgaveId).isPresent).isFalse
        behandleOppgaveHendelseService.behandleOpprettOppgave(oppgaveHendelse)

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
    fun `skal lagre journalforingsoppgave ved endring hvis ikke finnes`(){
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_3)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

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
    fun `skal endre oppgave`(){
        testDataGenerator.opprettOppgave(createOppgave(OPPGAVE_ID_3, journalpostId = "UKJENT"))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_3)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

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
    fun `skal slette oppgave fra databasen nar oppgave lukket`(){
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_3, statuskategori = Oppgavestatuskategori.AVSLUTTET)
        testDataGenerator.opprettOppgave(createOppgave(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_3))

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        val endretOppgaveOptional = testDataGenerator.hentOppgave(OPPGAVE_ID_3)
        assertThat(endretOppgaveOptional.isPresent).isFalse

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal opprette oppgave med BID prefix nar det ikke finnes noen aapne jfr oppgaver men journalpost status er mottatt`(){
        stubHentOppgave(emptyList())
        stubHentGeografiskEnhet()
        testDataGenerator.opprettJournalpost(createJournalpost(BID_JOURNALPOST_ID_1))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = BID_JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET, tildeltEnhetsnr = "4812")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        verifyOppgaveOpprettetWith("\"tildeltEnhetsnr\":\"4812\"", "\"aktoerId\":\"$AKTOER_ID\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${BID_JOURNALPOST_ID_1}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
        verifyHentGeografiskEnhetKalt(0)
    }

    @Test
    fun `skal hente geografisk enhet hvis oppgave ikke har tildelt enhetsnr`(){
        stubHentOppgave(emptyList())
        stubHentGeografiskEnhet("4816")
        testDataGenerator.opprettJournalpost(createJournalpost(BID_JOURNALPOST_ID_1))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = BID_JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET, tildeltEnhetsnr = null)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        verifyOppgaveOpprettetWith("\"tildeltEnhetsnr\":\"4816\"", "\"aktoerId\":\"$AKTOER_ID\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${BID_JOURNALPOST_ID_1}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
        verifyHentGeografiskEnhetKalt(1)
    }


    @Test
    fun `skal opprette oppgave nar det ikke finnes noen aapne jfr oppgaver men journalpost status er mottatt`(){
        stubHentOppgave(listOf(OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_1,
            aktoerId = AKTOER_ID,
            oppgavetype = "BEH_SAK",
            statuskategori = Oppgavestatuskategori.AAPEN,
            tema = "BID",
            tildeltEnhetsnr = "4833"
        )))
        stubHentGeografiskEnhet()
        testDataGenerator.opprettJournalpost(createJournalpost(JOURNALPOST_ID_1))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET, fristFerdigstillelse = LocalDate.of(2020, 2, 1))

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        verifyOppgaveOpprettetWith("\"fristFerdigstillelse\":\"2020-02-01\"", "\"aktoerId\":\"$AKTOER_ID\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"$JOURNALPOST_ID_1\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyHentGeografiskEnhetKalt(0)
    }

    @Test
    fun `skal opprette oppgave med frist neste dag`(){
        stubHentGeografiskEnhet()
        stubHentOppgave(
                listOf(
                    OppgaveData(
                        id = OPPGAVE_ID_1,
                        versjon = 1,
                        journalpostId = JOURNALPOST_ID_1,
                        aktoerId = AKTOER_ID,
                        oppgavetype = "BEH_SAK",
                        statuskategori = Oppgavestatuskategori.AAPEN,
                        tema = "BID",
                        tildeltEnhetsnr = "4833"
                    )
                )
            )
            testDataGenerator.opprettJournalpost(createJournalpost(JOURNALPOST_ID_1))
            val oppgaveHendelse = createOppgaveHendelse(
                OPPGAVE_ID_1,
                journalpostId = JOURNALPOST_ID_1,
                fnr = PERSON_IDENT_1,
                status = OppgaveStatus.FERDIGSTILT,
                statuskategori = Oppgavestatuskategori.AVSLUTTET,
                fristFerdigstillelse = null
            )

            behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

            verifyOppgaveOpprettetWith(
                "\"fristFerdigstillelse\":\"${formatterDatoForOppgave(DateUtils.finnNesteArbeidsdag())}\"",
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
    fun `skal ikke opprette oppgave nar det ikke finnes noen aapne jfr oppgaver men journalpost status er mottatt men ikke har tema bidrag`(){
        stubHentOppgave(listOf(OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_1,
            aktoerId = AKTOER_ID,
            oppgavetype = "BEH_SAK",
            statuskategori = Oppgavestatuskategori.AAPEN,
            tema = "BID",
            tildeltEnhetsnr = "4833"
        )))
        testDataGenerator.opprettJournalpost(createJournalpost(JOURNALPOST_ID_1, tema = "BAR"))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal ikke opprette oppgave nar oppgave ferdigstilt og journalpost status er mottatt men har allerede aapen JFR oppgave`(){
        stubHentOppgave(listOf(OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_1,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        )))
        testDataGenerator.opprettJournalpost(createJournalpost(JOURNALPOST_ID_1))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

       verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal ikke opprette oppgave nar oppgave ferdigstilt og det ikke finnes noen journalposter lagret i databasen`(){
        stubHentOppgave(emptyList())

        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal opprette oppgave nar oppgave endret fra JFR til BEH_SAK og journalpost er mottatt`(){
        stubHentOppgave(emptyList())
        stubHentGeografiskEnhet()
        testDataGenerator.opprettOppgave(createOppgave(OPPGAVE_ID_1))
        testDataGenerator.opprettJournalpost(createJournalpost(JOURNALPOST_ID_1))

        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "BEH_SAK")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        verifyOppgaveOpprettetWith("\"aktoerId\":\"$AKTOER_ID\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"$JOURNALPOST_ID_1\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
    }

    @Test
    fun `skal ikke opprette oppgave nar oppgave endret fra JFR til BEH_SAK og journalpost ikke mottatt`(){
        stubHentOppgave(emptyList())
        testDataGenerator.opprettOppgave(createOppgave(OPPGAVE_ID_1))
        testDataGenerator.opprettJournalpost(createJournalpost(JOURNALPOST_ID_1, status = "J"))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "BEH_SAK")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal ikke opprette oppgave nar oppgave endret fra JFR til BEH_SAK og oppgave ikke er lagret databasen`(){
        stubHentOppgave(emptyList())
        testDataGenerator.opprettJournalpost(createJournalpost(JOURNALPOST_ID_1, status = "J"))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "BEH_SAK")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal overfore oppgave til journalforende enhet hvis oppgave tildelt enhet ikke er knyttet til journalforende enhet`(){
        stubHentOppgave(emptyList())
        stubHentGeografiskEnhet("4806")
        val oppgaveHendelse = createOppgaveHendelse(12323213, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "JFR", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN, beskrivelse = "En annen beskrivelse")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)


        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveEndretWith(1, "Oppgave overført fra enhet 9999 til 4806")
        verifyOppgaveEndretWith(1, "En annen beskrivelse")
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal overfore journalforingsoppgave til journalforende enhet hvis oppgave ikke er paa journalforende enhet`(){
        stubHentOppgave(emptyList())
        stubHentGeografiskEnhet("4806")
        val oppgaveHendelse = createOppgaveHendelse(12323213, tilordnetRessurs = "z99123", journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "JFR", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN, beskrivelse = "En annen beskrivelse")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)


        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveEndretWith(1, "Oppgave overført fra enhet 9999 til 4806")
        verifyOppgaveEndretWith(1, "En annen beskrivelse")
        verifyOppgaveEndretWith(1, "Saksbehandler endret fra z99123 til ikke valgt")
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal overfore Vurder dokument oppgave til journalforende enhet hvis oppgave ikke er paa journalforende enhet`(){
        stubHentOppgave(emptyList())
        stubHentGeografiskEnhet("4806")
        stubHentJournalpost()
        val oppgaveHendelse = createOppgaveHendelse(12323213, tilordnetRessurs = "z99123", journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "VUR", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN, beskrivelse = "En annen beskrivelse")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)


        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveEndretWith(1, "Oppgave overført fra enhet 9999 til 4806")
        verifyOppgaveEndretWith(1, "En annen beskrivelse")
        verifyOppgaveEndretWith(1, "Saksbehandler endret fra z99123 til ikke valgt")
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal ikke overfore oppgave til journalforende enhet hvis oppgave tildelt enhet journalforende enhet`(){
        stubHentOppgave(emptyList())
        stubHentGeografiskEnhet()
        val oppgaveHendelse = createOppgaveHendelse(12323213, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "JFR", tildeltEnhetsnr = "4806", statuskategori = Oppgavestatuskategori.AAPEN)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)


        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveNotEndret()
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal ikke overfore oppgave til journalforende enhet hvis oppgave ikke tilhorer journalforende enhet`(){
        stubHentOppgave(emptyList())
        stubHentGeografiskEnhet("4806")
        val oppgaveHendelse = createOppgaveHendelse(12323213, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "BEH_SAK", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)


        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveNotEndret()
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal ikke overfore oppgave til journalforende enhet hvis oppgave tildelt enhet er fagpost`(){
        stubHentOppgave(emptyList())
        stubHentGeografiskEnhet()
        val oppgaveHendelse = createOppgaveHendelse(12323213, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "JFR", tildeltEnhetsnr = "2950", statuskategori = Oppgavestatuskategori.AAPEN)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)


        verifyOppgaveNotEndret()
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `Skal endre vurder dokument oppgavetype til journalforing hvis journalpost status mottatt`(){
        stubHentOppgave(emptyList())
        stubHentGeografiskEnhet("4806")
        stubHentJournalpost(journalpostResponse(Journalstatus.MOTTATT))
        val oppgaveHendelse = createOppgaveHendelse(12323213, tilordnetRessurs = "z99123", journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "VUR", tildeltEnhetsnr = "9999", statuskategori = Oppgavestatuskategori.AAPEN, beskrivelse = "En annen beskrivelse")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)


        verifyHentJournalforendeEnheterKalt()
        verifyOppgaveEndretWith(null, "Oppgave overført fra enhet 9999 til 4806")
        verifyOppgaveEndretWith(null, "En annen beskrivelse")
        verifyOppgaveEndretWith(null, "Saksbehandler endret fra z99123 til ikke valgt")
        verifyOppgaveEndretWith(null, "Oppgavetype endret fra VUR til JFR")
        verifyDokumentHentet()
        verifyOppgaveNotOpprettet()
    }


}