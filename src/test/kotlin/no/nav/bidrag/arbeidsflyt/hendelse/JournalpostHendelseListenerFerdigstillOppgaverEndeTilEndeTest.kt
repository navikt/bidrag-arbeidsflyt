package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity

@SpringBootTest
@DisplayName("JournalpostHendelseListener ved ferdigstilling av oppgaver \"ende til ende\"-test")
internal class JournalpostHendelseListenerFerdigstillOppgaverEndeTilEndeTest {

    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @MockBean
    private lateinit var httpHeaderRestTemplate: HttpHeaderRestTemplate

    @Test
    fun `skal ferdigstille oppgave for hendelse JOURNALFOR_JOURNALPOST`() {
        // when/then søk etter oppgave
        @Suppress("UNCHECKED_CAST")
        whenever(httpHeaderRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OppgaveSokResponse::class.java)))
            .thenReturn(
                ResponseEntity.ok(
                    OppgaveSokResponse(antallTreffTotalt = 1, listOf(OppgaveData(id = 1)))
                )
            ).thenReturn(
                ResponseEntity.noContent().build()
            )

        // when/then ferdigstill oppgave
        whenever(httpHeaderRestTemplate.exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(String::class.java)))
            .thenReturn(
                ResponseEntity.ok("bullseye")
            )

        // kafka hendelse
        journalpostHendelseListener.lesHendelse(
            """
            {
              "journalpostId":"BID-1",
              "hendelse":"JOURNALFOR_JOURNALPOST",
              "sporing": {
                "correlationId": "xyz"
              },
              "detaljer":{
                "enhetsnummer":"1001"
              }
            }
            """.trimIndent()
        )

        verify(httpHeaderRestTemplate).exchange(eq("/api/v1/oppgaver/1"), eq(HttpMethod.PATCH), any(), eq(String::class.java))
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal ikke ferdigstille oppgave for hendelse AVVIK_ENDRE_FAGOMRADE når fagområde som endres er internt`() {
        // kafka hendelse
        journalpostHendelseListener.lesHendelse(
            """
            {
              "journalpostId":"BID-2",
              "hendelse":"AVVIK_ENDRE_FAGOMRADE",
              "sporing": {
                "correlationId": "abc"
              },
              "detaljer":{
                "enhetsnummer":"1001",
                "gammeltFagomrade": "BID",
                "nyttFagomrade": "FAR"
              }
            }
            """.trimIndent()
        )

        verify(httpHeaderRestTemplate, never()).exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(String::class.java))
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal ferdigstille oppgave for hendelse AVVIK_ENDRE_FAGOMRADE når fagområde som endres er eksternt`() {
        // when/then søk etter oppgave
        @Suppress("UNCHECKED_CAST")
        whenever(httpHeaderRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OppgaveSokResponse::class.java)))
            .thenReturn(
                ResponseEntity.ok(
                    OppgaveSokResponse(antallTreffTotalt = 1, listOf(OppgaveData(id = 6)))
                )
            ).thenReturn(
                ResponseEntity.noContent().build()
            )

        // when/then ferdigstill oppgave
        whenever(httpHeaderRestTemplate.exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(String::class.java)))
            .thenReturn(
                ResponseEntity.ok("bullseye")
            )

        // kafka hendelse
        journalpostHendelseListener.lesHendelse(
            """
            {
              "journalpostId":"BID-2",
              "hendelse":"AVVIK_ENDRE_FAGOMRADE",
              "sporing": {
                "correlationId": "abc"
              },
              "detaljer":{
                "enhetsnummer":"1001",
                "fagomrade": "PEN"
              }
            }
            """.trimIndent()
        )

        verify(httpHeaderRestTemplate).exchange(eq("/api/v1/oppgaver/6"), eq(HttpMethod.PATCH), any(), eq(String::class.java))
    }
}
