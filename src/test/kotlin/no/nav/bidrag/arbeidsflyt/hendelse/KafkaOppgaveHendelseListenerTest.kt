package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.dto.Oppgavestatuskategori
import no.nav.bidrag.arbeidsflyt.utils.BID_JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_3
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVETYPE_JFR
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_1
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_3
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_1
import no.nav.bidrag.arbeidsflyt.utils.createJournalpost
import no.nav.bidrag.arbeidsflyt.utils.createOppgaveHendelse
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.TimeUnit


internal class KafkaOppgaveHendelseListenerTest: AbstractKafkaHendelseTest() {

    @Value("\${TOPIC_OPPGAVE_ENDRET}")
    private val topicEndret: String? = null

    @Test
    fun `skal mappe og behandle oppgave endret hendelse`() {
        stubHentOppgave(emptyList())
        testDataGenerator.opprettJournalpost(createJournalpost(JOURNALPOST_ID_1, gjelderId = PERSON_IDENT_1))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET)
        val hendelseString = objectMapper.writeValueAsString(oppgaveHendelse)

        configureProducer()?.send(ProducerRecord(topicEndret, hendelseString))


        await.atMost(4, TimeUnit.SECONDS).untilAsserted {
            verifyOppgaveOpprettetWith(
                "\"oppgavetype\":\"JFR\"",
                "\"journalpostId\":\"${JOURNALPOST_ID_1}\"",
                "\"opprettetAvEnhetsnr\":\"9999\"",
                "\"prioritet\":\"HOY\"",
                "\"tema\":\"BID\""
            )
            verifyOppgaveNotEndret()
        }

    }


}
