package no.nav.bidrag.arbeidsflyt.dto;

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
}
