package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.consumer.BidragOrganisasjonConsumer
import no.nav.bidrag.domene.enums.diverse.Enhetsstatus
import no.nav.bidrag.domene.felles.erNullEllerUgyldig
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.transport.organisasjon.EnhetDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OrganisasjonService(
    private val organisasjonConsumer: BidragOrganisasjonConsumer,
) {
    companion object {
        private val DEFAULT_ENHET = Enhetsnummer("4833")

        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(OrganisasjonService::class.java)
    }

    fun hentArbeidsfordeling(
        personId: String?,
        behandlingstema: String? = null,
    ): Enhetsnummer {
        if (personId.isNullOrEmpty()) {
            LOGGER.warn("hentArbeidsfordeling: Personid mangler, bruker enhet $DEFAULT_ENHET")
            return DEFAULT_ENHET
        }

        val geografiskEnhet = organisasjonConsumer.hentArbeidsfordeling(Personident(personId), behandlingstema)
        if (geografiskEnhet.erNullEllerUgyldig()) {
            SECURE_LOGGER.warn(
                "Fant ingen arbeidsfordeling for person $personId og behandlingstema=$behandlingstema, bruker enhet $DEFAULT_ENHET",
            )
            return DEFAULT_ENHET
        }

        SECURE_LOGGER.info("Hentet arbeidsfordeling $geografiskEnhet for person $personId og behandlingstema=$behandlingstema")
        return geografiskEnhet
    }

    fun hentBidragJournalforendeEnheter(): List<EnhetDto> = organisasjonConsumer.hentJournalforendeEnheter()

    fun erJournalførendeEnhet(enhet: String?): Boolean = hentBidragJournalforendeEnheter().any { it.nummer.verdi == enhet }

    fun enhetEksistererOgErAktiv(enhet: String?): Boolean {
        if (enhet.isNullOrEmpty()) {
            return true
        }
        return try {
            val response = organisasjonConsumer.hentEnhetInfo(Enhetsnummer(enhet))
            !(response == null || response.status == Enhetsstatus.NEDLAGT)
        } catch (e: Exception) {
            LOGGER.warn("Hent enhetinfo feilet. Går videre med antagelse at enhet finnes og ikke er nedlagt.", e)
            true
        }
    }
}
