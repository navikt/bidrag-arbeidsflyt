package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.consumer.BidragOrganisasjonConsumer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ArbeidsfordelingService(private val organisasjonConsumer: BidragOrganisasjonConsumer) {

    companion object {
        private val DEFAULT_ENHET = "4833"
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(ArbeidsfordelingService::class.java)
    }

    fun hentArbeidsfordelingFailSafe(personId: String?): String {
        return try {
            hentArbeidsfordeling(personId)
        } catch (e: Exception){
            LOGGER.warn("Det skjedde en feil ved henting av arbeidsfordeling for person $personId", e)
            DEFAULT_ENHET
        }
    }

    fun hentArbeidsfordeling(personId: String?): String {
        if (personId.isNullOrEmpty()){
            SECURE_LOGGER.warn("Personid mangler, bruker enhet $DEFAULT_ENHET")
            return DEFAULT_ENHET
        }

        SECURE_LOGGER.info("Henter arbeidsfordeling for personId $personId")
        val geografiskEnhet = organisasjonConsumer.hentGeografiskEnhet(personId)
        if (geografiskEnhet.isEmpty){
            SECURE_LOGGER.warn("Fant ingen geografisk enhet for person $personId, bruker enhet $DEFAULT_ENHET")
            return DEFAULT_ENHET
        }

        return geografiskEnhet.get()
    }

}