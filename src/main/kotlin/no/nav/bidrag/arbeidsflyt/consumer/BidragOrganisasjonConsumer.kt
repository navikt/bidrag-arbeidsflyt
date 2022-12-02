package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.CacheConfig
import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.model.*
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.apache.logging.log4j.util.Strings
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.web.client.HttpStatusCodeException
import java.util.*


open class BidragOrganisasjonConsumer(private val restTemplate: HttpHeaderRestTemplate) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BidragOrganisasjonConsumer::class.java)
    }

    @Cacheable(CacheConfig.GEOGRAFISK_ENHET_CACHE, unless = "#result==null")
    @Retryable(
        value = [HentArbeidsfordelingFeiletTekniskException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 2000, maxDelay = 10000, multiplier = 2.0)
    )
    open fun hentArbeidsfordeling(personId: String, behandlingstema: String? = null): String? {
        if (personId.isEmpty()) {
            return null
        }

        try {
            val response = restTemplate.exchange(
                "/arbeidsfordeling/enhetsliste/geografisktilknytning/$personId${behandlingstema?.let { "?behandlingstema=$it" }?:""}",
                HttpMethod.GET,
                null,
                GeografiskTilknytningResponse::class.java
            )

            if (response.statusCode == HttpStatus.NO_CONTENT) {
                return null
            }

            return response.body?.enhetIdent
        } catch (e: HttpStatusCodeException) {
            val errorMessage = "Det skjedde en feil ved henting av arbeidsfordeling for person $personId og behandlingstema=$behandlingstema"
            LOGGER.error(errorMessage, e)
            if (e.statusCode == HttpStatus.BAD_REQUEST && !behandlingstema.isNullOrEmpty()){
                LOGGER.warn("Kunne ikke hente arbeidsfordeling med behandlingstema=$behandlingstema. Forsøker å hente arbeidsfordeling med bare personId")
                return hentArbeidsfordeling(personId)
            }
            if (e.statusCode.is4xxClientError) {
                SECURE_LOGGER.error(errorMessage, e)
                throw HentArbeidsfordelingFeiletFunksjoneltException(errorMessage, e)
            }
            throw HentArbeidsfordelingFeiletTekniskException(errorMessage, e)
        }

    }

    @Cacheable(CacheConfig.JOURNALFORENDE_ENHET_CACHE, unless = "#result==null")
    @Retryable(
        value = [Exception::class],
        maxAttempts = 10,
        backoff = Backoff(delay = 2000, maxDelay = 30000, multiplier = 2.0)
    )
    open fun hentJournalforendeEnheter(): List<EnhetResponse> {
        val response = restTemplate.exchange(
            "/arbeidsfordeling/enhetsliste/journalforende",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<List<EnhetResponse>>() {})

        if (response.statusCode == HttpStatus.NO_CONTENT) {
            throw HentJournalforendeEnheterFeiletFunksjoneltException("Fant ingen journalforende enheter")
        }

        return response.body ?: emptyList()
    }

    @Cacheable(CacheConfig.ENHET_INFO_CACHE, unless = "#result==null")
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 2000, maxDelay = 30000, multiplier = 2.0)
    )
    open fun hentEnhetInfo(enhet: String): Optional<EnhetResponse> {
        val response = restTemplate.exchange(
            "/enhet/info/$enhet",
            HttpMethod.GET,
            null,
            EnhetResponse::class.java)

        if (response.statusCode == HttpStatus.NO_CONTENT) {
            return Optional.empty()
        }

        return Optional.ofNullable(response.body)
    }
}