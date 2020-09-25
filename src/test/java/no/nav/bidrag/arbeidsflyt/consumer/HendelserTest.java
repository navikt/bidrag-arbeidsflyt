package no.nav.bidrag.arbeidsflyt.consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;
import no.nav.bidrag.arbeidsflyt.BidragArbeidsflyt;
import no.nav.bidrag.arbeidsflyt.dto.RegistrerJournalpost;
import no.nav.bidrag.arbeidsflyt.producer.HendelserProducer;
import no.nav.bidrag.arbeidsflyt.service.JournalpostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest()
@DirtiesContext
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
public class HendelserTest {

  @Autowired
  private Hendelser hendelser;

  @Autowired
  private HendelserProducer producer;

  @MockBean
  private JournalpostService journalpostService;

  @Test
  public void testAtDeterMuligAPublisereOgLytteTilMeldingOmRegistreringAvJournalpostIKafkaTopic() throws Exception {
    String jsonMeldingITopic = """
        {"journalpostId":"1", "saksnummer":"007"}
        """.stripIndent();

    producer.sendMelding(jsonMeldingITopic);

    assertAll(
        () -> assertTrue(this.hendelser.getLatch().await(10, TimeUnit.SECONDS)),
        () -> verify(journalpostService).registrerJournalpost(new RegistrerJournalpost("1", "007"))
    );
  }
}
