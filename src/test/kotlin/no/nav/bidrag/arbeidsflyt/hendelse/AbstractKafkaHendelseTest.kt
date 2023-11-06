package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.PROFILE_KAFKA_TEST
import no.nav.bidrag.arbeidsflyt.PROFILE_TEST
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.ActiveProfiles
import java.util.Collections

@SpringBootTest
@ActiveProfiles(value = [PROFILE_KAFKA_TEST, PROFILE_TEST])
@DisplayName("OppgaveEndretHendelseListenerTest")
@EmbeddedKafka(partitions = 1, topics = ["topic_journalpost", "oppgave-hendelse"], bootstrapServersProperty = "KAFKA_BROKERS")
abstract class AbstractKafkaHendelseTest : AbstractBehandleHendelseTest() {
    @Autowired
    lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    fun configureConsumer(topic: String): Consumer<Int, String>? {
        val consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker)
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        val consumer: Consumer<Int, String> =
            DefaultKafkaConsumerFactory<Int, String>(consumerProps)
                .createConsumer()
        consumer.subscribe(Collections.singleton(topic))
        return consumer
    }

    fun configureProducer(): Producer<String, String>? {
        val props = KafkaTestUtils.producerProps(embeddedKafkaBroker)
        props.replace("key.serializer", StringSerializer::class.java)
        val producerProps: Map<String, Any> = HashMap(props)
        return DefaultKafkaProducerFactory<String, String>(producerProps).createProducer()
    }
}
