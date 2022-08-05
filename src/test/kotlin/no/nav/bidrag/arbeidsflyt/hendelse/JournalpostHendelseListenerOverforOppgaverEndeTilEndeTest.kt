package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.dto.OverforOppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.OppgaveDataForHendelse
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@DisplayName("JournalpostHendelseListener overfør oppgaver \"ende til ende\"-test")
@ActiveProfiles("test")
internal class JournalpostHendelseListenerOverforOppgaverEndeTilEndeTest {

    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @MockBean
    private lateinit var httpHeaderRestTemplateMock: HttpHeaderRestTemplate

    @Test
    fun `skal endre oppgavens enhet`() {

        // when/then - finner en oppgave når en søker etter oppgaver basert på journalpost Id
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OppgaveSokResponse::class.java)))
            .thenReturn(
                ResponseEntity.ok(OppgaveSokResponse(antallTreffTotalt = 1, listOf(OppgaveData(tildeltEnhetsnr = "1001", id = 6))))
            )

        // when/then ferdigstill oppgave
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(OppgaveData::class.java)))
            .thenReturn(
                ResponseEntity.ok(OppgaveData(1))
            )

        // kafka hendelse
        journalpostHendelseListener.lesHendelse(
            """
            {
              "journalpostId":"BID-666",
              "enhet":"1010",
              "sporing": {
                "correlationId": "abc",
                "enhetsnummer": "1234"
              }
            }
            """.trimIndent()
        )

        val overforOppgaveRequest = OverforOppgaveRequest(
            OppgaveDataForHendelse(
                OppgaveData(tildeltEnhetsnr = "1010", id = 6)
            ), "1010", "Z9999"
        )

        overforOppgaveRequest.endretAvEnhetsnr = "1234"

        verify(httpHeaderRestTemplateMock).exchange(
            eq("/api/v1/oppgaver/6"),
            eq(HttpMethod.PATCH),
            eq(overforOppgaveRequest.somHttpEntity()),
            eq(OppgaveData::class.java)
        )
    }
}
