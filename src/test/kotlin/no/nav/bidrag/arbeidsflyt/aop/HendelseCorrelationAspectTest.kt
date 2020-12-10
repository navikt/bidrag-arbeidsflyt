package no.nav.bidrag.arbeidsflyt.aop

import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.commons.CorrelationId
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@DisplayName("HendelseCorrelationAspect")
class HendelseCorrelationAspectTest {

    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @Test
    fun `skal spore CorrelationId fra JournalpostHendelse`() {
        val hendelse = """{
          "journalpostId":"BID-101",
          "hendelse":"JOURNALFOR_JOURNALPOST",
          "sporing": {
            "correlationId":"test.av.correlation.id",
            "opprettet":"nå"
          }
        }
        """.trimIndent()

        journalpostHendelseListener.lesHendelse(hendelse)

        assertThat(CorrelationId.fetchCorrelationIdForThread()).isEqualTo("test.av.correlation.id")
    }
}
