package no.nav.bidrag.arbeidsflyt.kafka;

import no.nav.bidrag.hendelse.producer.dto.RegistrerJournalpostDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class KafkaMeldingProducerTest {

  @Mock
  private KafkaTemplate<String, RegistrerJournalpostDto> kafkaTemplateMock;

  @Test
  @DisplayName("skal publisere melding med kafka template")
  public void skalPublisereMeldingMedKafkaTemplate() {

    // GIVEN
    final int journalpostID = 1;
    final String saksnummer = "1500000";
    final String kafkaTopic = "bidrag-registrert-journalpost-v2";

    // WHEN
    KafkaMeldingProducer producer = new KafkaMeldingProducer(kafkaTopic,
        kafkaTemplateMock);
    producer.sendMelding(journalpostID, saksnummer);

    Mockito.verify(kafkaTemplateMock)
        .send("bidrag-registrert-journalpost-v2", new RegistrerJournalpostDto(String.valueOf(journalpostID), saksnummer));
  }

  @Test
  @DisplayName("skal init klassen")
  public void skalInitKlassen() {
    // GIVEN
    final String kafkaTopic = "bidrag-registrert-journalpost-v2";

    // WHEN
    KafkaMeldingProducer producer = new KafkaMeldingProducer(kafkaTopic,
            kafkaTemplateMock);

    // THEN
    assertNotNull(producer);
  }

}
