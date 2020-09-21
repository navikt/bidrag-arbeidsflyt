package no.nav.bidrag.arbeidsflyt.consumer;

import static org.mockito.Mockito.verify;

import no.nav.bidrag.arbeidsflyt.dto.RegistrerJournalpost;
import no.nav.bidrag.arbeidsflyt.service.JournalpostService;
import no.nav.bidrag.hendelse.producer.dto.RegistrerJournalpostDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hendelser (arbeidsflyt test)")
class HendelserTest {

  @InjectMocks
  private Hendelser hendelser;

  @Mock
  private JournalpostService journalpostServiceMock;

  @Test
  @DisplayName("skal prosessere registrering av journalpost")
  void skalProsessereRegistreringAvJournalpost() {
    hendelser.utfor(new RegistrerJournalpostDto("101", "007"));

    verify(journalpostServiceMock).registrerJournalpost(new RegistrerJournalpost("101", "007"));
  }
}