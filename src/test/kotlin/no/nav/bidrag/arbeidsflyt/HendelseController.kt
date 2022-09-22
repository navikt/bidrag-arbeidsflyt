package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import no.nav.bidrag.arbeidsflyt.persistence.repository.DLQKafkaRepository
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

// Only enabled when application is started locally
@RestController
@Unprotected
@Profile("local")
class HendelseController(var behandleHendelseService: BehandleHendelseService, var jsonMapperService: JsonMapperService, var dlqKafkaRepository: DLQKafkaRepository) {

    // Simulate kafka message on journalpost hendelse
    @PostMapping("/journalpost")
    fun simulateJournalpostHendelse(@RequestBody journalpostHendelse: JournalpostHendelse){
        CorrelationId.existing("test fra bidrag")
        behandleHendelseService.behandleHendelse(journalpostHendelse)
    }

    @PostMapping("/oppgave")
    fun simulateOppgaveHendelse(@RequestBody oppgave: String){
        CorrelationId.existing("test fra bidrag")
        var oppgave = jsonMapperService.mapOppgaveHendelse(oppgave);

    }

    @GetMapping("/")
    fun simulateOppgaveHendelse(): List<DLQKafka> {
        return dlqKafkaRepository.findAll()

    }

}