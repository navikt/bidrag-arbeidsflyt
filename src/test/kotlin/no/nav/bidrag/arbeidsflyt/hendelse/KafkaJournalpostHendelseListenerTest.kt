package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.utils.BID_JOURNALPOST_ID_3_NEW
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_4_NEW
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_3
import no.nav.bidrag.arbeidsflyt.utils.createDLQKafka
import no.nav.bidrag.arbeidsflyt.utils.createJournalpostHendelse
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import java.util.concurrent.TimeUnit


internal class KafkaJournalpostHendelseListenerTest: AbstractKafkaHendelseTest() {

    @Value("\${TOPIC_JOURNALPOST}")
    private val topic: String? = null

    @Test
    fun `skal lagre journalpost i databasen`() {
        stubHentPerson(PERSON_IDENT_3)
        val journalpostHendelse = createJournalpostHendelse(JOURNALPOST_ID_1)
        val hendelseString = objectMapper.writeValueAsString(journalpostHendelse)
        configureProducer()?.send(ProducerRecord(topic, hendelseString))

        await.atMost(4, TimeUnit.SECONDS).untilAsserted {
            testDataGenerator.hentJournalpost(JOURNALPOST_ID_1).isPresent
            val journalpostOptional = testDataGenerator.hentJournalpost(JOURNALPOST_ID_1)
            assertThat(journalpostOptional.isPresent).isTrue

            assertThat(journalpostOptional).hasValueSatisfying { journalpost ->
                assertThat(journalpost.journalpostId).isEqualTo(JOURNALPOST_ID_1)
                assertThat(journalpost.status).isEqualTo("M")
                assertThat(journalpost.tema).isEqualTo("BID")
                assertThat(journalpost.enhet).isEqualTo("4833")
            }
        }
    }

    @Test
    fun `skal slette feilede meldinger fra dlqkafka nar behandling av melding gar ok`(){
        stubHentOppgaveContaining(listOf())
        stubHentPerson()
        stubHentGeografiskEnhet(enhet = "1234")
        val journalpostIdMedJoarkPrefix = "JOARK-$JOURNALPOST_ID_4_NEW"
        val journalpostHendelse = createJournalpostHendelse(journalpostIdMedJoarkPrefix)

        testDataGenerator.opprettDLQMelding(createDLQKafka(objectMapper.writeValueAsString(journalpostHendelse), messageKey = journalpostIdMedJoarkPrefix))
        testDataGenerator.opprettDLQMelding(createDLQKafka(objectMapper.writeValueAsString(journalpostHendelse), messageKey = journalpostIdMedJoarkPrefix))

        val dlqMessagesBefore = testDataGenerator.hentDlKafka()
        assertThat(dlqMessagesBefore.size).isEqualTo(2)

        journalpostHendelse.aktorId = "123213213"
        journalpostHendelse.fnr = "123123123"
        val hendelseString = objectMapper.writeValueAsString(journalpostHendelse)
        configureProducer()?.send(ProducerRecord(topic, hendelseString))

        await.atMost(4, TimeUnit.SECONDS).untilAsserted {
            val dlqMessagesAfter = testDataGenerator.hentDlKafka()
            assertThat(dlqMessagesAfter.size).isEqualTo(0)
        }


    }

    @Test
    fun `skal legge melding i dead_letter_kafka tabellen hvis behandling feiler`() {
        stubHentOppgave(emptyList())
        stubOpprettOppgave(status = HttpStatus.INTERNAL_SERVER_ERROR)
        stubHentPerson(PERSON_IDENT_3)
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW)
        val hendelseString = objectMapper.writeValueAsString(journalpostHendelse)
        configureProducer()?.send(ProducerRecord(topic, BID_JOURNALPOST_ID_3_NEW, hendelseString))

        await.atMost(4, TimeUnit.SECONDS).untilAsserted {
            val dlMessages = testDataGenerator.hentDlKafka()
            assertThat(dlMessages.size).isEqualTo(1)
            assertThat(dlMessages[0].messageKey).isEqualTo(BID_JOURNALPOST_ID_3_NEW)
        }
    }

    @Test
    fun `skal opprette oppgave med BID prefix nar journalpost mottatt uten oppgave`() {
        val geografiskEnhet = "4812"
        stubHentGeografiskEnhet("0101")
        stubHentOppgave(emptyList())
        stubOpprettOppgave()
        stubHentPerson(PERSON_IDENT_3)
        val journalpostHendelse = createJournalpostHendelse(BID_JOURNALPOST_ID_3_NEW)
        journalpostHendelse.enhet = geografiskEnhet
        val hendelseString = objectMapper.writeValueAsString(journalpostHendelse)
        configureProducer()?.send(ProducerRecord(topic, hendelseString))

        await.atMost(4, TimeUnit.SECONDS).untilAsserted {
            val journalpostOptional = testDataGenerator.hentJournalpost(BID_JOURNALPOST_ID_3_NEW)
            assertThat(journalpostOptional.isPresent).isTrue

            assertThat(journalpostOptional).hasValueSatisfying { journalpost ->
                assertThat(journalpost.journalpostId).isEqualTo(BID_JOURNALPOST_ID_3_NEW)
                assertThat(journalpost.status).isEqualTo("M")
                assertThat(journalpost.tema).isEqualTo("BID")
                assertThat(journalpost.enhet).isEqualTo(geografiskEnhet)
            }

            verifyOppgaveOpprettetWith("\"tildeltEnhetsnr\":\"$geografiskEnhet\"", "\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${BID_JOURNALPOST_ID_3_NEW}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
            verifyOppgaveNotEndret()
        }
    }


}
