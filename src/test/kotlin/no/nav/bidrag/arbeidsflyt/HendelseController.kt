package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.commons.CorrelationId
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

// Only used when running locally for testing servives
@RestController
@Unprotected
@Profile("local")
class HendelseController(var behandleHendelseService: BehandleHendelseService) {

    @PostMapping("/journalpost")
    fun simulateJournalpostHendelse(@RequestBody journalpostHendelse: JournalpostHendelse){
        CorrelationId.existing("test fra bidrag")
        behandleHendelseService.behandleHendelse(journalpostHendelse)
    }
}