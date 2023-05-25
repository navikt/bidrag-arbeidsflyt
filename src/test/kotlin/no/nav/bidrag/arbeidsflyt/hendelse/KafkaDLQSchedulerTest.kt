package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_2
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_3
import no.nav.bidrag.arbeidsflyt.utils.createDLQKafka
import no.nav.bidrag.arbeidsflyt.utils.createJournalpostHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

internal class KafkaDLQSchedulerTest : AbstractBehandleHendelseTest() {

    @Autowired
    lateinit var kafkaDLQRetryScheduler: KafkaDLQRetryScheduler

    @Test
    fun `should process and delete message with retry value true`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet()
        stubHentPerson(PERSON_IDENT_3)
        val journalpostHendelse1 = createJournalpostHendelse("JOARK-$JOURNALPOST_ID_1")
        val journalpostHendelse2 = createJournalpostHendelse("JOARK-$JOURNALPOST_ID_2")
        testDataGenerator.opprettDLQMelding(createDLQKafka(objectMapper.writeValueAsString(journalpostHendelse1), retry = true, messageKey = journalpostHendelse1.journalpostId))
        testDataGenerator.opprettDLQMelding(createDLQKafka(objectMapper.writeValueAsString(journalpostHendelse2), retry = false, messageKey = journalpostHendelse2.journalpostId))

        val dlqMessages = testDataGenerator.hentDlKafka()
        assertThat(dlqMessages.size).isEqualTo(2)

        kafkaDLQRetryScheduler.processMessages()

        val dlqMessagesAfter = testDataGenerator.hentDlKafka()
        assertThat(dlqMessagesAfter.size).isEqualTo(1)

        verifyOppgaveOpprettetWith("\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${JOURNALPOST_ID_1}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
    }

    @Test
    fun `should set retry to false if processing fails after max retry`() {
        stubHentOppgaveError()
        val journalpostHendelse = createJournalpostHendelse("JOARK-$JOURNALPOST_ID_1")
        testDataGenerator.opprettDLQMelding(createDLQKafka(objectMapper.writeValueAsString(journalpostHendelse), retry = true, retryCount = 19))

        val dlqMessages = testDataGenerator.hentDlKafka()
        assertThat(dlqMessages.size).isEqualTo(1)

        kafkaDLQRetryScheduler.processMessages()

        val dlqMessagesAfter = testDataGenerator.hentDlKafka()
        assertThat(dlqMessagesAfter.size).isEqualTo(1)
        assertThat(dlqMessagesAfter[0].retry).isEqualTo(false)
    }

    @Test
    fun `should increment retry count if processing fails`() {
        stubHentOppgaveError()
        val journalpostHendelse = createJournalpostHendelse("JOARK-$JOURNALPOST_ID_1")
        testDataGenerator.opprettDLQMelding(createDLQKafka(objectMapper.writeValueAsString(journalpostHendelse), retry = true, retryCount = 1))

        val dlqMessages = testDataGenerator.hentDlKafka()
        assertThat(dlqMessages.size).isEqualTo(1)
        assertThat(dlqMessages[0].retryCount).isEqualTo(1)

        kafkaDLQRetryScheduler.processMessages()

        val dlqMessagesAfter = testDataGenerator.hentDlKafka()
        assertThat(dlqMessagesAfter.size).isEqualTo(1)
        assertThat(dlqMessagesAfter[0].retry).isEqualTo(true)
        assertThat(dlqMessagesAfter[0].retryCount).isEqualTo(2)
    }

    @Test
    fun `should process messages ascending by created date`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet()
        stubHentPerson(PERSON_IDENT_3, status = HttpStatus.OK, nextScenario = "FAIL")
        stubHentPerson(PERSON_IDENT_3, status = HttpStatus.INTERNAL_SERVER_ERROR, scenarioState = "FAIL")

        val journalpostHendelseOld = createJournalpostHendelse("JOARK-$JOURNALPOST_ID_1")
            .copy(
                aktorId = null,
                fnr = "24444444"
            )
        val journalpostHendelse2 = createJournalpostHendelse("JOARK-$JOURNALPOST_ID_1")
            .copy(
                aktorId = null,
                fnr = "13213213"
            )
        testDataGenerator.opprettDLQMelding(createDLQKafka(objectMapper.writeValueAsString(journalpostHendelse2), retry = true, retryCount = 1, timestamp = LocalDateTime.now(), messageKey = journalpostHendelse2.journalpostId))
        testDataGenerator.opprettDLQMelding(createDLQKafka(objectMapper.writeValueAsString(journalpostHendelseOld), retry = true, retryCount = 1, timestamp = LocalDateTime.now().minusDays(1), messageKey = journalpostHendelseOld.journalpostId))

        val dlqMessages = testDataGenerator.hentDlKafka()
        assertThat(dlqMessages.size).isEqualTo(2)

        kafkaDLQRetryScheduler.processMessages()

        val dlqMessagesAfter = testDataGenerator.hentDlKafka()
        assertThat(dlqMessagesAfter.size).isEqualTo(1)
        assertThat(dlqMessagesAfter[0].payload).contains(journalpostHendelse2.fnr)
        verifyHentPersonKalt(2)
    }
}
