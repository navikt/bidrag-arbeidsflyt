package no.nav.bidrag.arbeidsflyt.dto

import no.nav.bidrag.arbeidsflyt.model.BIDRAG
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

internal class PatchOppgaveRequestTest {
    @Test
    fun `skal hente en FerdigstillOppgaveRequest som HttpEntity`() {
        val httpEntity = FerdigstillOppgaveRequest(OppgaveData(id = 1), BIDRAG, "1014").somHttpEntity()

        assertAll(
            { assertThat(httpEntity).isNotEqualTo(FerdigstillOppgaveRequest(OppgaveData(id = 2), BIDRAG, "1014").somHttpEntity()) },
            { assertThat(httpEntity).isEqualTo(FerdigstillOppgaveRequest(OppgaveData(id = 1), BIDRAG, "1014").somHttpEntity()) }
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
        val opprinneligOppgave = OppgaveData(status = "AAPEN", tema = BIDRAG, tildeltEnhetsnr = "007")
        val ferdigstillOppgaveRequest = FerdigstillOppgaveRequest(oppgaveData = opprinneligOppgave, "farskap", "1001")
        val overforOppgaveRequest = OverforOppgaveRequest(oppgaveData = opprinneligOppgave, "666")

        assertAll(
            { assertThat(opprinneligOppgave.status).`as`("opprinnelig status").isEqualTo("AAPEN")},
            { assertThat(opprinneligOppgave.tema).`as`("opprinnelig tema").isEqualTo(BIDRAG)},
            { assertThat(opprinneligOppgave.tildeltEnhetsnr).`as`("opprinnelig tildelt enhetsnummer").isEqualTo("007")},
            { assertThat(ferdigstillOppgaveRequest.tema).`as`("ferdigstillt tema").isEqualTo("farskap")},
            { assertThat(ferdigstillOppgaveRequest.tildeltEnhetsnr).`as`("ferdigstillt enhetsnummer").isEqualTo("1001")},
            { assertThat(overforOppgaveRequest.tildeltEnhetsnr).`as`("ferdigstillt enhetsnummer").isEqualTo("666")}
        )
    }
}