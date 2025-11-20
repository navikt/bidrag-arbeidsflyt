package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.CacheConfig
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class BidragBBMConsumer(
    @Value("\${BIDRAG_BBM_URL}") private val bidragBBMurl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-bbm") {
    private val bidragBBMUri
        get() = UriComponentsBuilder.fromUri(bidragBBMurl).pathSegment("api", "beregning")

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    @Cacheable(CacheConfig.BBM_SØKNAD_CACHE)
    fun hentSøknad(request: HentSøknadRequest): HentSøknadResponse =
        postForNonNullEntity(
            bidragBBMUri.pathSegment("hentsoknad").build().toUri(),
            request,
        )
}
