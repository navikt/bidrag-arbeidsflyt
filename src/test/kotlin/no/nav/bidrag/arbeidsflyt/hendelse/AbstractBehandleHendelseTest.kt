package no.nav.bidrag.arbeidsflyt.hendelse

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import com.github.tomakehurst.wiremock.stubbing.Scenario
import no.nav.bidrag.arbeidsflyt.PROFILE_TEST
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.utils.AKTOER_ID
import no.nav.bidrag.arbeidsflyt.utils.ENHET_4806
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_1
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_1
import no.nav.bidrag.arbeidsflyt.utils.TestDataGenerator
import no.nav.bidrag.arbeidsflyt.utils.createJournalforendeEnheterResponse
import no.nav.bidrag.arbeidsflyt.utils.journalpostResponse
import no.nav.bidrag.arbeidsflyt.utils.oppgaveDataResponse
import no.nav.bidrag.domene.enums.diverse.Enhetsstatus
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.util.Arrays

@SpringBootTest
@ActiveProfiles(PROFILE_TEST)
@EnableMockOAuth2Server
@AutoConfigureWireMock(port = 0)
abstract class AbstractBehandleHendelseTest {
    @Autowired
    lateinit var testDataGenerator: TestDataGenerator

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun init() {
        testDataGenerator.deleteAll()
        stubHentPerson()
        stubOpprettOppgave()
        stubEndreOppgave()
        stubHentOppgaveSok()
        stubHentTemaTilgang()
        stubHentJournalforendeEnheter()
    }

    @AfterEach
    fun cleanupData() {
        WireMock.reset()
        WireMock.resetToDefault()
        testDataGenerator.deleteAll()
    }

    private fun aClosedJsonResponse(): ResponseDefinitionBuilder =
        WireMock
            .aResponse()
            .withHeader(HttpHeaders.CONNECTION, "close")
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")

    fun stubOpprettOppgave(
        oppgaveId: Long = OPPGAVE_ID_1,
        status: HttpStatus = HttpStatus.OK,
    ) {
        val responseBody =
            OppgaveData(
                id = OPPGAVE_ID_1,
                versjon = 1,
                aktoerId = AKTOER_ID,
                oppgavetype = "JFR",
                tema = "BID",
                tildeltEnhetsnr = "4833",
            )
        stubFor(
            post("/oppgave/api/v1/oppgaver/").willReturn(
                aClosedJsonResponse().withStatus(status.value()).withBody(objectMapper.writeValueAsString(responseBody)),
            ),
        )
    }

    fun stubEndreOppgave() {
        stubFor(patch(urlMatching("/oppgave/api/v1/oppgaver/.*")).willReturn(aClosedJsonResponse().withStatus(HttpStatus.OK.value())))
    }

    fun stubHentOppgaveSok(oppgaver: List<OppgaveData> = oppgaveDataResponse()) {
        stubFor(
            get(urlMatching("/oppgave/api/v1/oppgaver/\\?.*")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(objectMapper.writeValueAsString(OppgaveSokResponse(oppgaver = oppgaver, antallTreffTotalt = 10))),
            ),
        )
    }

    fun stubHentOppgave(
        oppgaveId: Long,
        oppgave: OppgaveData,
    ) {
        stubFor(
            get(urlMatching("/oppgave/api/v1/oppgaver/$oppgaveId")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(objectMapper.writeValueAsString(oppgave)),
            ),
        )
    }

    fun stubHentJournalpost(
        journalpostResponse: JournalpostResponse = journalpostResponse(),
        status: HttpStatus = HttpStatus.OK,
    ) {
        stubFor(
            get(urlMatching("/dokument/bidrag-dokument/journal/.*")).willReturn(
                aClosedJsonResponse().withStatus(status.value()).withBody(objectMapper.writeValueAsString(journalpostResponse)),
            ),
        )
    }

    fun stubHentOppgaveError() {
        stubFor(
            get(
                urlMatching("/oppgave/api/v1/oppgaver/.*"),
            ).willReturn(aClosedJsonResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())),
        )
    }

    fun stubHentOppgaveContaining(
        oppgaver: List<OppgaveData> = oppgaveDataResponse(),
        vararg params: Pair<String, String>,
    ) {
        var matchUrl = "/oppgave/api/v1/oppgaver/.*"
        params.forEach { matchUrl = "$matchUrl${it.first}=${it.second}.*" }
        val stub = get(urlMatching(matchUrl))
        stub.willReturn(
            aClosedJsonResponse()
                .withStatus(HttpStatus.OK.value())
                .withBody(objectMapper.writeValueAsString(OppgaveSokResponse(oppgaver = oppgaver, antallTreffTotalt = 10))),
        )
        stubFor(stub)
    }

    fun stubHentTemaTilgang(result: Boolean = true) {
        stubFor(
            post(urlMatching("/tilgangskontroll/api/tilgang/tema(.*)"))
                .willReturn(
                    aClosedJsonResponse().withStatus(HttpStatus.OK.value()).withBody(result.toString()),
                ),
        )
    }

    fun stubHentPerson(
        personId: String = PERSON_IDENT_1,
        aktorId: String = AKTOER_ID,
        status: HttpStatus = HttpStatus.OK,
        scenarioState: String? = null,
        nextScenario: String? = null,
    ) {
        stubFor(
            post(urlMatching("/person/bidrag-person/informasjon"))
                .inScenario("Hent person response")
                .whenScenarioStateIs(scenarioState ?: Scenario.STARTED)
                .willReturn(
                    aClosedJsonResponse()
                        .withStatus(status.value())
                        .withBody(objectMapper.writeValueAsString(PersonDto(ident = Personident(personId), aktørId = aktorId))),
                ).willSetStateTo(nextScenario),
        )
    }

    fun stubHentGeografiskEnhet(
        enhet: String = ENHET_4806,
        status: HttpStatus = HttpStatus.OK,
    ) {
        stubFor(
            post(urlMatching("/organisasjon/bidrag-organisasjon/arbeidsfordeling/enhet/geografisktilknytning")).willReturn(
                aClosedJsonResponse().withStatus(status.value()).withBody(
                    objectMapper.writeValueAsString(
                        EnhetDto(Enhetsnummer(enhet), "Enhetnavn"),
                    ),
                ),
            ),
        )
    }

    fun stubHentEnhet(
        enhet: String = ENHET_4806,
        erNedlagt: Boolean = false,
        status: HttpStatus = HttpStatus.OK,
    ) {
        stubFor(
            get(urlMatching("/organisasjon/bidrag-organisasjon/enhet/info/.*")).willReturn(
                aClosedJsonResponse().withStatus(status.value()).withBody(
                    objectMapper.writeValueAsString(
                        EnhetDto(
                            Enhetsnummer(enhet),
                            "Enhetnavn",
                            status = if (erNedlagt) Enhetsstatus.NEDLAGT else Enhetsstatus.AKTIV,
                        ),
                    ),
                ),
            ),
        )
    }

    fun stubHentJournalforendeEnheter() {
        stubFor(
            get(urlEqualTo("/organisasjon/bidrag-organisasjon/arbeidsfordeling/enhetsliste/journalforende")).willReturn(
                aClosedJsonResponse().withStatus(HttpStatus.OK.value()).withBody(
                    objectMapper.writeValueAsString(
                        createJournalforendeEnheterResponse(),
                    ),
                ),
            ),
        )
    }

    fun verifyHentGeografiskEnhetKalt(antall: Int = 1) {
        verify(antall, postRequestedFor(urlMatching("/organisasjon/bidrag-organisasjon/arbeidsfordeling/enhet/geografisktilknytning")))
    }

    fun verifyHentJournalforendeEnheterKalt(antall: Int = 1) {
        verify(antall, getRequestedFor(urlEqualTo("/organisasjon/bidrag-organisasjon/arbeidsfordeling/enhetsliste/journalforende")))
    }

    fun verifyHentPersonKalt(antall: Int = 1) {
        verify(antall, postRequestedFor(urlMatching("/person.*")))
    }

    fun verifySjekkTematilgangKalt(
        antall: Int = 1,
        saksbehandlerId: String,
    ) {
        verify(antall, postRequestedFor(urlEqualTo("/tilgangskontroll/api/tilgang/tema?navIdent=$saksbehandlerId")))
    }

    fun verifyHentPersonKaltMedFnr(fnr: String) {
        verify(1, postRequestedFor(urlEqualTo("/person/bidrag-person/informasjon")).withRequestBody(ContainsPattern(fnr)))
    }

    fun verifyOppgaveNotOpprettet() {
        verify(0, postRequestedFor(urlMatching("/oppgave/api/v1/oppgaver/")))
    }

    fun verifyOppgaveNotEndret() {
        verify(0, WireMock.patchRequestedFor(urlMatching("/oppgave/api/v1/oppgaver/.*")))
    }

    fun verifyOppgaveOpprettetWith(vararg contains: String) {
        val requestPattern = postRequestedFor(urlMatching("/oppgave/api/v1/oppgaver/"))
        Arrays.stream(contains).forEach { contain: String? ->
            requestPattern.withRequestBody(
                ContainsPattern(contain),
            )
        }
        verify(1, requestPattern)
    }

    fun verifyDokumentHentet(count: Int? = 1) {
        verify(count ?: 1, getRequestedFor(urlMatching("/dokument/bidrag-dokument/journal/.*")))
    }

    fun verifyOppgaveEndretWith(
        count: Int? = null,
        vararg contains: String,
    ) {
        val requestPattern = WireMock.patchRequestedFor(urlMatching("/oppgave/api/v1/oppgaver/.*"))
        Arrays.stream(contains).forEach { contain: String? ->
            requestPattern.withRequestBody(
                ContainsPattern(contain),
            )
        }
        if (count != null) verify(count, requestPattern) else verify(requestPattern)
    }
}
