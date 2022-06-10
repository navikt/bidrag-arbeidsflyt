package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.CacheConfig
import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.model.GeografiskTilknytningResponse
import no.nav.bidrag.arbeidsflyt.model.HentArbeidsfordelingFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.HentArbeidsfordelingFeiletTekniskException
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.apache.logging.log4j.util.Strings
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.web.client.HttpStatusCodeException
import java.util.Optional

open class BidragOrganisasjonConsumer(private val restTemplate: HttpHeaderRestTemplate) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BidragOrganisasjonConsumer::class.java)
    }
    @Cacheable(CacheConfig.GEOGRAFISK_ENHET_CACHE, unless = "#result==null")
    @Retryable(value = [HentArbeidsfordelingFeiletTekniskException::class], maxAttempts = 10, backoff = Backoff(delay = 2000, maxDelay = 30000, multiplier = 2.0))
    open fun hentArbeidsfordeling(personId: String): Optional<String> {
        if (Strings.isEmpty(personId)) {
            return Optional.empty()
        }

        try {
            val response = restTemplate.exchange("/arbeidsfordeling/enhetsliste/geografisktilknytning/$personId", HttpMethod.GET, null, GeografiskTilknytningResponse::class.java)

            if (response.statusCode == HttpStatus.NO_CONTENT){
                return Optional.empty()
            }

            return Optional.ofNullable(response.body?.enhetIdent)
        } catch (e: HttpStatusCodeException){
            if (e.statusCode.is4xxClientError){
                LOGGER.error("Det skjedde en feil ved henting av arbeidsfordeling for person $personId", e)
                SECURE_LOGGER.error("Det skjedde en feil ved henting av arbeidsfordeling for person $personId", e)
                throw HentArbeidsfordelingFeiletFunksjoneltException("Det skjedde en feil ved henting av arbeidsfordeling for person $personId", e)
            }
            throw HentArbeidsfordelingFeiletTekniskException("Det skjedde en teknisk feil ved henting av arbeidsfordeling for person $personId", e)
        }

    }
}