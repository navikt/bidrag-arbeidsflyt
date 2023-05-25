package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.dto.Oppgavestatuskategori
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_1
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_1
import no.nav.bidrag.arbeidsflyt.utils.createOppgaveHendelse
import no.nav.bidrag.arbeidsflyt.utils.journalpostResponse
import no.nav.bidrag.arbeidsflyt.utils.toOppgaveData
import no.nav.bidrag.dokument.dto.Journalstatus
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.TimeUnit

internal class KafkaOppgaveHendelseListenerTest : AbstractKafkaHendelseTest() {

    @Value("\${TOPIC_OPPGAVE_ENDRET}")
    private val topicEndret: String? = null

    @Test
    fun `skal mappe og behandle oppgave endret hendelse`() {
        stubHentOppgaveSok(emptyList())
        stubHentGeografiskEnhet()
        stubHentJournalpost(journalpostResponse(journalStatus = Journalstatus.MOTTATT))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET)
        stubHentOppgave(oppgaveHendelse.id, oppgaveHendelse.toOppgaveData())
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

    @Test
    fun `skal lagre hendelse i dead letter repository ved feil`() {
        stubHentOppgaveError()
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET)
        stubHentOppgave(oppgaveHendelse.id, oppgaveHendelse.toOppgaveData())
        val hendelseString = objectMapper.writeValueAsString(oppgaveHendelse)

        configureProducer()?.send(ProducerRecord(topicEndret, hendelseString))

        await.atMost(4, TimeUnit.SECONDS).untilAsserted {
            assertThat(testDataGenerator.hentDlKafka().size).isEqualTo(1)
        }
    }
}
