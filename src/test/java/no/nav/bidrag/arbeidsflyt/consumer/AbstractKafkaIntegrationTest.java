package no.nav.bidrag.arbeidsflyt.consumer;

import no.nav.bidrag.arbeidsflyt.BidragArbeidsflytLocal;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;

@SpringBootTest(classes = BidragArbeidsflytLocal.class)
public abstract class AbstractKafkaIntegrationTest {

  static KafkaContainer kafkaContainer = new KafkaContainer();

  @DynamicPropertySource
  static void kafkaProperties(DynamicPropertyRegistry registry) {
    kafkaContainer.start();
    registry.add("spring.kafka.properties.bootstrap.servers", kafkaContainer::getBootstrapServers);
    registry.add("spring.kafka.consumer.properties.auto.offset.reset", () -> "earliest");

  }
}