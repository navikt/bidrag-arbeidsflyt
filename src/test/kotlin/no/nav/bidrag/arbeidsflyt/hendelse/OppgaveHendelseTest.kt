package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveIdentType
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.dto.Oppgavestatuskategori
import no.nav.bidrag.arbeidsflyt.service.BehandleOppgaveHendelseService
import no.nav.bidrag.arbeidsflyt.utils.AKTOER_ID
import no.nav.bidrag.arbeidsflyt.utils.BID_JOURNALPOST_ID_1
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
import no.nav.bidrag.arbeidsflyt.utils.createOppgaveHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OppgaveHendelseTest: AbstractBehandleHendelseTest() {

    @Autowired
    lateinit var behandleOppgaveHendelseService: BehandleOppgaveHendelseService

    @Test
    fun `skal opprette oppgave med BID prefix nar det ikke finnes noen aapne jfr oppgaver men journalpost status er mottatt`(){
        stubHentOppgave(emptyList())
        val journalpostIdWithoutPrefix = BID_JOURNALPOST_ID_1.replace("BID-", "")
        testDataGenerator.opprettJournalpost(createJournalpost(BID_JOURNALPOST_ID_1, gjelderId = PERSON_IDENT_1))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = journalpostIdWithoutPrefix, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        verifyOppgaveOpprettetWith("\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${journalpostIdWithoutPrefix}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
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
        testDataGenerator.opprettJournalpost(createJournalpost(JOURNALPOST_ID_1, gjelderId = PERSON_IDENT_1))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        verifyOppgaveOpprettetWith("\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"$JOURNALPOST_ID_1\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
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
        testDataGenerator.opprettJournalpost(createJournalpost(JOURNALPOST_ID_1, gjelderId = PERSON_IDENT_1, tema = "BAR"))
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
        testDataGenerator.opprettJournalpost(createJournalpost(JOURNALPOST_ID_1, gjelderId = PERSON_IDENT_1))
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
        testDataGenerator.opprettJournalpost(createJournalpost(JOURNALPOST_ID_1, gjelderId = PERSON_IDENT_1))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "BEH_SAK")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        verifyOppgaveOpprettetWith("\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"$JOURNALPOST_ID_1\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
    }

    @Test
    fun `skal ikke opprette oppgave nar oppgave endret fra JFR til BEH_SAK og journalpost ikke mottatt`(){
        stubHentOppgave(emptyList())
        testDataGenerator.opprettJournalpost(createJournalpost(JOURNALPOST_ID_1, gjelderId = PERSON_IDENT_1, status = "J"))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "BEH_SAK")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        verifyOppgaveNotOpprettet()
    }
}