package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.dto.OpprettOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.PatchOppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.OppgaveDataForHendelse
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod

private const val OPPGAVE_CONTEXT = "/api/v1/oppgaver/"

interface OppgaveConsumer {
    fun finnOppgaverForJournalpost(oppgaveSokRequest: OppgaveSokRequest): OppgaveSokResponse
    fun endreOppgave(patchOppgaveRequest: PatchOppgaveRequest)
    fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): OppgaveDataForHendelse
}

class DefaultOppgaveConsumer(private val restTemplate: HttpHeaderRestTemplate) : OppgaveConsumer {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(DefaultOppgaveConsumer::class.java)
    }

    override fun finnOppgaverForJournalpost(oppgaveSokRequest: OppgaveSokRequest): OppgaveSokResponse {
        val parameters = oppgaveSokRequest.hentParametre()

        LOGGER.info("søk opp åpne oppgaver på en journalpost: $parameters")

        val oppgaveSokResponseEntity = restTemplate.exchange(
            "$OPPGAVE_CONTEXT?$parameters",
            HttpMethod.GET,
            null,
            OppgaveSokResponse::class.java
        )

        LOGGER.info("Response: ${oppgaveSokResponseEntity.statusCode}, ${initStringOf(oppgaveSokResponseEntity.body)}")

        return oppgaveSokResponseEntity.body ?: OppgaveSokResponse(0)
    }

    private fun initStringOf(oppgaveSokResponse: OppgaveSokResponse?): String {
        if (oppgaveSokResponse != null) {
            return "OppgaveSokResponse(antallTreff=${oppgaveSokResponse.antallTreffTotalt},oppgaver=[${initStringOf(oppgaveSokResponse.oppgaver)}]"
        }

        return "no body, antall treff = 0"
    }

    private fun initStringOf(oppgaver: List<OppgaveData>): String {
        val oppgaveBuilder = StringBuilder("")
        oppgaver.forEach {
            oppgaveBuilder.append(
                "(id=${it.id},journalpostId=${it.journalpostId},tema=${it.tema},aktoerId=${it.aktoerId},oppgavetype=${it.oppgavetype})\n")
        }

        return oppgaveBuilder.toString()
    }

    override fun endreOppgave(patchOppgaveRequest: PatchOppgaveRequest) {
        val oppgaverPath = patchOppgaveRequest.leggOppgaveIdPa(OPPGAVE_CONTEXT)
        LOGGER.info("Endrer en oppgave med id $oppgaverPath: $patchOppgaveRequest")

        val responseEntity = restTemplate.exchange(
            oppgaverPath,
            HttpMethod.PATCH,
            patchOppgaveRequest.somHttpEntity(),
            String::class.java
        )

        LOGGER.info("Response: ${responseEntity.statusCode}, ${responseEntity.body}")
    }

    override fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): OppgaveDataForHendelse {
        LOGGER.info("Oppretter oppgave for journalpost ${opprettOppgaveRequest.journalpostId}")

        val responseEntity = restTemplate.exchange(
            OPPGAVE_CONTEXT,
            HttpMethod.POST,
            opprettOppgaveRequest.somHttpEntity(),
            OppgaveData::class.java
        )

        LOGGER.info("Response: {}, HttpStatus: {}", responseEntity.body, responseEntity.statusCode)
        return responseEntity.body?.somOppgaveForHendelse() ?: OppgaveDataForHendelse(id = -1, versjon = -1)
    }
}
