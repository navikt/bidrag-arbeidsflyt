package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.CacheConfig.Companion.PERSON_CACHE
import no.nav.bidrag.arbeidsflyt.dto.HentPersonResponse
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpMethod

interface PersonConsumer {
    fun hentPerson(ident: String?): HentPersonResponse
}

open class DefaultPersonConsumer(private val restTemplate: HttpHeaderRestTemplate) : PersonConsumer {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(DefaultPersonConsumer::class.java)
    }
    @Cacheable(PERSON_CACHE, unless = "#ident==null")
    override fun hentPerson(ident: String?): HentPersonResponse {
        if (ident == null){
            return HentPersonResponse()
        }

        return try {
            restTemplate.exchange(
                "/informasjon/$ident",
                HttpMethod.GET,
                null,
                HentPersonResponse::class.java
            ).body ?: HentPersonResponse()
        } catch (e: Exception){
            LOGGER.error("Det skjedde en feil ved henting av person $ident fra bidrag-person", e)
            HentPersonResponse()
        }


    }


}
