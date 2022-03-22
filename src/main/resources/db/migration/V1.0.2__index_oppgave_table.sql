CREATE INDEX idx_oppgave_id ON oppgave(oppgave_id);
CREATE INDEX idx_oppgave_jpid ON oppgave(journalpost_id);
CREATE INDEX idx_oppgave_status ON oppgave(status);
CREATE INDEX idx_oppgave_statuskat ON oppgave(statuskategori);
CREATE INDEX idx_oppgave_type ON oppgave(oppgavetype);
