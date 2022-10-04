package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.BidragDokumentConsumer
import no.nav.bidrag.arbeidsflyt.model.Fagomrade
import no.nav.bidrag.arbeidsflyt.model.Journalstatus
import no.nav.bidrag.dokument.dto.JournalpostResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Optional

internal val JournalpostResponse.erBidragFagomrade get(): Boolean = journalpost?.fagomrade == Fagomrade.BIDRAG || journalpost?.fagomrade == Fagomrade.FARSKAP
@Service
class JournalpostService(private val bidragDokumentConsumer: BidragDokumentConsumer) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(JournalpostService::class.java)
    }

    fun hentJournalpostMedStatusMottatt(journalpostId: String): Optional<JournalpostResponse> {
        val journalpost = bidragDokumentConsumer.hentJournalpost(journalpostId)
        LOGGER.info("Hentet journalpost $journalpostId fra bidrag-dokument")
        return journalpost.filter{ it.erBidragFagomrade }.filter { it.journalpost?.journalstatus == Journalstatus.MOTTATT }
    }

}