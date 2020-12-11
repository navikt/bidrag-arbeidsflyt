package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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
}
