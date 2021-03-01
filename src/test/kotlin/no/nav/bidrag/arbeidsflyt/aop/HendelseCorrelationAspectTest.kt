package no.nav.bidrag.arbeidsflyt.aop

import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.commons.CorrelationId
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
@DisplayName("HendelseCorrelationAspect")
internal class HendelseCorrelationAspectTest {

    @Autowired
    private lateinit var journalpostHendelseListener: JournalpostHendelseListener

    @MockBean
    private lateinit var oppgaveConsumerMock: OppgaveConsumer

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
