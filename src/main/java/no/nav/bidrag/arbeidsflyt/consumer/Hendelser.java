package no.nav.bidrag.arbeidsflyt.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.bidrag.arbeidsflyt.mapper.RegistrerJournalpostMapper;
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
  private final RegistrerJournalpostMapper registrerJournalpostMapper;

  private final CountDownLatch latch = new CountDownLatch(1);

  public CountDownLatch getLatch() {
    return latch;
  }

  Hendelser(JournalpostService journalpostService, RegistrerJournalpostMapper registrerJournalpostMapper) {
    this.registrerJournalpostMapper = registrerJournalpostMapper;
    this.journalpostService = journalpostService;
  }

  @KafkaListener(topics = {"${kafka.topic}"})
  public void utfor(String melding) throws JsonProcessingException {
    LOGGER.info("prosesserer $registrerJournalpostDto");
    journalpostService.registrerJournalpost(registrerJournalpostMapper.map(melding));
    latch.countDown();
  }
}
