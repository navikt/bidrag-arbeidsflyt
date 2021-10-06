package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@Suppress("NonAsciiCharacters")
internal class JournalpostHendelseTest {
    @Test
    fun `skal hente journalpostId fra prefikset journalpostId`() {
        val journalpostHendelse = JournalpostHendelse(journalpostId = "BID-101")

        assertThat(journalpostHendelse.hentJournalpostIdUtenPrefix()).isEqualTo("101")
    }

    @Test
    fun `skal hente fagområde fra prefikset journalpostId`() {
        val journalpostHendelse = JournalpostHendelse(journalpostId = "BID-101")

        assertThat(journalpostHendelse.hentFagomradeFraDetaljer()).isEqualTo("BID")
    }

    @Test
    fun `skal hente null når hendelse er ukjent`() {
        val journalpostHendelse = JournalpostHendelse(hendelse = "ikke kjent hendelse")

        assertThat(journalpostHendelse.hentHendelse()).isNull()
    }
}