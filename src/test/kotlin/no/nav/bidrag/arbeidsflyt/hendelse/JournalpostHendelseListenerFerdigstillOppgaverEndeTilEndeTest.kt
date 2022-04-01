package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.model.Fagomrade
import no.nav.bidrag.arbeidsflyt.model.JOURNALFORINGSOPPGAVE
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
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@DisplayName("JournalpostHendelseListener ved ferdigstilling av oppgaver \"ende til ende\"-test")
@ActiveProfiles("test")
internal class JournalpostHendelseListenerFerdigstillOppgaverEndeTilEndeTest {

    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @MockBean
    private lateinit var httpHeaderRestTemplateMock: HttpHeaderRestTemplate

    @Test
    fun `skal ferdigstille oppgave når fagomrade endres til et eksternt`() {
        // when/then søk etter oppgave
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OppgaveSokResponse::class.java))).thenReturn(
            ResponseEntity.ok(OppgaveSokResponse(antallTreffTotalt = 1, listOf(OppgaveData(id = 1, tema = Fagomrade.BIDRAG, oppgavetype = "BEH_SAK"),
                OppgaveData(id = 2, tema = Fagomrade.BIDRAG, oppgavetype = JOURNALFORINGSOPPGAVE))))
        )

        // when/then ferdigstill oppgave
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(OppgaveData::class.java)))
            .thenReturn(
                ResponseEntity.ok(OppgaveData())
            )

        // kafka hendelse
        journalpostHendelseListener.lesHendelse(
            """
            {
              "journalpostId":"BID-1",
              "fagomrade":"EKSTERNT",
              "sporing": {
                "correlationId": "xyz"
              }
            }
            """.trimIndent()
        )

        verify(httpHeaderRestTemplateMock).exchange(eq("/api/v1/oppgaver/2"), eq(HttpMethod.PATCH), any(), eq(OppgaveData::class.java))
        verify(httpHeaderRestTemplateMock, never()).exchange(eq("/api/v1/oppgaver/1"), eq(HttpMethod.PATCH), any(), eq(OppgaveData::class.java))
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal ikke ferdigstille oppgave når fagområde som endres er internt`() {
        // when/then søk etter oppgave
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OppgaveSokResponse::class.java))).thenReturn(
            ResponseEntity.ok(OppgaveSokResponse(antallTreffTotalt = 1, listOf(OppgaveData(id = 1, tema = Fagomrade.BIDRAG))))
        )

        // kafka hendelse
        journalpostHendelseListener.lesHendelse(
            """
            {
              "journalpostId":"BID-2",
              "fagomrade": "FAR",
              "sporing": {
                "correlationId": "abc"
              }
            }
            """.trimIndent()
        )

        verify(httpHeaderRestTemplateMock, never()).exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(OppgaveData::class.java))
    }
}
