package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.utils.AKTOER_ID
import no.nav.bidrag.arbeidsflyt.utils.BID_JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.BID_JOURNALPOST_ID_3_NEW
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_1
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_1
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_3
import no.nav.bidrag.arbeidsflyt.utils.createJournalpostHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
            assertThat(journalpost.gjelderId).isEqualTo(PERSON_IDENT_1)
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
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_1, status = "J", sporingEnhet = "1234")

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val journalpostOptional = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_1)
        assertThat(journalpostOptional.isPresent).isTrue

        assertThat(journalpostOptional).hasValueSatisfying { journalpost ->
            assertThat(journalpost.journalpostId).isEqualTo(BID_JOURNALPOST_ID_1)
            assertThat(journalpost.gjelderId).isEqualTo(PERSON_IDENT_1)
            assertThat(journalpost.status).isEqualTo("J")
            assertThat(journalpost.tema).isEqualTo("BID")
            assertThat(journalpost.enhet).isEqualTo("4833")
        }

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
            assertThat(journalpost.gjelderId).isEqualTo(PERSON_IDENT_3)
            assertThat(journalpost.status).isEqualTo("M")
            assertThat(journalpost.tema).isEqualTo("BID")
            assertThat(journalpost.enhet).isEqualTo("4833")
        }

        verifyOppgaveOpprettetWith("\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${BID_JOURNALPOST_ID_3_NEW.replace("BID-", "")}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveEndretWith(1, BID_JOURNALPOST_ID_3_NEW)
    }

}
