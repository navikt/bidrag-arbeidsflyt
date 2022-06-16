package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.CacheConfig.Companion.PERSON_CACHE
import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.dto.HentPersonResponse
import no.nav.bidrag.arbeidsflyt.model.HentArbeidsfordelingFeiletTekniskException
import no.nav.bidrag.arbeidsflyt.model.HentPersonFeiletFunksjoneltException
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
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
    @Retryable(value = [HentArbeidsfordelingFeiletTekniskException::class], maxAttempts = 10, backoff = Backoff(delay = 2000, maxDelay = 30000, multiplier = 2.0))
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
                SECURE_LOGGER.warn("Fant ingen person for ident $ident")
                return Optional.empty()
            }

            return Optional.ofNullable(response.body)

        } catch (statusException: HttpStatusCodeException){
            if (statusException.statusCode.is4xxClientError){
                LOGGER.error("Det skjedde en feil ved henting av person $ident", statusException)
                SECURE_LOGGER.error("Det skjedde en feil ved henting av person $ident", statusException)
                throw HentPersonFeiletFunksjoneltException("Det skjedde en feil ved henting av person $ident", statusException)
            }
            throw HentArbeidsfordelingFeiletTekniskException("Det skjedde en teknisk feil ved henting av person $ident", statusException)
        }

    }


}
