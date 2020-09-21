package no.nav.bidrag.arbeidsflyt.dto;

import java.util.Objects;
import no.nav.bidrag.hendelse.producer.dto.RegistrerJournalpostDto;

public class RegistrerJournalpost {

  private final String journalpostId;
  private final String saksnummer;

  public RegistrerJournalpost(CharSequence journalpostId, CharSequence saksnummer) {
    if (journalpostId == null) {
      throw new IllegalArgumentException("JournalpostId kan ikke være null!");
    }

    if (saksnummer == null) {
      throw new IllegalArgumentException("Saksnunmmer kan ikke være null!");
    }

    this.journalpostId = String.valueOf(journalpostId);
    this.saksnummer = String.valueOf(saksnummer);
  }

  public RegistrerJournalpost(RegistrerJournalpostDto registrerJournalpost) {
    this(registrerJournalpost.getJournalpostid(), registrerJournalpost.getSaksnummer());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RegistrerJournalpost that = (RegistrerJournalpost) o;

    return Objects.equals(journalpostId, that.journalpostId) &&
        Objects.equals(saksnummer, that.saksnummer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(journalpostId, saksnummer);
  }

  public String getJournalpostId() {
    return journalpostId;
  }

  public String getSaksnummer() {
    return saksnummer;
  }
}
