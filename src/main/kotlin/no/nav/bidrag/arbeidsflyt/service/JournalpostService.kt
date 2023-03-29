package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.BidragDokumentConsumer
import no.nav.bidrag.arbeidsflyt.model.Fagomrade
import no.nav.bidrag.dokument.dto.JournalpostResponse
import no.nav.bidrag.dokument.dto.Journalstatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

internal val JournalpostResponse.erBidragFagomrade get(): Boolean = journalpost?.fagomrade == Fagomrade.BIDRAG || journalpost?.fagomrade == Fagomrade.FARSKAP

@Service
class JournalpostService(private val bidragDokumentConsumer: BidragDokumentConsumer) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(JournalpostService::class.java)
    }

    fun hentJournalpostMedStatusMottatt(journalpostId: String): JournalpostResponse? {
        val journalpost = bidragDokumentConsumer.hentJournalpost(journalpostId)
        LOGGER.info("Hentet journalpost $journalpostId fra bidrag-dokument")
        return journalpost.takeIf { it?.journalpost?.journalstatus == Journalstatus.MOTTATT }
    }
}
