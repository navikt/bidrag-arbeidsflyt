package no.nav.bidrag.arbeidsflyt.consumer;

import no.nav.bidrag.arbeidsflyt.dto.RegistrerJournalpost;
import no.nav.bidrag.arbeidsflyt.service.JournalpostService;
import no.nav.bidrag.hendelse.producer.dto.RegistrerJournalpostDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class Hendelser {

  private static final Logger LOGGER = LoggerFactory.getLogger(Hendelser.class);

  private final JournalpostService journalpostService;

  Hendelser(JournalpostService journalpostService) {
    this.journalpostService = journalpostService;
  }

  @KafkaListener(topics = {"bidrag-registrert-journalpost-v2"}, groupId = "bidrag-arbeidsflyt")
  public void utfor(RegistrerJournalpostDto registrerJournalpostDto) {
    LOGGER.info("prosesserer $registrerJournalpostDto");

    journalpostService.registrerJournalpost(new RegistrerJournalpost(registrerJournalpostDto));
  }
}
