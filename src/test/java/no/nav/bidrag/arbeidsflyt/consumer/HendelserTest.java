package no.nav.bidrag.arbeidsflyt.consumer;

import no.nav.bidrag.arbeidsflyt.BidragArbeidsflyt;
import no.nav.bidrag.arbeidsflyt.dto.RegistrerJournalpost;
import no.nav.bidrag.arbeidsflyt.kafka.KafkaMeldingProducer;
import no.nav.bidrag.arbeidsflyt.service.JournalpostService;
import no.nav.bidrag.arbeidsflyt.util.SpringBootEmbeddedKafka;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {BidragArbeidsflyt.class})
public class HendelserTest extends SpringBootEmbeddedKafka {

    @Autowired
    private Hendelser hendelser;

    @Autowired
    private KafkaMeldingProducer sender;

    @MockBean
    JournalpostService journalpostService;

    @Value("${kafkatest.topic}")
    private String topic;

    @Test
    public void testReceive() throws Exception {
        template.send(topic, "Sending with default template");

        hendelser.getLatch().await(10000, TimeUnit.MILLISECONDS);
        assertThat(hendelser.getLatch().getCount(), equalTo(0L));
    }

    @Test
    public void testSend() throws Exception {
        sender.sendMelding(01,"007");

        hendelser.getLatch().await(10000, TimeUnit.MILLISECONDS);
        verify(journalpostService).registrerJournalpost(new RegistrerJournalpost("01","007"));
    }
}