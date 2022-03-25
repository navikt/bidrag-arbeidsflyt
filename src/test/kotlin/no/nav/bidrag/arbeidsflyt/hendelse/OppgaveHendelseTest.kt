package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveIdentType
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
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
import no.nav.bidrag.arbeidsflyt.utils.createOppgaveHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
            assertThat(oppgave.ident).isEqualTo(PERSON_IDENT_1)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_JFR)
            assertThat(oppgave.tema).isEqualTo("BID")
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.OPPRETTET.name)
        }
    }

    @Test
    fun `skal lagre oppgave med bnr`(){
        val journalpostId = "213213123"
        val oppgaveId = 5L
        val bnr = "123213123"
        val oppgaveHendelse = createOppgaveHendelse(oppgaveId, journalpostId = journalpostId, identVerdi = bnr, identType = OppgaveIdentType.BNR)

        behandleOppgaveHendelseService.behandleOpprettOppgave(oppgaveHendelse)

        val opprettetOppgaveOptional = testDataGenerator.hentOppgave(oppgaveId)
        assertThat(opprettetOppgaveOptional.isPresent).isTrue

        assertThat(opprettetOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(oppgaveId)
            assertThat(oppgave.journalpostId).isEqualTo(journalpostId)
            assertThat(oppgave.ident).isEqualTo(bnr)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_JFR)
            assertThat(oppgave.tema).isEqualTo("BID")
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.OPPRETTET.name)
        }
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal endre oppgave`(){
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_3, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        val endretOppgaveOptional = testDataGenerator.hentOppgave(OPPGAVE_ID_3)
        assertThat(endretOppgaveOptional.isPresent).isTrue

        assertThat(endretOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(OPPGAVE_ID_3)
            assertThat(oppgave.journalpostId).isEqualTo(JOURNALPOST_ID_3)
            assertThat(oppgave.ident).isEqualTo(PERSON_IDENT_1)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_JFR)
            assertThat(oppgave.tema).isEqualTo("BID")
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.FERDIGSTILT.name)
        }

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal opprette oppgave med BID prefix nar oppgave ferdigstilt men journalpost status er mottatt`(){
        stubHentOppgave(emptyList())
        val journalpostIdWithoutPrefix = BID_JOURNALPOST_ID_1.replace("BID-", "")
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_5, journalpostId = journalpostIdWithoutPrefix, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        val endretOppgaveOptional = testDataGenerator.hentOppgave(OPPGAVE_ID_5)
        assertThat(endretOppgaveOptional.isPresent).isTrue

        assertThat(endretOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(OPPGAVE_ID_5)
            assertThat(oppgave.journalpostId).isEqualTo(journalpostIdWithoutPrefix)
            assertThat(oppgave.ident).isEqualTo(PERSON_IDENT_1)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_JFR)
            assertThat(oppgave.tema).isEqualTo("BID")
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.FERDIGSTILT.name)
        }

        verifyOppgaveOpprettetWith("\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${journalpostIdWithoutPrefix}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
    }

    @Test
    fun `skal opprette oppgave nar oppgave ferdigstilt men journalpost status er mottatt`(){
        stubHentOppgave(listOf(OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_1,
            aktoerId = AKTOER_ID,
            oppgavetype = "BEH_SAK",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        )))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        val endretOppgaveOptional = testDataGenerator.hentOppgave(OPPGAVE_ID_1)
        assertThat(endretOppgaveOptional.isPresent).isTrue

        assertThat(endretOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(OPPGAVE_ID_1)
            assertThat(oppgave.ident).isEqualTo(PERSON_IDENT_1)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_JFR)
            assertThat(oppgave.tema).isEqualTo("BID")
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.FERDIGSTILT.name)
        }

        verifyOppgaveOpprettetWith("\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"$JOURNALPOST_ID_1\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
    }

    @Test
    fun `skal ikke opprette oppgave nar oppgave ferdigstilt og journalpost status er mottatt men har allerede åpen JFR oppgave`(){
        stubHentOppgave(listOf(OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_1,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        )))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        val endretOppgaveOptional = testDataGenerator.hentOppgave(OPPGAVE_ID_1)
        assertThat(endretOppgaveOptional.isPresent).isTrue

        assertThat(endretOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(OPPGAVE_ID_1)
            assertThat(oppgave.ident).isEqualTo(PERSON_IDENT_1)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_JFR)
            assertThat(oppgave.tema).isEqualTo("BID")
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.FERDIGSTILT.name)
        }

       verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal opprette oppgave nar oppgave endret fra JFR til BEH_SAK og journalpost er mottat`(){
        stubHentOppgave(emptyList())
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "BEH_SAK")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        val endretOppgaveOptional = testDataGenerator.hentOppgave(OPPGAVE_ID_1)
        assertThat(endretOppgaveOptional.isPresent).isTrue

        assertThat(endretOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(OPPGAVE_ID_1)
            assertThat(oppgave.ident).isEqualTo(PERSON_IDENT_1)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_BEH_SAK)
            assertThat(oppgave.tema).isEqualTo("BID")
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.OPPRETTET.name)
        }

        verifyOppgaveOpprettetWith("\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"$JOURNALPOST_ID_1\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
    }

    @Test
    fun `skal ikke opprette oppgave nar oppgave endret fra JFR til BEH_SAK og journalpost ikke mottat`(){
        stubHentOppgave(emptyList())
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_3, fnr = PERSON_IDENT_1, oppgavetype = "BEH_SAK")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        val endretOppgaveOptional = testDataGenerator.hentOppgave(OPPGAVE_ID_3)
        assertThat(endretOppgaveOptional.isPresent).isTrue

        assertThat(endretOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(OPPGAVE_ID_3)
            assertThat(oppgave.ident).isEqualTo(PERSON_IDENT_1)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_BEH_SAK)
            assertThat(oppgave.tema).isEqualTo("BID")
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.OPPRETTET.name)
        }

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal ikke lagre oppgave som ikke har type JFR`(){
        stubHentOppgave(emptyList())
        val oppgaveId = 500L
        val oppgaveHendelse = createOppgaveHendelse(oppgaveId, journalpostId = JOURNALPOST_ID_4_NEW, fnr = PERSON_IDENT_1, oppgavetype = "BEH_SAK")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        val endretOppgaveOptional = testDataGenerator.hentOppgave(oppgaveId)
        assertThat(endretOppgaveOptional.isPresent).isFalse

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal oppdatere oppgave som har type JFR men har endret type`(){
        stubHentOppgave(emptyList())
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_4_NEW, fnr = PERSON_IDENT_1, oppgavetype = "BEH_SAK")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        val endretOppgaveOptional = testDataGenerator.hentOppgave(OPPGAVE_ID_1)
        assertThat(endretOppgaveOptional.isPresent).isTrue

        assertThat(endretOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(OPPGAVE_ID_1)
            assertThat(oppgave.ident).isEqualTo(PERSON_IDENT_1)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_BEH_SAK)
            assertThat(oppgave.tema).isEqualTo("BID")
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.OPPRETTET.name)
        }

        verifyOppgaveNotOpprettet()
    }

    @Test
    @Disabled
    fun `skal ikke opprette oppgave nar oppgave ikke er JFR men det ikke finnes noe oppgave lagret fra for`(){
        stubHentOppgave(emptyList())
        val oppgaveId = 500L
        val oppgaveHendelse = createOppgaveHendelse(oppgaveId, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, oppgavetype = "BEH_SAK")

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        val endretOppgaveOptional = testDataGenerator.hentOppgave(oppgaveId)
        assertThat(endretOppgaveOptional.isPresent).isTrue

        assertThat(endretOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(oppgaveId)
            assertThat(oppgave.ident).isEqualTo(PERSON_IDENT_1)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_BEH_SAK)
            assertThat(oppgave.tema).isEqualTo("BID")
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.OPPRETTET.name)
        }

        verifyOppgaveNotOpprettet()
    }
}