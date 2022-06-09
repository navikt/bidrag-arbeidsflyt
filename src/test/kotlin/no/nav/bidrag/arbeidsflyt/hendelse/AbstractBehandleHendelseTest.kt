package no.nav.bidrag.arbeidsflyt.hendelse

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import no.nav.bidrag.arbeidsflyt.PROFILE_TEST
import no.nav.bidrag.arbeidsflyt.dto.HentPersonResponse
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.model.GeografiskTilknytningResponse
import no.nav.bidrag.arbeidsflyt.utils.AKTOER_ID
import no.nav.bidrag.arbeidsflyt.utils.ENHET_4806
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_1
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_1
import no.nav.bidrag.arbeidsflyt.utils.TestDataGenerator
import no.nav.bidrag.arbeidsflyt.utils.oppgaveDataResponse
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
    fun init(){
        stubHentPerson()
        stubOpprettOppgave()
        stubEndreOppgave()
        stubHentOppgave()
    }

    @AfterEach
    fun cleanupData(){
        WireMock.reset()
        WireMock.resetToDefault()
        testDataGenerator.deleteAll()
    }

    private fun aClosedJsonResponse(): ResponseDefinitionBuilder {
        return WireMock.aResponse()
            .withHeader(HttpHeaders.CONNECTION, "close")
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
    }

    fun stubOpprettOppgave(oppgaveId: Long = OPPGAVE_ID_1){
        val responseBody = OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            aktoerId = AKTOER_ID,
            oppgavetype = "JFR",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        )
        stubFor(post("/oppgave/api/v1/oppgaver/").willReturn(aClosedJsonResponse().withStatus(HttpStatus.OK.value()).withBody(objectMapper.writeValueAsString(responseBody))))
    }

    fun stubEndreOppgave(){
        stubFor(patch(urlMatching("/oppgave/api/v1/oppgaver/.*")).willReturn(aClosedJsonResponse().withStatus(HttpStatus.OK.value())))
    }

    fun stubHentOppgave(oppgaver: List<OppgaveData> = oppgaveDataResponse()){
        stubFor(get(urlMatching("/oppgave/api/v1/oppgaver/.*")).willReturn(aClosedJsonResponse().withStatus(HttpStatus.OK.value()).withBody(objectMapper.writeValueAsString(OppgaveSokResponse(oppgaver = oppgaver, antallTreffTotalt = 10)))))
    }

    fun stubHentOppgaveError(){
        stubFor(get(urlMatching("/oppgave/api/v1/oppgaver/.*")).willReturn(aClosedJsonResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())))
    }

    fun stubHentOppgaveContaining(oppgaver: List<OppgaveData> = oppgaveDataResponse(), vararg params: Pair<String, String>){
        var matchUrl = "/oppgave/api/v1/oppgaver/.*"
        params.forEach { matchUrl = "$matchUrl${it.first}=${it.second}.*"}
        val stub = get(urlMatching(matchUrl))
        stub.willReturn(aClosedJsonResponse().withStatus(HttpStatus.OK.value()).withBody(objectMapper.writeValueAsString(OppgaveSokResponse(oppgaver = oppgaver, antallTreffTotalt = 10))))
        stubFor(stub)
    }

    fun stubHentPerson(personId: String = PERSON_IDENT_1, aktorId: String = AKTOER_ID, status: HttpStatus = HttpStatus.OK){
        stubFor(get(urlMatching("/person.*")).willReturn(aClosedJsonResponse().withStatus(status.value()).withBody(objectMapper.writeValueAsString(HentPersonResponse(personId, aktorId)))))
    }

    fun stubHentGeografiskEnhet(enhet: String = ENHET_4806){
        stubFor(get(urlMatching("/organisasjon.*")).willReturn(aClosedJsonResponse().withStatus(HttpStatus.OK.value()).withBody(objectMapper.writeValueAsString(
            GeografiskTilknytningResponse(enhet, "Enhetnavn")
        ))))
    }

    fun verifyHentGeografiskEnhetKalt(antall: Int = 1){
        WireMock.verify(antall, WireMock.getRequestedFor(urlMatching("/organisasjon.*")))
    }
    fun verifyHentPersonKalt(antall: Int = 1){
        WireMock.verify(antall, WireMock.getRequestedFor(urlMatching("/person.*")))
    }
    fun verifyOppgaveNotOpprettet(){
        WireMock.verify(0, WireMock.postRequestedFor(WireMock.urlMatching("/oppgave/api/v1/oppgaver/")))
    }

    fun verifyOppgaveNotEndret(){
        WireMock.verify(0, WireMock.patchRequestedFor(WireMock.urlMatching("/oppgave/api/v1/oppgaver/.*")))
    }

    fun verifyOppgaveOpprettetWith(vararg contains: String){
        val requestPattern = WireMock.postRequestedFor(WireMock.urlMatching("/oppgave/api/v1/oppgaver/"))
        Arrays.stream(contains).forEach { contain: String? ->
            requestPattern.withRequestBody(
                ContainsPattern(contain)
            )
        }
        WireMock.verify(1, requestPattern)
    }

    fun verifyOppgaveEndretWith(count: Int? = null, vararg contains: String){
        val requestPattern = WireMock.patchRequestedFor(WireMock.urlMatching("/oppgave/api/v1/oppgaver/.*"))
        Arrays.stream(contains).forEach { contain: String? ->
            requestPattern.withRequestBody(
                ContainsPattern(contain)
            )
        }
        if (count != null) WireMock.verify(count, requestPattern) else WireMock.verify(requestPattern)
    }
}