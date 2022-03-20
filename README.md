# bidrag-arbeidsflyt
Mikrotjeneste for administrering av arbeidsflyt i bidrag. Dette er en applikasjon som reagerer på hendelser definert i [bidrag-kafka](https://github.com/navikt/bidrag-kafka)

[![continuous integration](https://github.com/navikt/bidrag-arbeidsflyt/actions/workflows/ci.yaml/badge.svg)](https://github.com/navikt/bidrag-arbeidsflyt/actions/workflows/ci.yaml)
[![test build on pull request](https://github.com/navikt/bidrag-arbeidsflyt/actions/workflows/pr.yaml/badge.svg)](https://github.com/navikt/bidrag-arbeidsflyt/actions/workflows/pr.yaml)
[![release bidrag-arbeidsflyt](https://github.com/navikt/bidrag-arbeidsflyt/actions/workflows/release.yaml/badge.svg)](https://github.com/navikt/bidrag-arbeidsflyt/actions/workflows/release.yaml)

### bygg og kjør applikasjon

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
kcat -b localhost:9092 -t bidrag-journalpost -P -K:
````
og lim inn eks:
```bash
BID-2121212121:{"journalpostId":"BID-2121212121","aktorId":"2889800801806","fagomrade":"BID","enhet":"4806","journalstatus":"J","sporing":{"brukerident":"z992903","correlationId":"17f9db643c5-cuke"}}
```
og deretter trykk Ctrl+D. Da vil meldingen bli sendt til topic bidrag-journalpost