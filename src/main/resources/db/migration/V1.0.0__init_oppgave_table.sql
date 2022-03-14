CREATE TABLE IF NOT EXISTS oppgave (
    oppgave_id numeric NOT NULL,
    journalpost_id varchar(30) NOT NULL,
    status varchar(20) NOT NULL,
    tema varchar(20) NOT NULL,
    oppgavetype varchar(20) NOT NULL,
    opprettet_timestamp timestamp DEFAULT now() NOT NULL,
    CONSTRAINT oppgave_pkey PRIMARY KEY (oppgave_id)
);

GRANT ALL PRIVILEGES ON TABLE public.oppgave TO cloudsqliamuser;