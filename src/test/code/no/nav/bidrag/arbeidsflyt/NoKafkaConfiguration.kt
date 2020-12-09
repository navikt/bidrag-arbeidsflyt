package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.service.HendelseService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NoKafkaConfiguration {
    @Bean
    fun journalpostHendelseListener(hendelseService: HendelseService): JournalpostHendelseListener = StubbedJournalpostHendelseListener(
        hendelseService
    )

    private class StubbedJournalpostHendelseListener(private val hendelseService: HendelseService) : JournalpostHendelseListener {
        override fun lesHendelse(hendelse: String) {
            hendelseService.lesHendelse(hendelse)
        }
    }
}

