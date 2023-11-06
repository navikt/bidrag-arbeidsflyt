package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.dto.OpprettOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.PatchOppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.EndreOppgaveFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.OpprettOppgaveFeiletFunksjoneltException
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpStatusCodeException

private const val OPPGAVE_CONTEXT = "/api/v1/oppgaver/"

interface OppgaveConsumer {
    fun finnOppgaverForJournalpost(oppgaveSokRequest: OppgaveSokRequest): OppgaveSokResponse

    fun hentOppgave(oppgaveId: Long): OppgaveData

    fun endreOppgave(
        patchOppgaveRequest: PatchOppgaveRequest,
        endretAvEnhetsnummer: String? = null,
    )

    fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): OppgaveData
}

class DefaultOppgaveConsumer(private val restTemplate: HttpHeaderRestTemplate) : OppgaveConsumer {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(DefaultOppgaveConsumer::class.java)
    }

    override fun finnOppgaverForJournalpost(oppgaveSokRequest: OppgaveSokRequest): OppgaveSokResponse {
        val parameters = oppgaveSokRequest.hentParametre()

        LOGGER.info("søk opp åpne oppgaver på en journalpost: $parameters")

        val oppgaveSokResponseEntity =
            restTemplate.exchange(
                "$OPPGAVE_CONTEXT$parameters",
                HttpMethod.GET,
                null,
                OppgaveSokResponse::class.java,
            )

        LOGGER.info("Response søk oppgave - ${initStringOf(oppgaveSokResponseEntity.body)}")

        return oppgaveSokResponseEntity.body ?: OppgaveSokResponse(0)
    }

    override fun hentOppgave(oppgaveId: Long): OppgaveData {
        return try {
            restTemplate.exchange(
                "$OPPGAVE_CONTEXT/$oppgaveId",
                HttpMethod.GET,
                null,
                OppgaveData::class.java,
            ).body!!
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                throw EndreOppgaveFeiletFunksjoneltException("Fant ikke oppgave med id $oppgaveId. Feilet med feilmelding ${e.message}", e)
            }

            throw e
        }
    }

    private fun initStringOf(oppgaveSokResponse: OppgaveSokResponse?): String {
        if (oppgaveSokResponse != null) {
            return "OppgaveSokResponse(antallTreff=${oppgaveSokResponse.antallTreffTotalt},oppgaver=[${oppgaveSokResponse.oppgaver}]"
        }

        return "no body, antall treff = 0"
    }

    override fun endreOppgave(
        patchOppgaveRequest: PatchOppgaveRequest,
        endretAvEnhetsnummer: String?,
    ) {
        patchOppgaveRequest.endretAvEnhetsnr = endretAvEnhetsnummer

        val oppgaverPath = patchOppgaveRequest.leggOppgaveIdPa(OPPGAVE_CONTEXT)
        LOGGER.info("Endrer oppgave ${patchOppgaveRequest.id} - $patchOppgaveRequest")

        try {
            val responseEntity =
                restTemplate.exchange(
                    oppgaverPath,
                    HttpMethod.PATCH,
                    patchOppgaveRequest.somHttpEntity(),
                    OppgaveData::class.java,
                )
            LOGGER.info("Endret oppgave ${patchOppgaveRequest.id}, fikk respons ${responseEntity.body}")
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

    override fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): OppgaveData {
        try {
            SECURE_LOGGER.info("Oppretter oppgave med verdi $opprettOppgaveRequest")
            val responseEntity =
                restTemplate.exchange(
                    OPPGAVE_CONTEXT,
                    HttpMethod.POST,
                    opprettOppgaveRequest.somHttpEntity(),
                    OppgaveData::class.java,
                )

            LOGGER.info(
                "Opprettet oppgave ${responseEntity.body?.id} med type ${opprettOppgaveRequest.oppgavetype} og journalpostId ${opprettOppgaveRequest.journalpostId}",
            )
            return responseEntity.body!!
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
