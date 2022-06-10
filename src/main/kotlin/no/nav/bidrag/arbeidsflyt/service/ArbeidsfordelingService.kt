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

    fun hentArbeidsfordeling(personId: String?): String {
        if (personId.isNullOrEmpty()){
            LOGGER.warn("hentArbeidsfordeling: Personid mangler, bruker enhet $DEFAULT_ENHET")
            return DEFAULT_ENHET
        }

        SECURE_LOGGER.info("Henter arbeidsfordeling for personId $personId")
        val arbeidsfordeling = organisasjonConsumer.hentArbeidsfordeling(personId)
        if (arbeidsfordeling.isEmpty){
            SECURE_LOGGER.warn("Fant ingen arbeidsfordeling for person $personId, bruker enhet $DEFAULT_ENHET")
            return DEFAULT_ENHET
        }

        val geografiskEnhet = arbeidsfordeling.get()
        SECURE_LOGGER.info("Hentet arbeidsfordeling $geografiskEnhet for person $personId")
        return geografiskEnhet
    }

}