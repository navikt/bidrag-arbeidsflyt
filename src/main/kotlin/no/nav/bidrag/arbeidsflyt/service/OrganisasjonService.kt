package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.consumer.BidragOrganisasjonConsumer
import no.nav.bidrag.arbeidsflyt.model.EnhetResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OrganisasjonService(private val organisasjonConsumer: BidragOrganisasjonConsumer) {

    companion object {
        private val DEFAULT_ENHET = "4833"
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(OrganisasjonService::class.java)
    }

    fun hentArbeidsfordeling(personId: String?): String {
        if (personId.isNullOrEmpty()){
            LOGGER.warn("hentArbeidsfordeling: Personid mangler, bruker enhet $DEFAULT_ENHET")
            return DEFAULT_ENHET
        }

        val arbeidsfordeling = organisasjonConsumer.hentArbeidsfordeling(personId)
        if (arbeidsfordeling.isEmpty){
            SECURE_LOGGER.warn("Fant ingen arbeidsfordeling for person $personId, bruker enhet $DEFAULT_ENHET")
            return DEFAULT_ENHET
        }

        val geografiskEnhet = arbeidsfordeling.get()
        SECURE_LOGGER.info("Hentet arbeidsfordeling $geografiskEnhet for person $personId")
        return geografiskEnhet
    }

    fun hentBidragJournalforendeEnheter(): List<EnhetResponse> {
        return organisasjonConsumer.hentJournalforendeEnheter()
    }

    fun enhetEksistererOgErAktiv(enhet: String?): Boolean {
        if (enhet.isNullOrEmpty()){
            return true
        }
        return try {
            val enhetResponse =  organisasjonConsumer.hentEnhetInfo(enhet)
            !(enhetResponse.isEmpty || enhetResponse.filter{ it.erNedlagt() }.isPresent)
        } catch (e: Exception){
            LOGGER.warn("Hent enhetinfo feilet. Går videre med antagelse at enhet finnes og ikke er nedlagt.", e)
            true
        }
    }

}