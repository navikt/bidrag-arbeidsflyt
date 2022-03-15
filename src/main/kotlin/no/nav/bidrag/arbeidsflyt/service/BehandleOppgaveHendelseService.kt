package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.arbeidsflyt.persistence.repository.JournalpostRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.OppgaveRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class BehandleOppgaveHendelseService(
    var oppgaveRepository: OppgaveRepository,
    var journalpostRepository: JournalpostRepository,
    var oppgaveService: OppgaveService
) {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(BehandleOppgaveHendelseService::class.java)
    }


    @Transactional
    fun behandleOpprettOppgave(oppgaveHendelse: OppgaveHendelse){
        val oppgave = Oppgave(
            oppgaveId = oppgaveHendelse.id!!,
            oppgavetype =  oppgaveHendelse.oppgavetype!!,
            status = oppgaveHendelse.status?.name!!,
            journalpostId = oppgaveHendelse.journalpostId!!,
            tema = oppgaveHendelse.tema!!
        )
        oppgaveRepository.save(oppgave)
        LOGGER.info("Lagret opprettet oppgave ${oppgaveHendelse.id} i databasen.")
    }

    @Transactional
    fun behandleEndretOppgave(oppgaveHendelse: OppgaveHendelse){

        if (oppgaveHendelse.erJournalforingOppgave()){
            opprettNyOppgaveHvisFerdigstiltOgJournalpostErMottatt(oppgaveHendelse)
        } else {
            opprettNyOppgaveHvisOppgavetypeEndretFraJournalforingTilNoeAnnet(oppgaveHendelse)
        }

        oppdaterOppgaveFraHendelse(oppgaveHendelse)
    }

    fun oppdaterOppgaveFraHendelse(oppgaveHendelse: OppgaveHendelse){
        oppgaveRepository.findById(oppgaveHendelse.id!!).ifPresent {
            it.oppdaterOppgaveFraHendelse(oppgaveHendelse)
            oppgaveRepository.save(it)
         }
    }

    fun opprettNyOppgaveHvisFerdigstiltOgJournalpostErMottatt(oppgaveHendelse: OppgaveHendelse){
        if (oppgaveHendelse.journalpostId == null){
            LOGGER.warn("Oppgave ${oppgaveHendelse.id} har ingen journalpostid. Stopper videre behandling.")
            return
        }

        val journalpost = journalpostRepository.findByJournalpostIdContaining(oppgaveHendelse.journalpostId!!)
        if (!journalpost.isPresent){
            LOGGER.warn("Fant ingen tilhørende journalpost for oppgave ${oppgaveHendelse.id} med journalpostid ${oppgaveHendelse.journalpostId}. Stopper videre behandling.")
            return
        }

        if (oppgaveHendelse.erStatusFerdigstilt() && journalpost.get().erStatusMottatt()){
            LOGGER.info("Oppgave ${oppgaveHendelse.id} ble lukket når tilhørende journalpost ${oppgaveHendelse.journalpostId} fortsatt har status MOTTATT. Oppretter ny oppgave")
            oppgaveService.opprettJournalforingOppgave(oppgaveHendelse)
        }
    }

    fun opprettNyOppgaveHvisOppgavetypeEndretFraJournalforingTilNoeAnnet(oppgaveHendelse: OppgaveHendelse){
        val existingOppgave = oppgaveRepository.findById(oppgaveHendelse.id!!)
        if (!existingOppgave.isPresent){
            LOGGER.info("Fant ingen oppgave med id ${oppgaveHendelse.id} i databasen. Lagrer opppgave og stopper videre behandling")
            behandleOpprettOppgave(oppgaveHendelse)
            return
        }
        if (oppgaveHendelse.hasJournalpostId() && !oppgaveHendelse.erJournalforingOppgave() && existingOppgave.get().erJournalforingOppgave()){
            LOGGER.info("Oppgavetype på oppgave ${oppgaveHendelse.id} ble endret fra Journalføring (JFR) til ${oppgaveHendelse.oppgavetype}. Oppretter ny oppgave med type JFR.")
            oppgaveService.opprettJournalforingOppgave(oppgaveHendelse)
            return
        }
    }
}