package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.CacheConfig
import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.model.HentArbeidsfordelingFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.HentArbeidsfordelingFeiletTekniskException
import no.nav.bidrag.arbeidsflyt.model.HentJournalforendeEnheterFeiletFunksjoneltException
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.domain.ident.PersonIdent
import no.nav.bidrag.domain.string.Behandlingstema
import no.nav.bidrag.domain.string.Enhetsnummer
import no.nav.bidrag.domain.string.TEMA_BIDRAG
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.organisasjon.HentEnhetRequest
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.exchange

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
    open fun hentArbeidsfordeling(personId: PersonIdent, behandlingstema: Behandlingstema? = null): Enhetsnummer? {
        try {
            val response = restTemplate.exchange<EnhetDto>(
                "/arbeidsfordeling/enhet/geografisktilknytning",
                HttpMethod.POST,
                HttpEntity(
                    HentEnhetRequest(
                        ident = personId,
                        behandlingstema = behandlingstema,
                        tema = TEMA_BIDRAG
                    )
                )
            )

            if (response.statusCode == HttpStatus.NO_CONTENT) {
                return null
            }

            return response.body?.nummer
        } catch (e: HttpStatusCodeException) {
            val errorMessage =
                "Det skjedde en feil ved henting av arbeidsfordeling for person $personId og behandlingstema=$behandlingstema"
            LOGGER.error(errorMessage, e)
            if (e.statusCode == HttpStatus.BAD_REQUEST && behandlingstema != null) {
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
    open fun hentJournalforendeEnheter(): List<EnhetDto> {
        val response = restTemplate.exchange<List<EnhetDto>>(
            "/arbeidsfordeling/enhetsliste/journalforende",
            HttpMethod.GET,
            null
        )

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
    open fun hentEnhetInfo(enhet: Enhetsnummer): EnhetDto? {
        val response = restTemplate.exchange<EnhetDto>(
            "/enhet/info/$enhet",
            HttpMethod.GET,
            null
        )

        return if (response.statusCode == HttpStatus.NO_CONTENT) null else response.body
    }
}
