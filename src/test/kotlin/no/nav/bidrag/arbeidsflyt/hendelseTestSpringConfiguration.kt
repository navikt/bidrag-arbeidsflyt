package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.HendelseConfiguration.Companion.hendelseFilterForLiveProfile
import no.nav.bidrag.arbeidsflyt.model.Hendelse
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.hendelse.PojoJournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.service.BehandleHendelseService
import no.nav.bidrag.arbeidsflyt.service.DefaultHendelseFilter
import no.nav.bidrag.arbeidsflyt.service.JsonMapperService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

const val PROFILE_NO_KAFKA = "no-kafka"

@Configuration
class NoKafkaConfiguration {
    @Bean
    fun journalpostHendelseListener(
        jsonMapperService: JsonMapperService,
        behandleHendelseService: BehandleHendelseService
    ): JournalpostHendelseListener = PojoJournalpostHendelseListener(
        jsonMapperService,
        behandleHendelseService
    )
}

@Configuration
class TestConfiguration {

    @Bean
    @Profile("!${PROFILE_NO_KAFKA}")
    fun hendelseFilterForEnhetstester() = DefaultHendelseFilter(
        listOf(
            Hendelse.AVVIK_ENDRE_FAGOMRADE,
            Hendelse.AVVIK_OVERFOR_TIL_ANNEN_ENHET,
            Hendelse.JOURNALFOR_JOURNALPOST,
            Hendelse.NO_SUPPORT
        )
    )

    @Bean
    @Profile(PROFILE_NO_KAFKA)
    fun hendelseFilter() = hendelseFilterForLiveProfile
}
