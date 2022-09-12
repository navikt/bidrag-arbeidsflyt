package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import no.nav.bidrag.dokument.dto.Sporingsdata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@DisplayName("JournalpostHendelseListener")
@ActiveProfiles("test")
internal class JournalpostHendelseListenerTest {

    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @MockBean
    private lateinit var behandleHendelseServiceMock: BehandleHendelseService

    @Test
    fun `skal mappe og behandle JournalpostHendelse`() {
        journalpostHendelseListener.lesHendelse(
            """{
              "journalpostId":"BID-1"
            }""".trimIndent()
        )

        verify(behandleHendelseServiceMock).behandleHendelse(JournalpostHendelse(journalpostId = "BID-1"))
    }

    @Test
    fun `skal ha sporingsdata i meldingen`() {
        journalpostHendelseListener.lesHendelse(
            """{
              "journalpostId":"BID-2",
              "sporing": {
                "correlationId":"xyz",
                "brukerident":"jb",
                "saksbehandlersNavn":"Jon Blund"
              }
            }""".trimIndent()
        )

        verify(behandleHendelseServiceMock).behandleHendelse(
            JournalpostHendelse(
                journalpostId = "BID-2",
                sporing = Sporingsdata(
                    correlationId = "xyz",
                    brukerident = "jb",
                    saksbehandlersNavn = "Jon Blund"
                )
            )
        )
    }
}
