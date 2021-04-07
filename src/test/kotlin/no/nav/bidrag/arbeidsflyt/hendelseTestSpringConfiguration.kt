package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val PROFILE_TEST = "test"

@Configuration
class NoKafkaConfiguration {
    @Bean
    fun journalpostHendelseListener(jsonMapperService: JsonMapperService): JournalpostHendelseListener = StubbedJournalpostHendelseListener(
        jsonMapperService
    )

    private class StubbedJournalpostHendelseListener(private val jsonMapperService: JsonMapperService) : JournalpostHendelseListener {
        override fun lesHendelse(hendelse: String) {
            jsonMapperService.lesHendelse(hendelse)
        }
    }
}
