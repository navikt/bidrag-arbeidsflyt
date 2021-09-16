package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.PROFILE_NO_KAFKA
import no.nav.bidrag.arbeidsflyt.model.Hendelse
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles(PROFILE_NO_KAFKA)
@DisplayName("DefaultBehandleHendelseService med samme HendelseFilter som live-profil")
internal class DefaultBehandleHendelseServiceProfileNoKafkaTest {

    @Autowired
    private lateinit var behandleHendelseService: DefaultBehandleHendelseService

    @MockBean
    private lateinit var oppgaveServiceMock: OppgaveService

    @BeforeEach
    fun `reset oppgaveServiceMock`() {
        reset(oppgaveServiceMock)
    }

    @Test
    fun `skal ikke behandle AVVIK_OVERFOR_TIL_ANNEN_ENHET i live profile`() {
        behandleHendelseService.behandleHendelse(JournalpostHendelse(hendelse = Hendelse.AVVIK_OVERFOR_TIL_ANNEN_ENHET.name))
        verifyNoMoreInteractions(oppgaveServiceMock)
    }

    @Test
    fun `skal ikke behandle AVVIK_ENDRE_FAGOMRADE i live profile`() {
        behandleHendelseService.behandleHendelse(JournalpostHendelse(hendelse = Hendelse.AVVIK_ENDRE_FAGOMRADE.name))
        verifyNoMoreInteractions(oppgaveServiceMock)
    }

    @Test
    fun `skal ikke behandle JOURNALFOR_JOURNALPOST i live profile`() {
        behandleHendelseService.behandleHendelse(JournalpostHendelse(hendelse = Hendelse.JOURNALFOR_JOURNALPOST.name))
        verifyNoMoreInteractions(oppgaveServiceMock)
    }
}