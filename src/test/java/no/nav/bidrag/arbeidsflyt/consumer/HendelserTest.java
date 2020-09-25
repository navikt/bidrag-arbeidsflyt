package no.nav.bidrag.arbeidsflyt.consumer;

import no.nav.bidrag.arbeidsflyt.BidragArbeidsflyt;
import no.nav.bidrag.arbeidsflyt.produser.HendelserProducer;
import no.nav.bidrag.arbeidsflyt.service.JournalpostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {BidragArbeidsflyt.class})
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
        producer.sendMelding(1, "007");
        assertTrue(this.hendelser.getLatch().await(10, TimeUnit.SECONDS));
        verify(journalpostService).registrerJournalpost("1", "007");
    }
}