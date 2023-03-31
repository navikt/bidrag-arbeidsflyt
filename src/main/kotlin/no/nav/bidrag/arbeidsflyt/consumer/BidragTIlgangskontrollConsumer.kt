package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.CacheConfig.Companion.TILGANG_TEMA_CACHE
import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BidragTIlgangskontrollConsumer(
    @Value("\${BIDRAG_TILGANGSKONTROLL_URL}") val url: URI,
    @Qualifier("azure") private val restTemplate: RestOperations
) : AbstractRestClient(restTemplate, "bidrag-tilgangskontroll") {

    private fun createUri(path: String?) = UriComponentsBuilder.fromUri(url)
        .path(path ?: "").build().toUri()

    @Retryable(value = [Exception::class], maxAttempts = 3, backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0))
    @Cacheable(TILGANG_TEMA_CACHE)
    fun sjekkTilgangTema(tema: String, saksbehandlerIdent: String): Boolean {
        return try {
            postForEntity(createUri("/api/tilgang/tema?navIdent=$saksbehandlerIdent"), tema) ?: false
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.FORBIDDEN) return false
            throw e
        }
    }
}