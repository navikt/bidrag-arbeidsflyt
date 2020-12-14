@file:Suppress("UNCHECKED_CAST")

package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
@DisplayName("BehandleHendelseService")
internal class BehandleHendelseServiceTest {

    @Autowired
    private lateinit var behandleHendelseService: BehandleHendelseService

    @MockBean
    private lateinit var oppgaveConsumerMock: OppgaveConsumer

    @Test
    @DisplayName("skal søke etter åpne oppgaver når hendelsen er JOURNALFOR_JOURNALPOST")
    fun `skal soke etter apne oppgaver nar hendelsen er JOURNALFOR_JOURNALPOST`() {
        val journalpostHendelse = JournalpostHendelse("BID-1", "JOURNALFOR_JOURNALPOST")

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val oppgaveSokRequests = journalpostHendelse.hentOppgaveSokRequestsMedOgUtenPrefix()
        verify(oppgaveConsumerMock).finnOppgaverForJournalpost(oppgaveSokRequests.first)
        verify(oppgaveConsumerMock).finnOppgaverForJournalpost(oppgaveSokRequests.second)
    }

    @Test
    @DisplayName("skal ferdigstille oppgaver som ble funnet av oppgavesøket")
    fun `skal ferdigstille oppgaver som ble funnet av oppgavesoket`() {
        val journalpostHendelse = JournalpostHendelse("BID-1", "JOURNALFOR_JOURNALPOST")

        `when`(oppgaveConsumerMock.finnOppgaverForJournalpost(anyOppgaveSokRequest())).thenReturn(
            OppgaveSokResponse(1, listOf(OppgaveData()) as MutableList<OppgaveData>)
        )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verify(oppgaveConsumerMock, times(2)).ferdigstillOppgaver(anyFerdigstillOppgaveRequest())
    }

    private fun <T> anyOppgaveSokRequest(): T = any(OppgaveSokRequest::class.java) as T
    private fun <T> anyFerdigstillOppgaveRequest() = any(FerdigstillOppgaveRequest::class.java) as T
}
