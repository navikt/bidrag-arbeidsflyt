package no.nav.bidrag.arbeidsflyt.aop

import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.service.OppgaveService
import no.nav.bidrag.commons.ExceptionLogger
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.concurrent.ExecutionException

@SpringBootTest(properties = ["OPPGAVE_URL=https://unit.test"])
@DisplayName("ExceptionLoggerAspect")
internal class ExceptionLoggerAspectTest {
    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @MockBean
    private lateinit var oppgaveServiceMock: OppgaveService

    @MockBean
    private lateinit var exceptionLoggerMock: ExceptionLogger

    @Test
    fun `skal logge exceptions fra service`() {
        whenever(oppgaveServiceMock.overforOppgaver(any(), any())).thenThrow(IllegalStateException("Logg exception!"))

        assertThatExceptionOfType(ExecutionException::class.java).isThrownBy {
            journalpostHendelseListener.lesHendelse(
                """{
                  "journalpostId":"BID-101",
                  "hendelse":"AVVIK_OVERFOR_TIL_ANNEN_ENHET"
                }""".trimIndent()
            )
        }

        verify(exceptionLoggerMock).logException(any(), anyString())
    }
}
