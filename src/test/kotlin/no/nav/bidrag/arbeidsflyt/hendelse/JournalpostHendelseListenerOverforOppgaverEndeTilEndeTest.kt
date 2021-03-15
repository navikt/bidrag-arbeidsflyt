package no.nav.bidrag.arbeidsflyt.hendelse

import com.nhaarman.mockito_kotlin.whenever
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.dto.OverforOppgaveRequest
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity

@SpringBootTest(properties = ["OPPGAVE_URL=https://unit.test"])
@DisplayName("JournalpostHendelseListener AVVIK_OVERFOR_TIL_ANNEN_ENHET \"ende til ende\"-test")
internal class JournalpostHendelseListenerOverforOppgaverEndeTilEndeTest {

    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @MockBean
    private lateinit var httpHeaderRestTemplateMock: HttpHeaderRestTemplate

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal søke etter oppgaver som må overføres til annen enhet ved AVVIK_OVERFOR_TIL_ANNEN_ENHET`() {

        // when/then søk etter oppgave
        @Suppress("UNCHECKED_CAST")
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OppgaveSokResponse::class.java)))
            .thenReturn(
                ResponseEntity.ok(OppgaveSokResponse(antallTreffTotalt = 0))
            )

        // kafka hendelse
        journalpostHendelseListener.lesHendelse(
            """
            {
              "journalpostId":"BID-2",
              "hendelse":"AVVIK_OVERFOR_TIL_ANNEN_ENHET",
              "sporing": {
                "correlationId": "abc"
              },
              "detaljer":{
                "nytt-enhetsnummer":"1001",
                "gammelt-enhetsnummer":"1010"
              }
            }
            """.trimIndent()
        )

        verify(httpHeaderRestTemplateMock, times(2)).exchange(anyString(), eq(HttpMethod.GET), any(), eq(OppgaveSokResponse::class.java))
    }

    @Test
    fun `skal endre oppgavens enhet fra gammelt nummer til nytt`() {

        // when/then - finner en oppgave når en søker etter oppgaver basert på journalpost Id
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OppgaveSokResponse::class.java)))
            .thenReturn(
                ResponseEntity.ok(
                    OppgaveSokResponse(antallTreffTotalt = 1, listOf(OppgaveData(tildeltEnhetsnr = "1010", id = 6)))
                )
            ).thenReturn(
                ResponseEntity.ok(OppgaveSokResponse(antallTreffTotalt = 0))
            )

        // when/then ferdigstill oppgave
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.PUT), any(), eq(String::class.java)))
            .thenReturn(
                ResponseEntity.ok("endret")
            )

        // kafka hendelse
        journalpostHendelseListener.lesHendelse(
            """
            {
              "journalpostId":"BID-666",
              "hendelse":"AVVIK_OVERFOR_TIL_ANNEN_ENHET",
              "sporing": {
                "correlationId": "abc"
              },
              "detaljer":{
                "nytt-enhetsnummer":"1001",
                "gammelt-enhetsnummer":"1010"
              }
            }
            """.trimIndent()
        )

        verify(httpHeaderRestTemplateMock).exchange(
            eq("/api/v1/oppgaver/6"),
            eq(HttpMethod.PUT),
            eq(OverforOppgaveRequest(OppgaveData(tildeltEnhetsnr = "1010", id = 6), "1001").somHttpEntity()),
            eq(String::class.java)
        )
    }
}
