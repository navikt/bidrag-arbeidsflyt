package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.hendelse.PojoJournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import no.nav.bidrag.arbeidsflyt.service.PersistenceService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

const val PROFILE_TEST = "test"

@Configuration
class TestConfiguration {
    @Bean
    @Profile("!$PROFILE_KAFKA_TEST&!local")
    fun journalpostHendelseListener(
        jsonMapperService: JsonMapperService,
        behandleHendelseService: BehandleHendelseService,
        persistenceService: PersistenceService
    ): JournalpostHendelseListener = PojoJournalpostHendelseListener(
        jsonMapperService,
        persistenceService,
        behandleHendelseService
    )
}
