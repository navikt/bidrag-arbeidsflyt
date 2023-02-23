package no.nav.bidrag.arbeidsflyt.hendelse.featuretoggle

import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.dto.Oppgavestatuskategori
import no.nav.bidrag.arbeidsflyt.hendelse.AbstractKafkaHendelseTest
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_1
import no.nav.bidrag.arbeidsflyt.utils.createOppgaveHendelse
import org.apache.kafka.clients.producer.ProducerRecord
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

    @Test
    fun `skal ikke mappe og behandle oppgave endret hendelse`() {
        val oppgaveId = 239999L
        val oppgaveHendelse = createOppgaveHendelse(oppgaveId, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT, statuskategori = Oppgavestatuskategori.AVSLUTTET)
        val hendelseString = objectMapper.writeValueAsString(oppgaveHendelse)

        configureProducer()?.send(ProducerRecord(topicEndret, hendelseString))

        await.atLeast(3, TimeUnit.SECONDS)

        verifyOppgaveNotOpprettet()
    }
}