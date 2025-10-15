package no.nav.bidrag.arbeidsflyt.consumer

import no.nav.bidrag.arbeidsflyt.CacheConfig
import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.model.HentArbeidsfordelingFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.HentArbeidsfordelingFeiletTekniskException
import no.nav.bidrag.arbeidsflyt.model.HentJournalforendeEnheterFeiletFunksjoneltException
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.enums.diverse.Tema
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.organisasjon.HentEnhetRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BidragOrganisasjonConsumer(
    @Value("\${BIDRAG_ORGANISASJON_URL}") val url: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
    @Value("\${retry.enabled:true}") val shouldRetry: Boolean,
) : AbstractRestClient(restTemplate, "bidrag-organisasjon") {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BidragOrganisasjonConsumer::class.java)
    }

    private val baseUri get() =
        UriComponentsBuilder
            .fromUri(url)

    @Cacheable(CacheConfig.GEOGRAFISK_ENHET_CACHE, unless = "#result==null")
    @Retryable(
        exceptionExpression = "@bidragOrganisasjonConsumer.shouldRetry",
        value = [HentArbeidsfordelingFeiletTekniskException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 2000, maxDelay = 10000, multiplier = 2.0),
    )
    open fun hentArbeidsfordeling(
        personId: Personident,
        behandlingstema: String? = null,
    ): Enhetsnummer? {
        try {
            val response =
                postForEntity<EnhetDto>(
                    baseUri.pathSegment("arbeidsfordeling", "enhet", "geografisktilknytning").build().toUri(),
                    HentEnhetRequest(
                        ident = personId,
                        behandlingstema = behandlingstema,
                        tema = Tema.TEMA_BIDRAG.verdi,
                    ),
                )

            return response?.nummer
        } catch (e: HttpStatusCodeException) {
            val errorMessage =
                "Det skjedde en feil ved henting av arbeidsfordeling for person ${personId.verdi} og behandlingstema=$behandlingstema"
            secureLogger.error(e) { errorMessage }
            if (e.statusCode == HttpStatus.BAD_REQUEST && behandlingstema != null) {
                LOGGER.warn(
                    "Kunne ikke hente arbeidsfordeling med behandlingstema=$behandlingstema. Forsøker å hente arbeidsfordeling med bare personId",
                )
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
        exceptionExpression = "@bidragOrganisasjonConsumer.shouldRetry",
        value = [Exception::class],
        maxAttempts = 10,
        backoff = Backoff(delay = 2000, maxDelay = 30000, multiplier = 2.0),
    )
    open fun hentJournalforendeEnheter(): List<EnhetDto> {
        val response =
            getForEntity<List<EnhetDto>>(
                baseUri.pathSegment("arbeidsfordeling", "enhetsliste", "journalforende").build().toUri(),
            )

        if (response == null || response.isEmpty()) {
            throw HentJournalforendeEnheterFeiletFunksjoneltException("Fant ingen journalforende enheter")
        }

        return response
    }

    @Cacheable(CacheConfig.ENHET_INFO_CACHE, unless = "#result==null")
    @Retryable(
        exceptionExpression = "@bidragOrganisasjonConsumer.shouldRetry",
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 2000, maxDelay = 30000, multiplier = 2.0),
    )
    open fun hentEnhetInfo(enhet: Enhetsnummer): EnhetDto? {
        val response =
            getForEntity<EnhetDto>(
                baseUri.pathSegment("enhet", "info", enhet.verdi).build().toUri(),
            )

        return response
    }
}
