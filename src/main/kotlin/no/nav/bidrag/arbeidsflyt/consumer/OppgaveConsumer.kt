package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.dto.OpprettJournalforingsOppgaveRequest
import no.nav.bidrag.arbeidsflyt.dto.PatchOppgaveRequest
import no.nav.bidrag.arbeidsflyt.model.EndreOppgaveFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.OppgaveDataForHendelse
import no.nav.bidrag.arbeidsflyt.model.OpprettOppgaveFeiletFunksjoneltException
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpStatusCodeException

private const val OPPGAVE_CONTEXT = "/api/v1/oppgaver/"

interface OppgaveConsumer {
    fun finnOppgaverForJournalpost(oppgaveSokRequest: OppgaveSokRequest): OppgaveSokResponse
    fun endreOppgave(patchOppgaveRequest: PatchOppgaveRequest, endretAvEnhetsnummer: String? = null)
    fun opprettOppgave(opprettJournalforingsOppgaveRequest: OpprettJournalforingsOppgaveRequest): OppgaveDataForHendelse
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
            return "OppgaveSokResponse(antallTreff=${oppgaveSokResponse.antallTreffTotalt},oppgaver=[${oppgaveSokResponse.oppgaver}]"
        }

        return "no body, antall treff = 0"
    }

    override fun endreOppgave(patchOppgaveRequest: PatchOppgaveRequest, endretAvEnhetsnummer: String?) {
        patchOppgaveRequest.endretAvEnhetsnr = endretAvEnhetsnummer

        val oppgaverPath = patchOppgaveRequest.leggOppgaveIdPa(OPPGAVE_CONTEXT)
        LOGGER.info("Endrer en oppgave med id $oppgaverPath: $patchOppgaveRequest")
        SECURE_LOGGER.info("Endrer en oppgave med id $oppgaverPath: $patchOppgaveRequest")

        try {
            val responseEntity = restTemplate.exchange(
                oppgaverPath,
                HttpMethod.PATCH,
                patchOppgaveRequest.somHttpEntity(),
                OppgaveData::class.java
            )
            LOGGER.info("Endret en oppgave: ${responseEntity.statusCode}, ${responseEntity.body}")
        } catch (e: HttpStatusCodeException){
            if (e.statusCode == HttpStatus.BAD_REQUEST){
                throw EndreOppgaveFeiletFunksjoneltException("Kunne ikke endre oppgave med id ${patchOppgaveRequest.id}. Feilet med feilmelding ${e.message}", e)
            }

            throw e
        }

    }

    override fun opprettOppgave(opprettJournalforingsOppgaveRequest: OpprettJournalforingsOppgaveRequest): OppgaveDataForHendelse {
        try {
            LOGGER.info("Oppretter ${opprettJournalforingsOppgaveRequest.oppgavetype} oppgave for journalpost ${opprettJournalforingsOppgaveRequest.journalpostId}")

            val responseEntity = restTemplate.exchange(
                OPPGAVE_CONTEXT,
                HttpMethod.POST,
                opprettJournalforingsOppgaveRequest.somHttpEntity(),
                OppgaveData::class.java
            )

            LOGGER.info("Opprettet ${opprettJournalforingsOppgaveRequest.oppgavetype} oppgave for journalpost {}, HttpStatus: {}", responseEntity.body?.id, responseEntity.statusCode)
            return responseEntity.body?.somOppgaveForHendelse() ?: OppgaveDataForHendelse(id = -1, versjon = -1)
        } catch (e: HttpStatusCodeException){
            if (e.statusCode == HttpStatus.BAD_REQUEST){
                throw OpprettOppgaveFeiletFunksjoneltException("Kunne ikke opprette oppgave for journalpost ${opprettJournalforingsOppgaveRequest.journalpostId}. Feilet med feilmelding ${e.message}", e)
            }

            throw e
        }

    }
}
