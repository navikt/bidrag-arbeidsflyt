package no.nav.bidrag.arbeidsflyt.consumer;

import no.nav.bidrag.arbeidsflyt.service.JournalpostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
public class Hendelser {

  private static final Logger LOGGER = LoggerFactory.getLogger(Hendelser.class);

  private final JournalpostService journalpostService;

  private final CountDownLatch latch = new CountDownLatch(1);

  public CountDownLatch getLatch() {
    return latch;
  }

    Hendelser(JournalpostService journalpostService) {
    this.journalpostService = journalpostService;
  }

  @KafkaListener(topics = {"${kafka.topic}"})
  public void utfor(String melding) {
    LOGGER.info("prosesserer $registrerJournalpostDto");
    journalpostService.registrerJournalpost("1", "007");
    latch.countDown();
  }
}
