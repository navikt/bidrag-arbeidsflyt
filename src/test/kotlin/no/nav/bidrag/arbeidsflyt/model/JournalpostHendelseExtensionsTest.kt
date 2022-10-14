package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.dokument.dto.JournalpostHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq

class JournalpostHendelseExtensionsTest {

    @Test
    fun `should get fnr with no spaces`(){
        val jpHendelse = JournalpostHendelse(
            journalpostId = "38837926",
            fnr = "123213 123123 12323"
        )

        assertThat(jpHendelse.fnrNoSpaces).isEqualTo("12321312312312323")
    }

    @Test
    fun `should get journalpostId with BID prefix`(){
        val jpHendelse = JournalpostHendelse(
            journalpostId = "38837926"
        )

        val jpHendelse2 = JournalpostHendelse(
            journalpostId = "BID-38837926"
        )

        assertThat(jpHendelse.journalpostMedPrefix).isEqualTo("BID-38837926")
        assertThat(jpHendelse2.journalpostMedPrefix).isEqualTo("BID-38837926")
    }

    @Test
    fun `should get journalpostId with JOARK prefix`(){
        val jpHendelse1 = JournalpostHendelse(
            journalpostId = "573781136"
        )

        val jpHendelse2 = JournalpostHendelse(
            journalpostId = "JOARK-573781136"
        )

        assertThat(jpHendelse1.journalpostMedPrefix).isEqualTo("JOARK-573781136")
        assertThat(jpHendelse2.journalpostMedPrefix).isEqualTo("JOARK-573781136")
    }
}