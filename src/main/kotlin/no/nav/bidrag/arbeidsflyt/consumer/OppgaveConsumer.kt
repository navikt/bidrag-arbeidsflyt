package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.dto.PatchOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate

private const val OPPGAVE_CONTEXT = "/api/v1/oppgaver/"
private const val PARAMETERS = "tema={fagomrade}&journalpostId={id}&statuskategori=AAPEN&sorteringsrekkefolge=ASC&sorteringsfelt=FRIST&limit=10"

interface OppgaveConsumer {
    fun finnOppgaverForJournalpost(oppgaveSokRequest: OppgaveSokRequest): OppgaveSokResponse
    fun endreOppgave(patchOppgaveRequest: PatchOppgaveRequest)
}

class DefaultOppgaveConsumer(private val restTemplate: HttpHeaderRestTemplate) : OppgaveConsumer {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(DefaultOppgaveConsumer::class.java)
    }

    override fun finnOppgaverForJournalpost(oppgaveSokRequest: OppgaveSokRequest): OppgaveSokResponse {
        val parameters = PARAMETERS
            .replace("{id}", oppgaveSokRequest.journalpostId)
            .replace("{fagomrade}", oppgaveSokRequest.fagomrade)

        LOGGER.info("søk opp åpne oppgaver på en journalpost: $parameters")

        val oppgaveSokResponse = restTemplate.exchange(
            "$OPPGAVE_CONTEXT?$parameters",
            HttpMethod.GET,
            null,
            OppgaveSokResponse::class.java
        )

        LOGGER.info("Response: ${oppgaveSokResponse.statusCode}/${oppgaveSokResponse.body}")

        return oppgaveSokResponse.body ?: OppgaveSokResponse(0)
    }

    override fun endreOppgave(patchOppgaveRequest: PatchOppgaveRequest) {
        val oppgaverPath = patchOppgaveRequest.leggOppgaveIdPa(OPPGAVE_CONTEXT)
        LOGGER.info("Endrer en oppgave med id: $oppgaverPath")
        val responseEntity = restTemplate.exchange(
            oppgaverPath,
            HttpMethod.PATCH,
            patchOppgaveRequest.somHttpEntity(),
            String::class.java
        )

        LOGGER.info("Response: {}, HttpStatus: {}", responseEntity.body, responseEntity.statusCode)
    }
}
