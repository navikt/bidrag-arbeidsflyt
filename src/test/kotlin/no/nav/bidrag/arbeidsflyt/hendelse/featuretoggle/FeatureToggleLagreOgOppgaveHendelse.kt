package no.nav.bidrag.arbeidsflyt.hendelse.featuretoggle

import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.hendelse.AbstractKafkaHendelseTest
import no.nav.bidrag.arbeidsflyt.utils.BID_JOURNALPOST_ID_3_NEW
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_3
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_1
import no.nav.bidrag.arbeidsflyt.utils.createJournalpostHendelse
import no.nav.bidrag.arbeidsflyt.utils.createOppgaveHendelse
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.TimeUnit

@TestPropertySource(properties = ["FEATURE_ENABLED=NONE"])
class FeatureToggleLagreOgOppgaveHendelse: AbstractKafkaHendelseTest() {

    @Value("\${TOPIC_JOURNALPOST}")
    private val topicJournalpost: String? = null

    @Value("\${TOPIC_OPPGAVE_ENDRET}")
    private val topicEndret: String? = null

    @Value("\${TOPIC_OPPGAVE_OPPRETTET}")
    private val topicOpprettet: String? = null

    @Test
    fun `skal ikke mappe og behandle oppgave endret hendelse`() {
        val oppgaveId = 239999L
        val oppgaveHendelse = createOppgaveHendelse(oppgaveId, journalpostId = JOURNALPOST_ID_3, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT)
        val hendelseString = objectMapper.writeValueAsString(oppgaveHendelse)

        configureProducer()?.send(ProducerRecord(topicEndret, hendelseString))

        await.atLeast(3, TimeUnit.SECONDS)

        val endretOppgaveOptional = testDataGenerator.hentOppgave(oppgaveId)
        Assertions.assertThat(endretOppgaveOptional.isPresent).isFalse
        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal ikke mappe og behandle oppgave opprettet hendelse`() {
        val oppgaveId = 239999L
        val oppgaveHendelse = createOppgaveHendelse(oppgaveId, journalpostId = JOURNALPOST_ID_3)
        val hendelseString = objectMapper.writeValueAsString(oppgaveHendelse)

        configureProducer()?.send(ProducerRecord(topicOpprettet, hendelseString))

        await.atLeast(3, TimeUnit.SECONDS)

        val endretOppgaveOptional = testDataGenerator.hentOppgave(oppgaveId)
        Assertions.assertThat(endretOppgaveOptional.isPresent).isFalse

        verifyOppgaveNotOpprettet()
    }

    @Test
    fun `skal lagre journalpost i databasen`() {
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW)
        val hendelseString = objectMapper.writeValueAsString(journalpostHendelse)
        configureProducer()?.send(ProducerRecord(topicJournalpost, hendelseString))

        await.atLeast(3, TimeUnit.SECONDS)

        val journalpostOptional = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_3_NEW)
        Assertions.assertThat(journalpostOptional.isPresent).isFalse

    }
}