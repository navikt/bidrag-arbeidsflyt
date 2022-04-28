CREATE TABLE IF NOT EXISTS oppgave (
       oppgave_id numeric NOT NULL,
       status varchar(20) NOT NULL,
       oppgavetype varchar(20) NOT NULL,
       journalpost_id varchar(30) NULL,
       opprettet_timestamp timestamp DEFAULT now() NOT NULL,
       endret_timestamp timestamp DEFAULT now() NOT NULL,
       CONSTRAINT oppgave_pkey PRIMARY KEY (oppgave_id)
);

CREATE TRIGGER update_oppgave_endret BEFORE UPDATE ON oppgave FOR EACH ROW EXECUTE PROCEDURE update_endret_timestamp();

GRANT ALL PRIVILEGES ON TABLE public.oppgave TO cloudsqliamuser;

CREATE INDEX idx_oppgave_id ON oppgave(oppgave_id);