package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
@DisplayName("JournalpostHendelseListener")
internal class JournalpostHendelseListenerTest {

    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @MockBean
    private lateinit var behandleHendelseServiceMock: BehandleHendelseService

    @Test
    fun `skal mappe og behandle JournalpostHendelse`() {
        journalpostHendelseListener.lesHendelse(
            """{
              "journalpostId":"BID-1",
              "hendelse":"TEST_HENDELSE"
            }""".trimIndent()
        )

        verify(behandleHendelseServiceMock).behandleHendelse(JournalpostHendelse("BID-1", "TEST_HENDELSE"))
    }

    @Test
    fun `skal ha sporingsdata i meldingen`() {
        journalpostHendelseListener.lesHendelse(
            """{
              "journalpostId":"BID-2",
              "hendelse":"TEST_HENDELSE",
              "sporing": {
                "correlationId":"xyz",
                "opprettet":"nå"
              }
            }""".trimIndent()
        )

        verify(behandleHendelseServiceMock).behandleHendelse(
            JournalpostHendelse("BID-2", "TEST_HENDELSE", Sporingsdata("xyz", "nå"))
        )
    }
}
