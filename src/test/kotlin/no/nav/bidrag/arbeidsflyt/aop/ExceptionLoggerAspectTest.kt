package no.nav.bidrag.arbeidsflyt.aop

import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.commons.ExceptionLogger
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(properties = ["OPPGAVE_URL=https://unit.test"])
@DisplayName("ExceptionLoggerAspect")
internal class ExceptionLoggerAspectTest {
    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @MockBean
    private lateinit var behandleHendelseServiceMock: BehandleHendelseService

    @MockBean
    private lateinit var exceptionLoggerMock: ExceptionLogger

    @Test
    fun `skal logge exceptions fra service`() {
        val illegalStateException = IllegalStateException("Logg exception!")
        whenever(behandleHendelseServiceMock.behandleHendelse(JournalpostHendelse("BID-101", "TEST_HENDELSE")))
            .thenThrow(illegalStateException)

        assertThatIllegalStateException().isThrownBy {
            journalpostHendelseListener.lesHendelse(
                """{
                  "journalpostId":"BID-101",
                  "hendelse":"TEST_HENDELSE"
                }""".trimIndent()
            )
        }

        verify(exceptionLoggerMock).logException(eq(illegalStateException), anyString())
    }
}
