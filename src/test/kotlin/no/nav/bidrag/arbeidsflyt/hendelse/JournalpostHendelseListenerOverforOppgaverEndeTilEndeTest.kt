package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
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
        Mockito.`when`(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OppgaveSokResponse::class.java)))
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
                "gammelt-enhetsnummer":"1001"
              }
            }
            """.trimIndent()
        )

        verify(httpHeaderRestTemplateMock, times(2)).exchange(anyString(), eq(HttpMethod.GET), any(), eq(OppgaveSokResponse::class.java))
    }
}
