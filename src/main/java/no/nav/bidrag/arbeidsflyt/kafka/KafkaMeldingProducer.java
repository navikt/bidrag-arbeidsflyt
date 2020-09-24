package no.nav.bidrag.arbeidsflyt.kafka;

import no.nav.bidrag.hendelse.producer.dto.RegistrerJournalpostDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaMeldingProducer {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMeldingProducer.class);
  private final String kafkaTopic;
  private KafkaTemplate<String, RegistrerJournalpostDto> kafkaTemplate;

  public KafkaMeldingProducer(@Value("${kafka.topic}") String kafkaTopic,
                              @Autowired KafkaTemplate<String, RegistrerJournalpostDto> kafkaTemplate) {
    this.kafkaTopic = kafkaTopic;
    this.kafkaTemplate = kafkaTemplate;
  }

  public void sendMelding(int journalpostid, String saksnummer) {
    LOGGER.info(String.format("Publiserer melding paa topic med journalpostid %s og saksnummer %s.", journalpostid, saksnummer));
    this.kafkaTemplate.send(this.kafkaTopic, new RegistrerJournalpostDto(String.valueOf(journalpostid), saksnummer));
  }
}
