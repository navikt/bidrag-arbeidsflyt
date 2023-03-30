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

    fun hentArbeidsfordeling(personId: String?, behandlingstema: String? = null): String {
        if (personId.isNullOrEmpty()) {
            LOGGER.warn("hentArbeidsfordeling: Personid mangler, bruker enhet $DEFAULT_ENHET")
            return DEFAULT_ENHET
        }

        val geografiskEnhet = organisasjonConsumer.hentArbeidsfordeling(personId, behandlingstema)
        if (geografiskEnhet.isNullOrEmpty()) {
            SECURE_LOGGER.warn("Fant ingen arbeidsfordeling for person $personId og behandlingstema=$behandlingstema, bruker enhet $DEFAULT_ENHET")
            return DEFAULT_ENHET
        }

        SECURE_LOGGER.info("Hentet arbeidsfordeling $geografiskEnhet for person $personId og behandlingstema=$behandlingstema")
        return geografiskEnhet
    }

    fun hentBidragJournalforendeEnheter(): List<EnhetResponse> {
        return organisasjonConsumer.hentJournalforendeEnheter()
    }

    fun erJournalførendeEnhet(enhet: String?): Boolean {
        return hentBidragJournalforendeEnheter().any { it.enhetIdent == enhet }
    }

    fun enhetEksistererOgErAktiv(enhet: String?): Boolean {
        if (enhet.isNullOrEmpty()) {
            return true
        }
        return try {
            val response = organisasjonConsumer.hentEnhetInfo(enhet)
            !(response == null || response.erNedlagt())
        } catch (e: Exception) {
            LOGGER.warn("Hent enhetinfo feilet. Går videre med antagelse at enhet finnes og ikke er nedlagt.", e)
            true
        }
    }
}
