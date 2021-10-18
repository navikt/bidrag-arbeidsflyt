package no.nav.bidrag.arbeidsflyt.aop

import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.commons.CorrelationId
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZoneId

@SpringBootTest
@DisplayName("HendelseCorrelationAspect")
@Disabled("java.lang.IllegalArgumentException: URI is not absolute, SecurityTokenService.kt, linje 24 ???")
internal class HendelseCorrelationAspectTest {

    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @Test
    fun `skal spore CorrelationId fra JournalpostHendelse`() {
        val hendelse = """{
          "journalpostId":"BID-101",
          "sporing": {
            "correlationId":"test.av.correlation.id"
          }
        }
        """.trimIndent()

        journalpostHendelseListener.lesHendelse(hendelse)

        assertThat(CorrelationId.fetchCorrelationIdForThread()).isEqualTo("test.av.correlation.id")
    }
}
