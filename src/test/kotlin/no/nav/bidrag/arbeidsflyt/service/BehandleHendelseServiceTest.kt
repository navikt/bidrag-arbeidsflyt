package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.model.Detalj
import no.nav.bidrag.arbeidsflyt.model.DetaljVerdi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
@Suppress("UNCHECKED_CAST", "NonAsciiCharacters")
@DisplayName("BehandleHendelseService")
internal class BehandleHendelseServiceTest {

    @Autowired
    private lateinit var behandleHendelseService: BehandleHendelseService

    @MockBean
    private lateinit var oppgaveConsumerMock: OppgaveConsumer

    @Test
    fun `skal søke etter åpne oppgaver når hendelsen er JOURNALFOR_JOURNALPOST`() {
        val journalpostHendelse = JournalpostHendelse("BID-1", "JOURNALFOR_JOURNALPOST")

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        val oppgaveSokRequests = journalpostHendelse.hentOppgaveSokRequestsMedOgUtenPrefix()
        verify(oppgaveConsumerMock).finnOppgaverForJournalpost(oppgaveSokRequests.first)
        verify(oppgaveConsumerMock).finnOppgaverForJournalpost(oppgaveSokRequests.second)
    }

    @Test
    fun `skal ferdigstille oppgaver som ble funnet av oppgavesøket`() {
        val journalpostHendelse = JournalpostHendelse("BID-1", "JOURNALFOR_JOURNALPOST", detaljer = mapOf("enhetsnummer" to "1001"))

        `when`(oppgaveConsumerMock.finnOppgaverForJournalpost(anyOppgaveSokRequest())).thenReturn(
            OppgaveSokResponse(1, listOf(OppgaveData()) as MutableList<OppgaveData>)
        )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        // forventer at søk blir gjort for journalpostId med og uten prefix...
        verify(oppgaveConsumerMock, times(2)).ferdigstillOppgaver(anyFerdigstillOppgaveRequest())
    }

    @Test
    fun `skal ikke feile når hendelsen er ukjent`() {
        val journalpostHendelse = JournalpostHendelse(hendelse = "ikke støttet")

        behandleHendelseService.behandleHendelse(journalpostHendelse)
    }

    @ParameterizedTest
    @ValueSource(strings = [DetaljVerdi.FAGOMRADE_BIDRAG, DetaljVerdi.FAGOMRADE_FARSKAP])
    fun `skal ikke søke etter oppgaver å ferdigstille når AVVIK_ENDRE_FAGOMRADE er til fagområde BID eller FAR`(fagomrade: String) {
        val journalpostHendelse = JournalpostHendelse(
            journalpostId = "FAR-1", hendelse = "AVVIK_ENDRE_FAGOMRADE", detaljer = mapOf(Detalj.FAGOMRADE to fagomrade)
        )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        verify(oppgaveConsumerMock, never()).finnOppgaverForJournalpost(anyOppgaveSokRequest())
    }

    @Test
    fun `skal søke etter oppgaver å ferdigstille når AVVIK_ENDRE_FAGOMRADE er til fagområde annet enn BID eller FAR`() {
        val journalpostHendelse = JournalpostHendelse(
            journalpostId = "FAR-1", hendelse = "AVVIK_ENDRE_FAGOMRADE", detaljer = mapOf(Detalj.FAGOMRADE to "AAP")
        )

        behandleHendelseService.behandleHendelse(journalpostHendelse)

        // forventer at søk blir gjort for journalpostId med og uten prefix...
        verify(oppgaveConsumerMock, times(2)).finnOppgaverForJournalpost(anyOppgaveSokRequest())
    }

    private fun <T> anyOppgaveSokRequest(): T = any(OppgaveSokRequest::class.java) as T
    private fun <T> anyFerdigstillOppgaveRequest() = any(FerdigstillOppgaveRequest::class.java) as T
}
