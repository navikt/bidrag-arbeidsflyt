package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.model.Hendelse
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.kotlin.anyOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
@DisplayName("DefaultBehandleHendelseService")
internal class DefaultBehandleHendelseServiceTest {

    @Autowired
    private lateinit var behandleHendelseService: DefaultBehandleHendelseService

    @MockBean
    private lateinit var oppgaveServiceMock: OppgaveService

    @BeforeEach
    fun `reset oppgaveServiceMock`() {
        reset(oppgaveServiceMock)
    }

    @Test
    fun `skal behandle AVVIK_OVERFOR_TIL_ANNEN_ENHET i test`() {
        behandleHendelseService.behandleHendelse(JournalpostHendelse(journalpostId = "BID-1", hendelse = Hendelse.AVVIK_OVERFOR_TIL_ANNEN_ENHET.name))
        verify(oppgaveServiceMock, atLeastOnce()).overforOppgaver(anyString(), anyString(), anyOrNull())
    }

    @Test
    fun `skal behandle AVVIK_ENDRE_FAGOMRADE i test`() {
        behandleHendelseService.behandleHendelse(JournalpostHendelse(journalpostId = "BID-1", hendelse = Hendelse.AVVIK_ENDRE_FAGOMRADE.name))
        verify(oppgaveServiceMock, atLeastOnce()).ferdigstillOppgaver(anyString(), anyString(), anyOrNull())
    }

    @Test
    fun `skal behandle JOURNALFOR_JOURNALPOST i test`() {
        behandleHendelseService.behandleHendelse(JournalpostHendelse(journalpostId = "BID-1", hendelse = Hendelse.JOURNALFOR_JOURNALPOST.name))
        verify(oppgaveServiceMock, atLeastOnce()).ferdigstillOppgaver(anyString(), anyString(), anyOrNull())
    }
}