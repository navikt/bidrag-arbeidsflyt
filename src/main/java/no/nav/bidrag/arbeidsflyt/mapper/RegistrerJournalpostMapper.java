package no.nav.bidrag.arbeidsflyt.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.arbeidsflyt.dto.RegistrerJournalpost;
import org.springframework.stereotype.Component;

@Component
public class RegistrerJournalpostMapper {

  private final ObjectMapper objectMapper;

  public RegistrerJournalpostMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public RegistrerJournalpost map(String registrerJournalpostAsJsonString) throws JsonProcessingException {
    return objectMapper.readValue(registrerJournalpostAsJsonString, RegistrerJournalpost.class);
  }
}
