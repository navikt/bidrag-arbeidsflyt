package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NoKafkaConfiguration {
    @Bean
    fun journalpostHendelseListener(
        jsonMapperService: JsonMapperService,
        behandleHendelseService: BehandleHendelseService
    ): JournalpostHendelseListener = StubbedJournalpostHendelseListener(
        jsonMapperService,
        behandleHendelseService
    )

    private class StubbedJournalpostHendelseListener(
        private val jsonMapperService: JsonMapperService,
        private val behandleHendelseService: BehandleHendelseService
    ) : JournalpostHendelseListener {
        override fun lesHendelse(hendelse: String) {
            behandleHendelseService.behandleHendelse(jsonMapperService.mapHendelse(hendelse))
        }
    }
}
