package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.consumer.PersonConsumer
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.repository.JournalpostRepository
import no.nav.bidrag.arbeidsflyt.utils.FeatureToggle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Optional
import javax.transaction.Transactional

@Service
class PersistenceService(
    private val journalpostRepository: JournalpostRepository,
    private val featureToggle: FeatureToggle,
    private val personConsumer: PersonConsumer
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(PersistenceService::class.java)
    }

    fun hentJournalpostMedStatusMottatt(journalpostId: String): Optional<Journalpost> {
        val harJournalpostIdPrefiks = journalpostId.contains("-")
        val journalpostIdUtenPrefiks = if (harJournalpostIdPrefiks) journalpostId.split('-')[1] else journalpostId
        return journalpostRepository.findByJournalpostIdContaining(journalpostIdUtenPrefiks)
            .filter{ it.erStatusMottatt && it.erBidragFagomrade }
    }

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    fun lagreEllerOppdaterJournalpostFraHendelse(journalpostHendelse: JournalpostHendelse){
        if (!featureToggle.isFeatureEnabled(FeatureToggle.Feature.LAGRE_JOURNALPOST)){
            return
        }

        val journalpostId = if (journalpostHendelse.harJournalpostIdJOARKPrefix()) journalpostHendelse.journalpostIdUtenPrefix else journalpostHendelse.journalpostId


        if (!journalpostHendelse.erMottaksregistrert || journalpostHendelse.erEksterntFagomrade){
            LOGGER.info("Sletter journalpost $journalpostId fordi status ikke lenger er MOTTATT eller er endret til ekstern fagområde (status=${journalpostHendelse.journalstatus}, fagomrade=${journalpostHendelse.fagomrade})")
            deleteJournalpost(journalpostId)
        } else {
            LOGGER.info("Lagrer journalpost $journalpostId fra hendelse")
            saveOrUpdateMottattJournalpost(journalpostId, journalpostHendelse)
        }
    }

    fun saveOrUpdateMottattJournalpost(journalpostId: String, journalpostHendelse: JournalpostHendelse){
        val gjelderId = personConsumer.hentPerson(journalpostHendelse.aktorId)
        journalpostRepository.findByJournalpostId(journalpostId)
            .ifPresentOrElse({
                it.status = journalpostHendelse.journalstatus ?: it.status
                it.enhet = journalpostHendelse.enhet ?: it.enhet
                it.tema = journalpostHendelse.fagomrade ?: it.tema
                it.gjelderId = gjelderId?.ident ?: it.gjelderId
                journalpostRepository.save(it)
            }, {
                journalpostRepository.save(
                    Journalpost(
                        journalpostId = journalpostId,
                        status = journalpostHendelse.journalstatus ?: "UKJENT",
                        tema = journalpostHendelse.fagomrade ?: "BID",
                        enhet = journalpostHendelse.enhet ?: "UKJENT",
                        gjelderId = gjelderId?.ident ?: journalpostHendelse.aktorId
                    )
                )
            })
    }

    fun deleteJournalpost(journalpostId: String){
        journalpostRepository.deleteByJournalpostId(journalpostId)
    }

}