package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate

private const val PARAMETERS = "?tema={fagomrade}&journalpostId={id}&statuskategori=AAPEN&sorteringsrekkefolge=ASC&sorteringsfelt=FRIST&limit=10"
private val LOGGER = LoggerFactory.getLogger(DefaultOppgaveConsumer::class.java)

interface OppgaveConsumer {
    fun finnOppgaverForJournalpost(oppgaveSokRequest: OppgaveSokRequest): OppgaveSokResponse?
    fun ferdigstillOppgaver(ferdigstillOppgaveRequest: FerdigstillOppgaveRequest)
}

class DefaultOppgaveConsumer(private val restTemplate: RestTemplate) : OppgaveConsumer {

    override fun finnOppgaverForJournalpost(oppgaveSokRequest: OppgaveSokRequest): OppgaveSokResponse? {
        val parametre = PARAMETERS
            .replace("{id}", oppgaveSokRequest.journalpostId)
            .replace("{fagomrade}", oppgaveSokRequest.fagomrade)

        LOGGER.info("søk opp åpne oppgaver på en journalpost: $parametre")

        val oppgaveSokResponse = restTemplate.exchange(
            parametre,
            HttpMethod.GET,
            null,
            OppgaveSokResponse::class.java
        )

        LOGGER.info("Response: ${oppgaveSokResponse.statusCode}/${oppgaveSokResponse.body}")

        return oppgaveSokResponse.body
    }

    override fun ferdigstillOppgaver(ferdigstillOppgaveRequest: FerdigstillOppgaveRequest) {
        LOGGER.info("Ferdigstiller en oppgave med id: ${ferdigstillOppgaveRequest.hentOppgaveDataIdSomContextPath()}")

        val responseEntity = restTemplate.exchange(
            ferdigstillOppgaveRequest.hentOppgaveDataIdSomContextPath(),
            HttpMethod.PUT,
            ferdigstillOppgaveRequest.somHttpEntity(),
            String::class.java
        )

        LOGGER.info("Response: {}, HttpStatus: {}", responseEntity.body, responseEntity.statusCode)
    }
}
