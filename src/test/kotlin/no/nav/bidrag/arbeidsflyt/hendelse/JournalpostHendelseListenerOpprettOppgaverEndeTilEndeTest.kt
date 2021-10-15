package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.dto.OpprettOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.UpdateOppgaveAfterOpprettRequest
import no.nav.bidrag.arbeidsflyt.model.Journalstatus
import no.nav.bidrag.arbeidsflyt.model.OppgaveDataForHendelse
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity

@SpringBootTest
@DisplayName("JournalpostHendelseListener opprett oppgaver \"ende til ende\"-test")
internal class JournalpostHendelseListenerOpprettOppgaverEndeTilEndeTest {

    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @MockBean
    private lateinit var httpHeaderRestTemplateMock: HttpHeaderRestTemplate

    @Test
    fun `skal opprette oppgave når journalposten er mottatt og det finnes en aktor på hendelsen`() {
        val oppgaveData = OppgaveData(id = 1L, versjon = 1)

        // when/then søk etter oppgave
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OppgaveSokResponse::class.java))).thenReturn(
            ResponseEntity.ok(OppgaveSokResponse(antallTreffTotalt = 0)) // opprinnelig søk gir ingen treff på oppgaver
        ).thenReturn(
            ResponseEntity.ok(OppgaveSokResponse(antallTreffTotalt = 1, oppgaver = listOf(oppgaveData))) // neste søk gir oppgave for patch av jpId
        )

        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(String::class.java)))
            .thenReturn(ResponseEntity.ok("OK"))

        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(OppgaveData::class.java)))
            .thenReturn(ResponseEntity.ok(oppgaveData))

        val journalpostId = "JOARK-2525"
        val aktoerId = "1234567890100"

        // kafka hendelse
        journalpostHendelseListener.lesHendelse(
            """
            {
              "journalpostId":"$journalpostId",
              "aktorId": "$aktoerId",
              "journalstatus": "${Journalstatus.MOTTATT}",
              "fagomrade": "BID",
              "sporing": {
                "correlationId": "abc"
              }
            }
            """.trimIndent()
        )

        val patchOppgaveJournalpostIdRequest = UpdateOppgaveAfterOpprettRequest(OppgaveDataForHendelse(oppgaveData), journalpostId)
        val opprettOppgaveRequest = OpprettOppgaveRequest(journalpostId.split("-")[1], aktoerId, "BID", enhetsNummer)

        verify(httpHeaderRestTemplateMock).exchange(
            anyString(),
            eq(HttpMethod.POST),
            eq(opprettOppgaveRequest.somHttpEntity()),
            eq(OppgaveData::class.java)
        )

        verify(httpHeaderRestTemplateMock).exchange(
            anyString(),
            eq(HttpMethod.PATCH),
            eq(patchOppgaveJournalpostIdRequest.somHttpEntity()),
            eq(String::class.java)
        )
    }

    @Test
    fun `skal opprette oppgave ved hendelse uten journalpost prefix`() {
        val oppgaveData = OppgaveData(id = 1L, versjon = 1)

        // when/then søk etter oppgave
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OppgaveSokResponse::class.java))).thenReturn(
            ResponseEntity.ok(OppgaveSokResponse(antallTreffTotalt = 0)) // opprinnelig søk gir ingen treff på oppgaver
        ).thenReturn(
            ResponseEntity.ok(OppgaveSokResponse(antallTreffTotalt = 1, oppgaver = listOf(oppgaveData))) // neste søk gir oppgave for patch av jpId
        )

        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(String::class.java)))
            .thenReturn(ResponseEntity.ok("OK"))

        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(OppgaveData::class.java)))
            .thenReturn(ResponseEntity.ok(oppgaveData))

        val journalpostId = "2525"
        val journalpostIdMedPrefix = "BID-$journalpostId"
        val aktoerId = "1234567890100"
        val enhetsNummer = "4806"
        // kafka hendelse
        journalpostHendelseListener.lesHendelse(
            """
            {
              "journalpostId":"$journalpostIdMedPrefix",
              "aktorId": "$aktoerId",
              "journalstatus": "${Journalstatus.MOTTATT}",
              "fagomrade": "BID",
              "sporing": {
                "correlationId": "abc"
              }
            }
            """.trimIndent()
        )

        val opprettOppgaveRequest = OpprettOppgaveRequest(journalpostId, aktoerId, "BID", enhetsNummer)

        verify(httpHeaderRestTemplateMock).exchange(
            anyString(),
            eq(HttpMethod.POST),
            eq(opprettOppgaveRequest.somHttpEntity()),
            eq(OppgaveData::class.java)
        )
    }

    @Test
    fun `skal opprette oppgaver med tema FAR`() {
        val oppgaveData = OppgaveData(id = 1L, versjon = 1)

        // when/then søk etter oppgave
        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.GET), any(), eq(OppgaveSokResponse::class.java))).thenReturn(
            ResponseEntity.ok(OppgaveSokResponse(antallTreffTotalt = 0)) // opprinnelig søk gir ingen treff på oppgaver
        ).thenReturn(
            ResponseEntity.ok(OppgaveSokResponse(antallTreffTotalt = 1, oppgaver = listOf(oppgaveData))) // neste søk gir oppgave for patch av jpId
        )

        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(String::class.java)))
            .thenReturn(ResponseEntity.ok("OK"))

        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(OppgaveData::class.java)))
            .thenReturn(ResponseEntity.ok(oppgaveData))

        val journalpostId = "JOARK-2525"
        val aktoerId = "1234567890100"
        val tema = "FAR"

        // kafka hendelse
        journalpostHendelseListener.lesHendelse(
            """
            {
              "journalpostId":"$journalpostId",
              "aktorId": "$aktoerId",
              "journalstatus": "${Journalstatus.MOTTATT}",
              "fagomrade":"$tema",
              "sporing": {
                "correlationId": "abc"
              }
            }
            """.trimIndent()
        )

        val opprettOppgaveRequest = OpprettOppgaveRequest("2525", aktoerId, tema, enhetsNummer)
        val updateOppgaveAfterOpprettRequest = UpdateOppgaveAfterOpprettRequest(OppgaveDataForHendelse(oppgaveData), journalpostId)

        verify(httpHeaderRestTemplateMock, times(1)).exchange(
            anyString(),
            eq(HttpMethod.POST),
            eq(opprettOppgaveRequest.somHttpEntity()),
            eq(OppgaveData::class.java)
        )

        verify(httpHeaderRestTemplateMock, times(1)).exchange(
            anyString(),
            eq(HttpMethod.PATCH),
            eq(updateOppgaveAfterOpprettRequest.somHttpEntity()),
            eq(String::class.java)
        )
    }
}
