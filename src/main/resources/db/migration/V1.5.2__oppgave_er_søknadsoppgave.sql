alter table oppgave add column if not exists søknadsoppgave boolean;
alter table oppgave add column if not exists tildelt_enhetsnr text;
alter table behandling add column if not exists hendelse jsonb;
CREATE INDEX if not exists idx_gin_behandling_hendelse ON behandling USING GIN (hendelse);
