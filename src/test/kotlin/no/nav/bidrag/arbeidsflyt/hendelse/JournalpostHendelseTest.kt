package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.model.Hendelse
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class JournalpostHendelseTest {
    @Test
    fun `skal hente journalpostId fra prefikset journalpostId`() {
        val journalpostHendelse = JournalpostHendelse(journalpostId = "BID-101")

        assertThat(journalpostHendelse.hentJournalpostIdUtenPrefix()).isEqualTo("101")
    }

    @Test
    @DisplayName("skal hente fagområde fra prefikset journalpostId")
    fun `skal hente fagomrade fra prefikset journalpostId`() {
        val journalpostHendelse = JournalpostHendelse(journalpostId = "BID-101")

        assertThat(journalpostHendelse.hentFagomradeFraDetaljer()).isEqualTo("BID")
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal hente NO_SUPPORT når hendelse er ukjent`() {
        val journalpostHendelse = JournalpostHendelse(hendelse = "ikke kjent hendelse")

        assertThat(journalpostHendelse.hentHendelse()).isEqualTo(Hendelse.NO_SUPPORT)
    }
}