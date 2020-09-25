package no.nav.bidrag.arbeidsflyt.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class HendelserProducer {

  private final String kafkaTopic;
  private final KafkaTemplate<String, String> kafkaTemplate;

  public HendelserProducer(
      @Value("${kafka.topic}") String kafkaTopic,
      @Autowired KafkaTemplate<String, String> kafkaTemplate
  ) {
    this.kafkaTopic = kafkaTopic;
    this.kafkaTemplate = kafkaTemplate;
  }

  public void sendMelding(String melding) {
    this.kafkaTemplate.send(this.kafkaTopic, melding);
  }
}
