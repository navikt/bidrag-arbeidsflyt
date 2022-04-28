-- Slett alle tabeller for å bygge opp databasen på nytt
drop table if exists journalpost cascade;
drop table if exists oppgave cascade;
drop table if exists dead_letter_kafka cascade;
drop function if exists update_endret_timestamp();