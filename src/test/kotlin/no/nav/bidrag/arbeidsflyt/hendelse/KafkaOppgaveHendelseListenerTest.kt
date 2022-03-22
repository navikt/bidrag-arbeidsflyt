package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_3
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVETYPE_JFR
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_1
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_3
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_1
import no.nav.bidrag.arbeidsflyt.utils.createOppgaveHendelse
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.TimeUnit


internal class KafkaOppgaveHendelseListenerTest: AbstractKafkaHendelseTest() {

    @Value("\${TOPIC_OPPGAVE_ENDRET}")
    private val topicEndret: String? = null

    @Value("\${TOPIC_OPPGAVE_OPPRETTET}")
    private val topicOpprettet: String? = null


    @Test
    fun `skal mappe og behandle oppgave endret hendelse`() {
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_3, journalpostId = JOURNALPOST_ID_3, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT)
        val hendelseString = objectMapper.writeValueAsString(oppgaveHendelse)

        configureProducer()?.send(ProducerRecord(topicEndret, hendelseString))

        await.atMost(2, TimeUnit.SECONDS).until {
            val oppgave = testDataGenerator.hentOppgave(OPPGAVE_ID_3)
            oppgave.isPresent && oppgave.get().ident == PERSON_IDENT_1
        }

        val endretOppgaveOptional = testDataGenerator.hentOppgave(OPPGAVE_ID_3)
        assertThat(endretOppgaveOptional.isPresent).isTrue

        assertThat(endretOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(OPPGAVE_ID_3)
            assertThat(oppgave.journalpostId).isEqualTo(JOURNALPOST_ID_3)
            assertThat(oppgave.ident).isEqualTo(PERSON_IDENT_1)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_JFR)
            assertThat(oppgave.tema).isEqualTo("BID")
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.FERDIGSTILT.name)
        }

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal mappe og behandle oppgave opprettet hendelse`() {
        val oppgaveId = 239999L
        val oppgaveHendelse = createOppgaveHendelse(oppgaveId, journalpostId = JOURNALPOST_ID_3)
        val hendelseString = objectMapper.writeValueAsString(oppgaveHendelse)

        configureProducer()?.send(ProducerRecord(topicOpprettet, hendelseString))

        await.atMost(2, TimeUnit.SECONDS).until {
            testDataGenerator.hentOppgave(oppgaveId).isPresent
        }

        val endretOppgaveOptional = testDataGenerator.hentOppgave(oppgaveId)
        assertThat(endretOppgaveOptional.isPresent).isTrue

        assertThat(endretOppgaveOptional).hasValueSatisfying { oppgave ->
            assertThat(oppgave.oppgaveId).isEqualTo(oppgaveId)
            assertThat(oppgave.journalpostId).isEqualTo(JOURNALPOST_ID_3)
            assertThat(oppgave.ident).isEqualTo(PERSON_IDENT_1)
            assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_JFR)
            assertThat(oppgave.tema).isEqualTo("BID")
            assertThat(oppgave.status).isEqualTo(OppgaveStatus.OPPRETTET.name)
        }

        verifyOppgaveNotOpprettet()
    }


}
