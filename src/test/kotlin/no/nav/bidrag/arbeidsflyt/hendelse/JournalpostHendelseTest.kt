package no.nav.bidrag.arbeidsflyt.hendelse

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class JournalpostHendelseTest {
    @Test
    fun `skal hente journalpostId som tall fra prefikset journalpostId`() {
        val journalpostHendelse = JournalpostHendelse(journalpostId = "BID-101")

        assertThat(journalpostHendelse.hentIdUtenPrefix()).isEqualTo("101")
    }

    @Test
    @DisplayName("skal hente fagområde fra prefikset journalpostId")
    fun `skal hente fagomrade fra prefikset journalpostId`() {
        val journalpostHendelse = JournalpostHendelse(journalpostId = "BID-101")

        assertThat(journalpostHendelse.hentFagomradeFraId()).isEqualTo("BID")
    }
}