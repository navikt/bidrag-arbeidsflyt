package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.utils.BID_JOURNALPOST_ID_3_NEW
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_3
import no.nav.bidrag.arbeidsflyt.utils.createJournalpostHendelse
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.TimeUnit


internal class KafkaJournalpostHendelseListenerTest: AbstractKafkaHendelseTest() {

    @Value("\${TOPIC_JOURNALPOST}")
    private val topic: String? = null

    @Test
    fun `skal opprette oppgave med BID prefix nar journalpost mottat uten oppgave`() {
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW)
        val hendelseString = objectMapper.writeValueAsString(journalpostHendelse)
        configureProducer()?.send(ProducerRecord(topic, hendelseString))

        await.atMost(4, TimeUnit.SECONDS).until {
            testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_3_NEW).isPresent
        }

        val journalpostOptional = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_3_NEW)
        assertThat(journalpostOptional.isPresent).isTrue

        assertThat(journalpostOptional).hasValueSatisfying { journalpost ->
            assertThat(journalpost.journalpostId).isEqualTo(BID_JOURNALPOST_ID_3_NEW)
            assertThat(journalpost.gjelderId).isEqualTo(PERSON_IDENT_3)
            assertThat(journalpost.status).isEqualTo("M")
            assertThat(journalpost.tema).isEqualTo("BID")
            assertThat(journalpost.enhet).isEqualTo("4833")
        }

        verifyOppgaveOpprettetWith("\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${BID_JOURNALPOST_ID_3_NEW.replace("BID-", "")}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveEndretWith(1, BID_JOURNALPOST_ID_3_NEW)
    }


}