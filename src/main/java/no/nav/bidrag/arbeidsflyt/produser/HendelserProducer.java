package no.nav.bidrag.arbeidsflyt.produser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class HendelserProducer {

  private static final Logger LOGGER = LoggerFactory.getLogger(HendelserProducer.class);
  private final String kafkaTopic;
  private KafkaTemplate<String, String> kafkaTemplate;

  public HendelserProducer(@Value("${kafka.topic}") String kafkaTopic,
                           @Autowired KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTopic = kafkaTopic;
    this.kafkaTemplate = kafkaTemplate;
  }

  public void sendMelding(String melding) {
    this.kafkaTemplate.send(this.kafkaTopic, melding);
  }
}
