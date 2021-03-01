package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.consumer.DefaultOppgaveConsumer
import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelseListener
import no.nav.bidrag.arbeidsflyt.service.HendelseService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

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

@Configuration
class BidragArbeidsflytTestBeanConfiguration {

    @Bean
    fun stubbedOppgaveConsumer(): OppgaveConsumer {
        return DefaultOppgaveConsumer(RestTemplate())
    }
}
