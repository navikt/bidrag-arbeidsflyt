package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

@Suppress("NonAsciiCharacters")
@ActiveProfiles("test")
internal class JournalpostHendelseTest {
    @Test
    fun `skal hente journalpostId fra prefikset journalpostId`() {
        val journalpostHendelse = JournalpostHendelse(journalpostId = "BID-101")

        assertThat(journalpostHendelse.journalpostIdUtenPrefix).isEqualTo("101")
    }
}
