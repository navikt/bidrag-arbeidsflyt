package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.model.HentJournalpostFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.HentJournalpostFeiletTekniskException
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.dokument.dto.JournalpostResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.web.client.HttpStatusCodeException
import java.util.Optional


open class BidragDokumentConsumer(private val restTemplate: HttpHeaderRestTemplate) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BidragDokumentConsumer::class.java)
    }

    @Retryable(
        value = [HentJournalpostFeiletTekniskException::class],
        maxAttempts = 10,
        backoff = Backoff(delay = 2000, maxDelay = 30000, multiplier = 2.0)
    )
    open fun hentJournalpost(journalpostId: String): Optional<JournalpostResponse> {

        try {
            val response = restTemplate.exchange(
                "/journal/$journalpostId",
                HttpMethod.GET,
                null,
                JournalpostResponse::class.java
            )
            if (response.statusCode == HttpStatus.NO_CONTENT) {
                return Optional.empty()
            }

            return Optional.ofNullable(response.body)
        } catch (e: HttpStatusCodeException) {
            if (HttpStatus.NOT_FOUND == e.statusCode){
                // Should not happen in production. Logging error to be notified
                LOGGER.error("Fant ikke journalpost $journalpostId")
                return Optional.empty()
            }

            val errorMessage = "Det skjedde en feil ved henting av journalpost $journalpostId"
            if (e.statusCode.is4xxClientError) {
                LOGGER.error(errorMessage, e)
                throw HentJournalpostFeiletFunksjoneltException(errorMessage, e)
            }
            throw HentJournalpostFeiletTekniskException(errorMessage, e)
        }

    }
}