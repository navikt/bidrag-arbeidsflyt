package no.nav.bidrag.arbeidsflyt.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.bidrag.arbeidsflyt.model.Fagomrade
import no.nav.bidrag.arbeidsflyt.model.OppgaveDataForHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PatchOppgaveRequestTest {

    @Test
    fun `skal serialisere UpdateOppgaveAfterOpprettRequest`() {
        val opprinneligOppgave = OppgaveDataForHendelse(
            OppgaveData(status = OppgaveStatus.AAPNET, tema = Fagomrade.BIDRAG, tildeltEnhetsnr = "007", versjon = 2, id = 1)
        )

        val updateOppgaveAfterOpprettRequest = UpdateOppgaveAfterOpprettRequest(opprinneligOppgave, "JOARK-test")
        val stringValue = jacksonObjectMapper().writer().writeValueAsString(updateOppgaveAfterOpprettRequest)
        val expectedValue = "{\"journalpostId\":\"JOARK-test\",\"id\":1,\"versjon\":2}"

        assertThat(stringValue).`as`("Expected json string value").contains(expectedValue)
    }

    @Test
    fun `skal serialisere FerdigstillOppgaveRequest`() {
        val opprinneligOppgave = OppgaveDataForHendelse(
            OppgaveData(status = OppgaveStatus.AAPNET, tema = Fagomrade.BIDRAG, tildeltEnhetsnr = "007", versjon = 2, id = 1)
        )

        val ferdigstillOppgaveRequest = FerdigstillOppgaveRequest(opprinneligOppgave)
        val stringValue = jacksonObjectMapper().writer().writeValueAsString(ferdigstillOppgaveRequest)

        val expectedValue = "{" +
                "\"status\":\"FERDIGSTILT\"," +
                "\"id\":1," +
                "\"versjon\":2" +
                "}"

        assertThat(stringValue).`as`("Expected json string value").contains(expectedValue)
    }

    @Test
    fun `skal serialisere OverforOppgaveRequest`() {
        val opprinneligOppgave = OppgaveDataForHendelse(
            OppgaveData(status = OppgaveStatus.AAPNET, tema = Fagomrade.BIDRAG, tildeltEnhetsnr = "007", versjon = 2, id = 1)
        )
        val overforOppgaveRequest = OverforOppgaveRequest(opprinneligOppgave, "4812", "Z999999")
        val stringValue = jacksonObjectMapper().writer().writeValueAsString(overforOppgaveRequest)
        val expectedValue = "" +
                "{\"tildeltEnhetsnr\":\"4812\"," +
                "\"id\":1," +
                "\"versjon\":2," +
                "\"tilordnetRessurs\":\"\""

        assertThat(stringValue).`as`("Expected json string value").contains(expectedValue)
        assertThat(stringValue).`as`("Expected json string value").contains("Oppgave overført fra enhet 007 til 4812")
        assertThat(stringValue).`as`("Expected json string value").contains("Saksbehandler endret fra Z999999 til ikke valgt")
    }

    @Test
    fun `skal serialisere OpprettOppgaveRequest`() {
        val opprettOppgaveRequest = OpprettJournalforingsOppgaveRequest("1234", "123213")
        val stringValue = jacksonObjectMapper().writer().writeValueAsString(opprettOppgaveRequest)

        assertThat(stringValue).`as`("Expected json string value")
            .containsPattern("\"opprettetAvEnhetsnr\":\"9999\"")
            .containsPattern("\"prioritet\":\"HOY\"")
            .containsPattern("\"oppgavetype\":\"JFR\"")
            .containsPattern("\"tema\":\"BID\"")
            .containsPattern("\"aktoerId\":\"123213\"")
            .containsPattern("\"journalpostId\":\"1234\"")
    }
}
