package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.hendelse.PojoJournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

const val PROFILE_TEST = "test"

@Configuration
class TestConfiguration {
    @Bean
    @Profile("!$PROFILE_KAFKA_TEST")
    fun journalpostHendelseListener(
        jsonMapperService: JsonMapperService,
        behandleHendelseService: BehandleHendelseService
    ): JournalpostHendelseListener = PojoJournalpostHendelseListener(
        jsonMapperService,
        behandleHendelseService
    )
}
