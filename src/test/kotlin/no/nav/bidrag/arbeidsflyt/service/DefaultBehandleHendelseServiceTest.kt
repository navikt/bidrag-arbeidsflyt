package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.dto.OverforOppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.JOURNALFORINGSOPPGAVE
import no.nav.bidrag.arbeidsflyt.model.OppgaveDataForHendelse
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@DisplayName("DefaultBehandleHendelseService")
@ActiveProfiles("test")
internal class DefaultBehandleHendelseServiceTest {

    @Autowired
    private lateinit var behandleHendelseService: BehandleHendelseService

    @MockBean
    private lateinit var oppgaveConsumerMock: OppgaveConsumer

    @BeforeEach
    fun `reset oppgaveServiceMock`() {
        reset(oppgaveConsumerMock)
    }

    @ParameterizedTest
    @ValueSource(strings = ["BID", "FAR"])
    fun `skal ikke ferdigstille oppgaver når det er endring til internt fagområde`(fagomrade: String) {
        val journalpostId = "$fagomrade-101"

        whenever(oppgaveConsumerMock.finnOppgaverForJournalpost(any())).thenReturn(
            OppgaveSokResponse(
                antallTreffTotalt = 1,
                oppgaver = listOf(OppgaveData(journalpostId = journalpostId))
            )
        )

        behandleHendelseService.behandleHendelse(JournalpostHendelse(journalpostId = journalpostId, fagomrade = fagomrade))
        verify(oppgaveConsumerMock, never()).endreOppgave(anyOrNull(), anyOrNull())
    }

    @ParameterizedTest
    @ValueSource(strings = ["AAREG", "ANNET_ENN_BID/FAR"])
    @Disabled
    fun `skal ferdigstille oppgaver når det er endring til eksternt fagområde`(fagomrade: String) {
        whenever(oppgaveConsumerMock.finnOppgaverForJournalpost(any())).thenReturn(
            OppgaveSokResponse(
                antallTreffTotalt = 1,
                oppgaver = listOf(OppgaveData(id = 1, tema = fagomrade, versjon = 1, oppgavetype = JOURNALFORINGSOPPGAVE))
            )
        )

        behandleHendelseService.behandleHendelse(JournalpostHendelse(journalpostId = "$fagomrade-101", fagomrade = fagomrade))
        verify(oppgaveConsumerMock).endreOppgave(FerdigstillOppgaveRequest(OppgaveDataForHendelse(id = 1, versjon = 1)))
    }

    @Test
    fun `skal ikke overfore oppgaver når det er samme tildelt enhetsnummer`() {
        whenever(oppgaveConsumerMock.finnOppgaverForJournalpost(any())).thenReturn(
            OppgaveSokResponse(
                antallTreffTotalt = 1,
                oppgaver = listOf(
                    OppgaveData(
                        id = 1,
                        tildeltEnhetsnr = "1001",
                        versjon = 1
                    )
                )
            )
        )

        behandleHendelseService.behandleHendelse(JournalpostHendelse(journalpostId = "BID-101", enhet = "1001"))
        verify(oppgaveConsumerMock, never()).endreOppgave(patchOppgaveRequest = any(), endretAvEnhetsnummer = anyOrNull())
    }

    @Test
    @Disabled
    fun `skal overfore oppgaver når det er nytt tildelt enhetsnummer`() {
        val journalpostId = "BID-101"

        whenever(oppgaveConsumerMock.finnOppgaverForJournalpost(any())).thenReturn(
            OppgaveSokResponse(
                antallTreffTotalt = 1,
                oppgaver = listOf(
                    OppgaveData(
                        id = 1,
                        tildeltEnhetsnr = "1001",
                        versjon = 1
                    )
                )
            )
        )

        val nyttEnhetsnummer = "1234"
        behandleHendelseService.behandleHendelse(JournalpostHendelse(journalpostId = journalpostId, enhet = nyttEnhetsnummer))

        verify(oppgaveConsumerMock).endreOppgave(
            patchOppgaveRequest = eq(
                OverforOppgaveRequest(
                    oppgaveDataForHendelse = OppgaveDataForHendelse(id = 1, versjon = 1, tildeltEnhetsnr = nyttEnhetsnummer),
                    nyttEnhetsnummer = nyttEnhetsnummer,
                    "Z99999"
                )
            ),
            endretAvEnhetsnummer = anyOrNull()
        )
    }

    @Test
    fun `skal ikke ferdigstille journalføringsoppgave når journalstatus er null`() {
        whenever(oppgaveConsumerMock.finnOppgaverForJournalpost(any())).thenReturn(
            OppgaveSokResponse(antallTreffTotalt = 1, oppgaver = listOf(OppgaveData(oppgavetype = JOURNALFORINGSOPPGAVE)))
        )

        behandleHendelseService.behandleHendelse(JournalpostHendelse(journalpostId = "BID-101", journalstatus = null))
        verify(oppgaveConsumerMock, never()).endreOppgave(anyOrNull(), any())
    }
}
