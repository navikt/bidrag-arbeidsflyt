package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.PROFILE_KAFKA_TEST
import no.nav.bidrag.arbeidsflyt.PROFILE_TEST
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.ActiveProfiles
import java.util.Collections


@SpringBootTest
@ActiveProfiles(value = arrayOf(PROFILE_KAFKA_TEST, PROFILE_TEST))
@EnableMockOAuth2Server
@AutoConfigureWireMock(port = 0)
@DisplayName("OppgaveEndretHendelseListenerTest")
@EmbeddedKafka(partitions = 1, brokerProperties = arrayOf("listeners=PLAINTEXT://localhost:9092", "port=9092"))
internal class OppgaveHendelseListenerTest {

    @Autowired
    var embeddedKafkaBroker: EmbeddedKafkaBroker? = null

    @Value("\${TOPIC_OPPGAVE_ENDRET}")
    private val topic: String? = null

    private val oppgaveHendelseMessage =
        "{\"id\":338307665," +
                "\"tildeltEnhetsnr\":\"4806\"," +
                "\"endretAvEnhetsnr\":\"4806\"," +
                "\"saksreferanse\":\"100026952\"," +
                "\"tilordnetRessurs\":\"Z999999\"," +
                "\"tema\":\"BID\"," +
                "\"behandlingstema\":\"ab0096\"," +
                "\"oppgavetype\":\"JFR\"," +
                "\"behandlingstype\":\"ae0118\"," +
                "\"versjon\":3," +
                "\"beskrivelse\":\"Beskrivelse på oppgave\"," +
                "\"fristFerdigstillelse\":\"2021-12-07\"," +
                "\"aktivDato\":\"2021-12-07\"," +
                "\"opprettetTidspunkt\":\"2021-12-07T12:51:49.611+01:00\"," +
                "\"opprettetAv\":\"srvBisys\"," +
                "\"endretAv\":\"Z994725\"," +
                "\"endretTidspunkt\":\"2021-12-07T13:21:31.51+01:00\"," +
                "\"prioritet\":\"HOY\"," +
                "\"status\":\"AAPNET\"," +
                "\"statuskategori\":\"AAPEN\"," +
                "\"ident\":{\"identType\":\"AKTOERID\",\"verdi\":\"12121212\",\"folkeregisterident\":\"12121212\",\"registrert_dato\":{\"year\":2021,\"month\":\"DECEMBER\",\"monthValue\":12,\"dayOfMonth\":1,\"chronology\":{\"calendarType\":\"iso8601\",\"id\":\"ISO\"},\"era\":\"CE\",\"dayOfWeek\":\"WEDNESDAY\",\"leapYear\":false,\"dayOfYear\":335}}," +
                "\"metadata\":{}}"

    @Test
    fun `skal mappe og behandle OppgaveEndret hendelse`() {
        configureProducer()?.send(ProducerRecord(topic, oppgaveHendelseMessage))
        val singleRecord = KafkaTestUtils.getSingleRecord(configureConsumer(), topic)
        assertThat(singleRecord).isNotNull
        assertThat(singleRecord.value()).isEqualTo(oppgaveHendelseMessage)
        // Verify when implemented
    }

    fun configureConsumer(): Consumer<Int, String>? {
        val consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker)
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        val consumer: Consumer<Int, String> = DefaultKafkaConsumerFactory<Int, String>(consumerProps)
            .createConsumer()
        consumer.subscribe(Collections.singleton(topic))
        return consumer
    }

    fun configureProducer(): Producer<Int, String>? {
        val producerProps: Map<String, Any> = HashMap(KafkaTestUtils.producerProps(embeddedKafkaBroker))
        return DefaultKafkaProducerFactory<Int, String>(producerProps).createProducer()
    }
}
