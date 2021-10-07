package no.nav.bidrag.arbeidsflyt.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.bidrag.arbeidsflyt.model.DetaljVerdi.FAGOMRADE_BIDRAG
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

internal class PatchOppgaveRequestTest {
    @Test
    fun `skal hente en FerdigstillOppgaveRequest som HttpEntity`() {
        val httpEntity = FerdigstillOppgaveRequest(OppgaveData(id = 1), FAGOMRADE_BIDRAG, "1014").somHttpEntity()

        assertAll(
            { assertThat(httpEntity).isNotEqualTo(FerdigstillOppgaveRequest(OppgaveData(id = 2), FAGOMRADE_BIDRAG, "1014").somHttpEntity()) },
            { assertThat(httpEntity).isEqualTo(FerdigstillOppgaveRequest(OppgaveData(id = 1), FAGOMRADE_BIDRAG, "1014").somHttpEntity()) }
        )
    }

    @Test
    fun `skal hente en OverforOppgaveRequest som HttpEntity`() {
        val httpEntity = OverforOppgaveRequest(OppgaveData(id = 1), "1015").somHttpEntity()

        assertAll(
            { assertThat(httpEntity).isNotEqualTo(OverforOppgaveRequest(OppgaveData(id = 2), "1015").somHttpEntity()) },
            { assertThat(httpEntity).isEqualTo(OverforOppgaveRequest(OppgaveData(id = 1), "1015").somHttpEntity()) }
        )
    }

    @Test
    fun `skal endre felt for patch request`() {
        val opprinneligOppgave = OppgaveData(status = "AAPEN", tema = FAGOMRADE_BIDRAG, tildeltEnhetsnr = "007")
        val ferdigstillOppgaveRequest = FerdigstillOppgaveRequest(oppgaveData = opprinneligOppgave, "farskap", "1001")
        val overforOppgaveRequest = OverforOppgaveRequest(oppgaveData = opprinneligOppgave, "666")

        assertAll(
            { assertThat(opprinneligOppgave.status).`as`("opprinnelig status").isEqualTo("AAPEN")},
            { assertThat(opprinneligOppgave.tema).`as`("opprinnelig tema").isEqualTo(FAGOMRADE_BIDRAG)},
            { assertThat(opprinneligOppgave.tildeltEnhetsnr).`as`("opprinnelig tildelt enhetsnummer").isEqualTo("007")},
            { assertThat(ferdigstillOppgaveRequest.tema).`as`("ferdigstillt tema").isEqualTo("farskap")},
            { assertThat(ferdigstillOppgaveRequest.tildeltEnhetsnr).`as`("ferdigstillt enhetsnummer").isEqualTo("1001")},
            { assertThat(overforOppgaveRequest.tildeltEnhetsnr).`as`("ferdigstillt enhetsnummer").isEqualTo("666")}
        )
    }

    @Test
    fun `skal serialisere UpdateOppgaveAfterOpprettRequest`() {
        val opprinneligOppgave = OppgaveData(status = "AAPEN", tema = FAGOMRADE_BIDRAG, tildeltEnhetsnr = "007", versjon = 2, id=1)
        val updateOppgaveAfterOpprettRequest = UpdateOppgaveAfterOpprettRequest(opprinneligOppgave, "JOARK-test")
        val stringValue = jacksonObjectMapper().writer().writeValueAsString(updateOppgaveAfterOpprettRequest)
        assertThat(stringValue).`as`("Expected json string value").isEqualTo("{\"journalpostId\":\"JOARK-test\",\"id\":1,\"versjon\":2}")
    }

    @Test
    fun `skal serialisere FerdigstillOppgaveRequest`() {
        val opprinneligOppgave = OppgaveData(status = "AAPEN", tema = FAGOMRADE_BIDRAG, tildeltEnhetsnr = "007", versjon = 2, id=1)
        val ferdigstillOppgaveRequest = FerdigstillOppgaveRequest(opprinneligOppgave, "BID", "4806")
        val stringValue = jacksonObjectMapper().writer().writeValueAsString(ferdigstillOppgaveRequest)
        assertThat(stringValue).`as`("Expected json string value").isEqualTo("{\"tema\":\"BID\",\"status\":\"FERDIGSTILLT\",\"tildeltEnhetsnr\":\"4806\",\"id\":1,\"versjon\":2,\"prioritet\":\"HOY\"}")
    }

    @Test
    fun `skal serialisere OverforOppgaveRequest`() {
        val opprinneligOppgave = OppgaveData(status = "AAPEN", tema = FAGOMRADE_BIDRAG, tildeltEnhetsnr = "007", versjon = 2, id=1)
        val overforOppgaveRequest = OverforOppgaveRequest(opprinneligOppgave, "4812")
        val stringValue = jacksonObjectMapper().writer().writeValueAsString(overforOppgaveRequest)
        assertThat(stringValue).`as`("Expected json string value").isEqualTo("{\"tildeltEnhetsnr\":\"4812\",\"id\":1,\"versjon\":2,\"prioritet\":\"HOY\",\"status\":\"AAPEN\",\"tema\":\"BID\"}")
    }
}