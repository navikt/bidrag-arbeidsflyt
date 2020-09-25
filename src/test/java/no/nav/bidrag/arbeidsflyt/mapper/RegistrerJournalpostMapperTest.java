package no.nav.bidrag.arbeidsflyt.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.bidrag.arbeidsflyt.consumer.Hendelser;
import no.nav.bidrag.arbeidsflyt.dto.RegistrerJournalpost;
import no.nav.bidrag.arbeidsflyt.producer.HendelserProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest()
class RegistrerJournalpostMapperTest {

  @Autowired
  private RegistrerJournalpostMapper registrerJournalpostMapper;

  @MockBean
  private Hendelser hendelserMock;
  @MockBean
  private HendelserProducer hendelserProducerMock;

  @Test
  public void gittStringMeldingMedJournalpostIdOgSaksnummerSkalReturnereRegistrerJournalpost() throws JsonProcessingException {
    // GIVEN
    var registrerJournalpostAsJsonString = """
        {"journalpostId":"1", "saksnummer":"07"}
        """.stripIndent();

    // WHEN
    final RegistrerJournalpost registrerJournalpost = registrerJournalpostMapper.map(registrerJournalpostAsJsonString);

    // THEN
    assertAll(
        () -> assertThat(registrerJournalpost).extracting(RegistrerJournalpost::getJournalpostId).isEqualTo("1"),
        () -> assertThat(registrerJournalpost).extracting(RegistrerJournalpost::getSaksnummer).isEqualTo("07")
    );
  }
}
