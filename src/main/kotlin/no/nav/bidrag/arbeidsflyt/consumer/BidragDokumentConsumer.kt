package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.dokument.dto.JournalpostDto
import no.nav.bidrag.dokument.dto.JournalpostResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

private val LOGGER = LoggerFactory.getLogger(BidragDokumentConsumer::class.java)

class DefaultBidragDokumentConsumer(private val restTemplate: RestTemplate) : BidragDokumentConsumer {
    override fun hentJournalpost(journalpostId: String): JournalpostDto? {
        val path = "/journal/$journalpostId"
        LOGGER.info("GET: $path")

        val exchange: ResponseEntity<JournalpostResponse> = restTemplate.exchange(
            path, HttpMethod.GET, null, JournalpostResponse::class.java
        )

        return exchange.body?.journalpost
    }
}

interface BidragDokumentConsumer {
    fun hentJournalpost(journalpostId: String): JournalpostDto?
}
