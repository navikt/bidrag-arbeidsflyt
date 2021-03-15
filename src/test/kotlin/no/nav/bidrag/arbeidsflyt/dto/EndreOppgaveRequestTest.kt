package no.nav.bidrag.arbeidsflyt.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

internal class EndreOppgaveRequestTest {
    @Test
    fun `skal hente en FerdigstillOppgaveRequest som HttpEntity`() {
        val httpEntity = FerdigstillOppgaveRequest(OppgaveData(id = 1), "BID", "1014").somHttpEntity()

        assertAll(
            { assertThat(httpEntity).isNotEqualTo(FerdigstillOppgaveRequest(OppgaveData(id = 2), "BID", "1014").somHttpEntity()) },
            { assertThat(httpEntity).isEqualTo(FerdigstillOppgaveRequest(OppgaveData(id = 1), "BID", "1014").somHttpEntity()) }
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
}