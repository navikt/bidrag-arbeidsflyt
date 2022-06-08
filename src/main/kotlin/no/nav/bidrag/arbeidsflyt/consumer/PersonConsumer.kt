package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.CacheConfig.Companion.PERSON_CACHE
import no.nav.bidrag.arbeidsflyt.dto.HentPersonResponse
import no.nav.bidrag.arbeidsflyt.model.HentGeografiskEnhetFeiletTekniskException
import no.nav.bidrag.arbeidsflyt.model.HentPersonFeiletFunksjoneltException
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.web.client.HttpStatusCodeException
import java.util.Optional

interface PersonConsumer {
    fun hentPerson(ident: String?): Optional<HentPersonResponse>
}

open class DefaultPersonConsumer(private val restTemplate: HttpHeaderRestTemplate) : PersonConsumer {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(DefaultPersonConsumer::class.java)
    }
    @Cacheable(PERSON_CACHE, unless = "#ident==null||#result==null")
    @Retryable(value = [HentGeografiskEnhetFeiletTekniskException::class], maxAttempts = 10, backoff = Backoff(delay = 1000, maxDelay = 10000, multiplier = 2.0))
    override fun hentPerson(ident: String?): Optional<HentPersonResponse> {
        if (ident == null){
            return Optional.empty()
        }

        try {
            val response =  restTemplate.exchange(
                "/informasjon/$ident",
                HttpMethod.GET,
                null,
                HentPersonResponse::class.java
            )

            if (response.statusCode == HttpStatus.NO_CONTENT){
                return Optional.empty()
            }

            return Optional.ofNullable(response.body)

        } catch (statusException: HttpStatusCodeException){
            if (statusException.statusCode.is4xxClientError){
                LOGGER.error("Det skjedde en feil ved henting av person $ident", statusException)
                throw HentPersonFeiletFunksjoneltException("Det skjedde en feil ved henting av person $ident", statusException)
            }
            throw HentGeografiskEnhetFeiletTekniskException("Det skjedde en teknisk feil ved henting av person $ident", statusException)
        }

    }


}
