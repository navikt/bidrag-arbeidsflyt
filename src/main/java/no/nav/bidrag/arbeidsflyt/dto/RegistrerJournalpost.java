package no.nav.bidrag.arbeidsflyt.dto;

import java.util.Objects;

public class RegistrerJournalpost {

  private String journalpostId;
  private String Saksnummer;

  public RegistrerJournalpost() {
  }

  public RegistrerJournalpost(String journalpostId, String saksnummer) {
    this.journalpostId = journalpostId;
    this.Saksnummer = saksnummer;
  }

  public String getJournalpostId() {
    return journalpostId;
  }

  public void setJournalpostId(String journalpostId) {
    this.journalpostId = journalpostId;
  }

  public String getSaksnummer() {
    return Saksnummer;
  }

  public void setSaksnummer(String saksnummer) {
    Saksnummer = saksnummer;
  }

  @Override
  public String toString() {
    return "RegistrerJournalpost{" +
        "journalpostId='" + journalpostId + '\'' +
        ", Saksnummer='" + Saksnummer + '\'' +
        '}';
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
        Objects.equals(Saksnummer, that.Saksnummer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(journalpostId, Saksnummer);
  }
}
