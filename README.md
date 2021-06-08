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
