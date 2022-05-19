package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_2
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_3
import no.nav.bidrag.arbeidsflyt.utils.createDLQKafka
import no.nav.bidrag.arbeidsflyt.utils.createJournalpostHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class KafkaDLQSchedulerTest: AbstractBehandleHendelseTest() {

    @Autowired
    lateinit var kafkaDLQRetryScheduler: KafkaDLQRetryScheduler


    @Test
    fun `should process and delete message with retry value true`(){
        stubHentOppgave(emptyList())
        stubHentPerson(PERSON_IDENT_3)
        val journalpostHendelse1 = createJournalpostHendelse("JOARK-$JOURNALPOST_ID_1")
        val journalpostHendelse2 = createJournalpostHendelse("JOARK-$JOURNALPOST_ID_2")
        testDataGenerator.opprettDLQMelding(createDLQKafka(objectMapper.writeValueAsString(journalpostHendelse1), retry = true))
        testDataGenerator.opprettDLQMelding(createDLQKafka(objectMapper.writeValueAsString(journalpostHendelse2), retry = false))

        val dlqMessages = testDataGenerator.hentDlKafka()
        assertThat(dlqMessages.size).isEqualTo(2)

        kafkaDLQRetryScheduler.processMessages()

        val dlqMessagesAfter = testDataGenerator.hentDlKafka()
        assertThat(dlqMessagesAfter.size).isEqualTo(1)

        verifyOppgaveOpprettetWith("\"oppgavetype\":\"JFR\"", "\"journalpostId\":\"${JOURNALPOST_ID_1}\"", "\"opprettetAvEnhetsnr\":\"9999\"", "\"prioritet\":\"HOY\"", "\"tema\":\"BID\"")
        verifyOppgaveNotEndret()
    }

    @Test
    fun `should set retry to false if processing fails`(){
        stubHentOppgaveError()
        val journalpostHendelse = createJournalpostHendelse("JOARK-$JOURNALPOST_ID_1")
        testDataGenerator.opprettDLQMelding(createDLQKafka(objectMapper.writeValueAsString(journalpostHendelse), retry = true))

        val dlqMessages = testDataGenerator.hentDlKafka()
        assertThat(dlqMessages.size).isEqualTo(1)

        kafkaDLQRetryScheduler.processMessages()

        val dlqMessagesAfter = testDataGenerator.hentDlKafka()
        assertThat(dlqMessagesAfter.size).isEqualTo(1)
        assertThat(dlqMessagesAfter[0].retry).isEqualTo(false)
    }


}
