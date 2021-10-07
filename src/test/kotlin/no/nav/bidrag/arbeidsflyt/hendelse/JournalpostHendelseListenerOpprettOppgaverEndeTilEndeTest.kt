package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OpprettOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.PatchOppgaveJournalpostIdRequest
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity

@SpringBootTest
@DisplayName("JournalpostHendelseListener OPPRETT_OPPGAVE \"ende til ende\"-test")
internal class JournalpostHendelseListenerOpprettOppgaverEndeTilEndeTest {

    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @MockBean
    private lateinit var httpHeaderRestTemplateMock: HttpHeaderRestTemplate

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal opprette oppgave ved hendelse OPPRETT_OPPGAVE`() {
        var oppgaveData = OppgaveData(id=1L, versjon = 1);
        // when/then søk etter oppgave
        @Suppress("UNCHECKED_CAST")
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(OppgaveData::class.java)))
            .thenReturn(ResponseEntity.ok(oppgaveData))

        @Suppress("UNCHECKED_CAST")
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(String::class.java)))
            .thenReturn(ResponseEntity.ok("OK"))

        val journalpostId = "JOARK-2525"
        val aktoerId = "1234567890100"
        // kafka hendelse
        journalpostHendelseListener.lesHendelse(
            """
            {
              "journalpostId":"%s",
              "hendelse":"OPPRETT_OPPGAVE",
              "sporing": {
                "correlationId": "abc"
              },
              "detaljer":{
                "aktoerId": "%s"
              }
            }
            """.trimIndent().format(journalpostId, aktoerId)
        )

        val patchOppgaveJournalpostIdRequest = PatchOppgaveJournalpostIdRequest(oppgaveData, journalpostId)
        val opprettOppgaveRequest = OpprettOppgaveRequest(journalpostId.split("-")[1], aktoerId, "BID")
        verify(httpHeaderRestTemplateMock, times(1)).exchange(anyString(), eq(HttpMethod.POST), eq(opprettOppgaveRequest.somHttpEntity()), eq(OppgaveData::class.java))
        verify(httpHeaderRestTemplateMock, times(1)).exchange(anyString(), eq(HttpMethod.PATCH), eq(patchOppgaveJournalpostIdRequest.somHttpEntity()), eq(String::class.java))
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal opprette oppgaver med tema FAR ved hendelse OPPRETT_OPPGAVE`() {
        var oppgaveData = OppgaveData(id=1L, versjon = 1);
        // when/then søk etter oppgave
        @Suppress("UNCHECKED_CAST")
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(OppgaveData::class.java)))
            .thenReturn(ResponseEntity.ok(oppgaveData))

        @Suppress("UNCHECKED_CAST")
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(String::class.java)))
            .thenReturn(ResponseEntity.ok("OK"))

        val journalpostId = "JOARK-2525"
        val aktoerId = "1234567890100"
        val tema = "FAR"
        // kafka hendelse
        journalpostHendelseListener.lesHendelse(
            """
            {
              "journalpostId":"%s",
              "hendelse":"OPPRETT_OPPGAVE",
              "sporing": {
                "correlationId": "abc"
              },
              "detaljer":{
                "aktoerId": "%s",
                "fagomrade":"%s"
              }
            }
            """.trimIndent().format(journalpostId, aktoerId, tema)
        )

        val opprettOppgaveRequest = OpprettOppgaveRequest("2525", aktoerId, tema)
        val patchOppgaveJournalpostIdRequest = PatchOppgaveJournalpostIdRequest(oppgaveData, journalpostId)
        verify(httpHeaderRestTemplateMock, times(1)).exchange(anyString(), eq(HttpMethod.POST), eq(opprettOppgaveRequest.somHttpEntity()), eq(OppgaveData::class.java))
        verify(httpHeaderRestTemplateMock, times(1)).exchange(anyString(), eq(HttpMethod.PATCH), eq(patchOppgaveJournalpostIdRequest.somHttpEntity()), eq(String::class.java))
    }
}
