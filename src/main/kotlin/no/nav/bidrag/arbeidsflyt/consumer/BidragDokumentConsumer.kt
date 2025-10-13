package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.model.HentJournalpostFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.HentJournalpostFeiletTekniskException
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.dokument.JournalpostResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BidragDokumentConsumer(
    @Value("\${BIDRAG_DOKUMENT_URL}") val url: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
    @Value("\${REST_MAX_RETRY:10}") val maxAttempts: Int,
) : AbstractRestClient(restTemplate, "bidrag-dokument") {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BidragDokumentConsumer::class.java)
    }

    private val baseUri get() =
        UriComponentsBuilder
            .fromUri(url)

    private val retryTemplate =
        RetryTemplate().apply {
            setRetryPolicy(SimpleRetryPolicy(maxAttempts, mapOf(HentJournalpostFeiletTekniskException::class.java to true)))
        }

    fun hentJournalpost(journalpostId: String): JournalpostResponse? {
        return try {
            retryTemplate.execute<JournalpostResponse?, HentJournalpostFeiletTekniskException> { context ->
                getForEntity<JournalpostResponse>(
                    baseUri
                        .pathSegment("journal")
                        .pathSegment(journalpostId)
                        .build()
                        .toUri(),
                )
            }
        } catch (e: HttpStatusCodeException) {
            if (HttpStatus.NOT_FOUND == e.statusCode) {
                // Should not happen in production. Logging error to be notified
                LOGGER.error("Fant ikke journalpost $journalpostId")
                return null
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
