package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.BidragDokumentConsumer
import no.nav.bidrag.arbeidsflyt.model.Journalstatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JournalpostService(private val bidragDokumentConsumer: BidragDokumentConsumer) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(JournalpostService::class.java)
    }

    fun erJournalpostStatusMottatt(journalpostId: String): Boolean {
        val journalpost = bidragDokumentConsumer.hentJournalpost(journalpostId)
        LOGGER.info("Hentet journalpost $journalpostId")
        return journalpost.map { it.journalpost?.journalstatus == Journalstatus.MOTTATT }.orElse(false)
    }

}