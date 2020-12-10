package no.nav.bidrag.arbeidsflyt.hendelse;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService;
import no.nav.bidrag.arbeidsflyt.service.HendelseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
@DisplayName("JournalpostHendelseListener")
class JournalpostHendelseListenerTest {
  private static final String HENDELSE_TEMPLATE = """
      {
        "journalpostId":"BID-%s",
        "hendelse":"%s",
        "sporing": {
          "correlationId":"xyz",
          "opprettet":"nå"
        }
      }
      """.stripIndent().trim();

  @Autowired
  private JournalpostHendelseListener journalpostHendelseListener;

  @MockBean
  private BehandleHendelseService behandleHendelseServiceMock;

  @Test
  @DisplayName("skal føre til mapping og behandling av journalpost hendelse")
  void skalForeTilMappingOgBehandlingAvJournalpostHendelse() {
    journalpostHendelseListener.lesHendelse(String.format(HENDELSE_TEMPLATE, 1, "TEST_HENDELSE"));

    verify(behandleHendelseServiceMock).behandleHendelse(new JournalpostHendelse("BID-1", "TEST_HENDELSE"));
  }

  @Test
  @DisplayName("skal ha sporingsdata i meldingen")
  void skalHaSporingsdataImeldingen() {
    journalpostHendelseListener.lesHendelse(String.format(HENDELSE_TEMPLATE, 2, "TEST_HENDELSE"));

    var argumentCaptor = ArgumentCaptor.forClass(JournalpostHendelse.class);

    verify(behandleHendelseServiceMock).behandleHendelse(argumentCaptor.capture());

    assertThat(argumentCaptor.getValue().getSporing()).isEqualTo(new Sporingsdata("xyz", "nå"));
  }
}
