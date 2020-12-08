package no.nav.bidrag.arbeidsflyt.hendelse;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.arbeidsflyt.service.HendelseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

  @Mock
  private HendelseService hendelseServiceMock;
  private JournalpostHendelseListener journalpostHendelseListener;

  @BeforeEach
  void initTestClass() {
    journalpostHendelseListener = new JournalpostHendelseListener(new ObjectMapper(), hendelseServiceMock);
  }

  @Test
  @DisplayName("skal behandle journalpost hendelse")
  void skalBehandleJournalpostHendelse() {
    journalpostHendelseListener.lesHendelse(String.format(HENDELSE_TEMPLATE, 1, "TEST_HENDELSE"));

    verify(hendelseServiceMock).behandleHendelse(new JournalpostHendelse("BID-1", "TEST_HENDELSE"));
  }

  @Test
  @DisplayName("skal ha sporingsdata på en hendelse")
  void skalHaSporingsdataPaHendelse() {
    journalpostHendelseListener.lesHendelse(String.format(HENDELSE_TEMPLATE, 2, "TEST_HENDELSE"));

    var argumentCaptor = ArgumentCaptor.forClass(JournalpostHendelse.class);

    verify(hendelseServiceMock).behandleHendelse(argumentCaptor.capture());

    assertThat(argumentCaptor.getValue().getSporing()).isEqualTo(new Sporingsdata("xyz", "nå"));
  }
}
