package no.nav.bidrag.arbeidsflyt.aop

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import no.nav.bidrag.arbeidsflyt.PROFILE_TEST
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.commons.CorrelationId
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@DisplayName("HendelseCorrelationAspect")
@EnableMockOAuth2Server
@AutoConfigureWireMock(port = 0)
@ActiveProfiles(PROFILE_TEST)
internal class HendelseCorrelationAspectTest {
    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @Test
    fun `skal spore CorrelationId fra JournalpostHendelse`() {
        val oppgaveSokResponse =
            """
            {
                "antallTreffTotalt": 0,
                "oppgaver": []
            }
            """.trimIndent()

        stubFor(
            get(urlMatching("/oppgave/api/v1/oppgaver/\\?.*")).willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .withStatus(HttpStatus.OK.value())
                    .withBody(oppgaveSokResponse),
            ),
        )

        val hendelse =
            """
            {
              "journalpostId":"BID-101",
              "sporing": {
                "correlationId":"test.av.correlation.id"
              }
            }
            """.trimIndent()

        journalpostHendelseListener.prosesserHendelse(hendelse)

        assertThat(CorrelationId.fetchCorrelationIdForThread()).isEqualTo("test.av.correlation.id")
    }
}
