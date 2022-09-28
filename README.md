# bidrag-arbeidsflyt
Mikrotjeneste for administrering av arbeidsflyt i bidrag. Dette er en applikasjon som reagerer på hendelser definert i [bidrag-kafka](https://github.com/navikt/bidrag-kafka)

[![continuous integration](https://github.com/navikt/bidrag-arbeidsflyt/actions/workflows/ci.yaml/badge.svg)](https://github.com/navikt/bidrag-arbeidsflyt/actions/workflows/ci.yaml)
[![test build on pull request](https://github.com/navikt/bidrag-arbeidsflyt/actions/workflows/pr.yaml/badge.svg)](https://github.com/navikt/bidrag-arbeidsflyt/actions/workflows/pr.yaml)
[![release bidrag-arbeidsflyt](https://github.com/navikt/bidrag-arbeidsflyt/actions/workflows/release.yaml/badge.svg)](https://github.com/navikt/bidrag-arbeidsflyt/actions/workflows/release.yaml)

Bidrag arbeidsflyt sørger for at mottatte Bidrag journalposter alltid har journalføringsoppgaver og at oppgavene er i synk med journalposten (gjelder, enhet osv).<br/>
Arbeidsflyt vil lukke journalføringsoppgave hvis journalpost er journalført eller er overført til annen tema

Bidrag arbeisflyt lytter på `oppgave-endret` og `oppgave-opprettet` kafka hendelser for sjekke om en oppgave blir lukket før journalpost er journalført.
Hvis oppgave er lukket men journalposten ikke er journalført vil arbeidsflyt automatisk opprette ny journalføringsoppgave for journalposten.

### Database og Kafka
Arbeidsflyt lagrer journalposter med status mottatt og åpne journalføringsoppgaver i databasen. Dette brukes for å sjekke status på journalposten og for å kunne sjekke om en oppgave er endret fra journalføringsoppgave til noe annet.
Denne informasjonen brukes da for å bestemme om en ny journalføringsoppgave skal opprettes eller ikke. 

Journalpost slettes fra `journalpost` tabellen hvis journalpost ikke lenger har status mottatt eller ikke lenger har tema BID/FAR. Dette vil føre til at det ikke vil sjekkes for om journalposten mangler journalføringsoppgave eller ikke.

Hvis en journalføringsoppgave skal lukkes uten at journalpost er journalført/endret tema må journalpost slettes fra `journalpost` tabellen. 
Ellers vil arbeidsflyt opprette ny journalføringsoppgave med engang journalføringsoppgaven lukkes.

#### Feilhåndtering
Alle kafka topics (opprett-oppgave, endret-oppgave, bidrag.journalpost) har samme feilhåndtering. Hvis behandling av melding feiler vil meldingen forsøkes å behandles på nytt X antall ganger med eksponensiell backoff policy. X er en miljøvariabel `KAFKA_MAX_RETRY` som er satt i [application.yaml](src/main/resources/application.yaml). Denne er satt nå til 15.
Hvis behandlingen av melding fortsatt feiler etter X antall forsøk vil meldingen lagres i `dead_letter_kafka` tabellen og kafka lytteren vil fortsette å lese neste melding i topicen.

##### Prosessering av feilede meldinger (DLQ)
All feilede kafka meldinger lagres i tabellen `dead_letter_kafka`. Scheduler i klassen [KafkaDLQRetryScheduler](src/main/kotlin/no/nav/bidrag/arbeidsflyt/hendelse/KafkaDLQRetryScheduler.kt) sjekker hver 30.min for rader med `retry=true` og prøver å prosessere feilede meldinger på nytt.
Hvis prosessering av melding feiler vil `retry_count` inkrementeres med 1. Hvis `retry_count` er høyere enn `SCHEDULER_MAX_RETRY` som er satt i [application.yaml](src/main/resources/application.yaml) vil `retry` settes til `false`
Retry må manuelt settes til true og `retry_count=0` for at arbeidsflyt skal prøve å prosessere melding på nytt.

Følg denne guiden for å koble deg til databasen https://doc.nais.io/persistence/postgres/#personal-database-access 
### Bygg og kjør applikasjon

Dette er en spring-boot applikasjon og kan kjøres som ren java applikasjon, ved å
bruke `maven` eller ved å bygge et docker-image og kjøre dette 

##### java og maven
* krever installasjon av java og maven

`mvn clean install`<br>
deretter<br>
`mvn spring-boot:run`<br>
eller<br>
`cd target`<br>
`java -jar bidrag-arbeidsflyt-<versjon>.jar`

##### docker og maven
* krever installasjon av java, maven og docker
* docker image er det som blir kjørt som nais applikasjon

`mvn clean install`<br>
deretter<br>
`docker build -t bidrag-arbeidsflyt .`<br>
`docker run -p bidrag-arbeidsflyt`




### Lokal utvikling

#### Kjør lokalt med kafka
Start kafka lokalt i en docker container med følgende kommando på root mappen
````bash
docker-compose up -d
````
Start opp applikasjon ved å kjøre BidragArbeidsflytLocal.kt under test/kotlin mappen.
Når du starter applikasjon må følgende miljøvariabler settes
```bash
-DAZURE_APP_CLIENT_SECRET=<secret>
-DAZURE_APP_CLIENT_ID=<secret>
```
Bruk `kcat` til å sende meldinger til kafka topic. Feks

````bash
kcat -b 0.0.0.0:9092 -t bidrag-journalpost -P -K:
````
og lim inn eks:
```bash
BID-2121212121:{"journalpostId":"BID-2121212121","aktorId":"2889800801806","fagomrade":"BID","enhet":"4806","journalstatus":"M","sporing":{"brukerident":"z992903","correlationId":"localtest"}}
```
```bash
JOARK-12345678:{"journalpostId":"JOARK-12345678","fnr":"17478549350","fagomrade":"BID","enhet":"4806","journalstatus":"M","sporing":{"brukerident":"z992903","correlationId":"localtest"}}
```
og deretter trykk Ctrl+D. Da vil meldingen bli sendt til topic bidrag-journalpost

For oppgave 
````bash
kcat -b 0.0.0.0:9092 -t bidrag-opprettet -P
````
og lim inn eks:
```bash
{"id": 351382364, "tildeltEnhetsnr": "4806", "opprettetAvEnhetsnr": "4806",  "journalpostId": "573782796", "aktoerId": "2578652659686", "beskrivelse": "Test kopier dokumenter til Bidrag", "tema": "BID", "oppgavetype": "VUR", "versjon": 1, "opprettetAv": "srvbisys", "prioritet": "HOY", "status": "OPPRETTET"}
```