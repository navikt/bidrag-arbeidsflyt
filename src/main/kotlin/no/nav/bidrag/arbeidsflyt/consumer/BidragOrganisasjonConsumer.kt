package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.CacheConfig
import no.nav.bidrag.arbeidsflyt.model.GeografiskTilknytningResponse
import no.nav.bidrag.arbeidsflyt.model.HentGeografiskEnhetFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.HentGeografiskEnhetFeiletTekniskException
import no.nav.bidrag.arbeidsflyt.model.HentPersonFeiletFunksjoneltException
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
        private val LOGGER = LoggerFactory.getLogger(DefaultPersonConsumer::class.java)
    }
    @Cacheable(CacheConfig.GEOGRAFISK_ENHET_CACHE, unless = "#result==null")
    @Retryable(value = [HentGeografiskEnhetFeiletTekniskException::class], maxAttempts = 10, backoff = Backoff(delay = 1000, maxDelay = 10000, multiplier = 2.0))
    open fun hentGeografiskEnhet(personId: String): Optional<String> {
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
                LOGGER.error("Det skjedde en feil ved henting av geografisk enhet for person $personId", e)
                throw HentGeografiskEnhetFeiletFunksjoneltException("Det skjedde en feil ved henting av geografisk enhet for person $personId", e)
            }
            throw HentGeografiskEnhetFeiletTekniskException("Det skjedde en teknisk feil ved henting av geografisk enhet for person $personId", e)
        }

    }
}