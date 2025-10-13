package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.dto.DefaultOpprettOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.dto.PatchOppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.EndreOppgaveFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.OpprettOppgaveFeiletFunksjoneltException
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private const val OPPGAVE_CONTEXT = "/api/v1/oppgaver/"

@Service
class OppgaveConsumer(
    @Value("\${OPPGAVE_URL}") val url: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "oppgave") {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(OppgaveConsumer::class.java)
    }

    private val baseUri get() =
        UriComponentsBuilder
            .fromUri(url)
            .path(OPPGAVE_CONTEXT)

    fun søkOppgaver(oppgaveSokRequest: OppgaveSokRequest): OppgaveSokResponse {
        val parameters = oppgaveSokRequest.tilMultiValueMap()

        LOGGER.info("søk opp åpne oppgaver på en journalpost: $parameters")

        val response =
            getForEntity<OppgaveSokResponse>(
                baseUri.queryParams(parameters).build().toUri(),
            )

        SECURE_LOGGER.info("Response søk oppgave - ${initStringOf(response)}")

        return response ?: OppgaveSokResponse(0)
    }

    fun hentOppgave(oppgaveId: Long): OppgaveData =
        try {
            getForNonNullEntity<OppgaveData>(
                baseUri.pathSegment(oppgaveId.toString()).build().toUri(),
            )
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                throw EndreOppgaveFeiletFunksjoneltException("Fant ikke oppgave med id $oppgaveId. Feilet med feilmelding ${e.message}", e)
            }

            throw e
        }

    private fun initStringOf(oppgaveSokResponse: OppgaveSokResponse?): String {
        if (oppgaveSokResponse != null) {
            return "OppgaveSokResponse(antallTreff=${oppgaveSokResponse.antallTreffTotalt},oppgaver=[${oppgaveSokResponse.oppgaver}]"
        }

        return "no body, antall treff = 0"
    }

    fun endreOppgave(
        patchOppgaveRequest: PatchOppgaveRequest,
        endretAvEnhetsnummer: String? = null,
    ) {
        patchOppgaveRequest.endretAvEnhetsnr = endretAvEnhetsnummer

        SECURE_LOGGER.info("Endrer oppgave ${patchOppgaveRequest.id} - $patchOppgaveRequest")

        try {
            val responseEntity =
                patchForEntity<OppgaveData>(
                    baseUri.pathSegment(patchOppgaveRequest.id.toString()).build().toUri(),
                    patchOppgaveRequest,
                )
            SECURE_LOGGER.info("Endret oppgave ${patchOppgaveRequest.id}, fikk respons $responseEntity")
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.BAD_REQUEST) {
                throw EndreOppgaveFeiletFunksjoneltException(
                    "Kunne ikke endre oppgave med id ${patchOppgaveRequest.id}. Feilet med feilmelding ${e.message}",
                    e,
                )
            }

            throw e
        }
    }

    fun opprettOppgave(opprettOppgaveRequest: DefaultOpprettOppgaveRequest): OppgaveData {
        try {
            SECURE_LOGGER.info("Oppretter oppgave med verdi $opprettOppgaveRequest")
            val responseEntity =
                postForNonNullEntity<OppgaveData>(
                    baseUri.build().toUri(),
                    opprettOppgaveRequest,
                )

            LOGGER.info(
                "Opprettet oppgave ${responseEntity.id} med type ${opprettOppgaveRequest.oppgavetype} og journalpostId ${opprettOppgaveRequest.journalpostId}",
            )
            return responseEntity
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.BAD_REQUEST) {
                throw OpprettOppgaveFeiletFunksjoneltException(
                    "Kunne ikke opprette oppgave for journalpost ${opprettOppgaveRequest.journalpostId}. Feilet med feilmelding ${e.message}",
                    e,
                )
            }

            throw e
        }
    }
}
