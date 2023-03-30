package no.nav.bidrag.arbeidsflyt.model

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.arbeidsflyt.dto.OppdaterOppgave
import no.nav.bidrag.arbeidsflyt.dto.OppgaveType
import no.nav.bidrag.arbeidsflyt.utils.createOppgaveHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import java.time.LocalDateTime

@DisplayName("OppdaterOppgaver")
internal class OppdaterOppgaveTest {

    val localDateTimeMock = mockStatic(LocalDateTime::class.java, Mockito.CALLS_REAL_METHODS)

    @BeforeEach
    fun `mock time`() {
        val mockTime = LocalDateTime.parse("2022-09-10T01:00:00.00")
        `when`(LocalDateTime.now()).thenReturn(mockTime)
    }

    @AfterEach
    fun `remove time mock`() {
        localDateTimeMock.close()
    }

    @Test
    fun `skal legge til beskrivelse for oppdatert oppgavetype`() {
        val existingBeskrivelse = "En beskrivelse fra før"
        val tilordnetRessurs = "Z99999"
        val hendelse = createOppgaveHendelse(id = 1, beskrivelse = existingBeskrivelse, tilordnetRessurs = tilordnetRessurs)
        val oppdaterOppgave = OppdaterOppgave(OppgaveDataForHendelse(hendelse))

        oppdaterOppgave.endreOppgavetype(OppgaveType.VUR)
        oppdaterOppgave.somHttpEntity()

        assertThat(oppdaterOppgave.beskrivelse).isEqualTo(
            "--- 10.09.2022 01:00 Automatisk jobb ---\r\n" +
                "· Oppgavetype endret fra Journalføring til Vurder dokument\r\n" +
                "· Saksbehandler endret fra $tilordnetRessurs til ikke valgt\r\n" +
                "\r\n\r\n$existingBeskrivelse"
        )
    }

    @Test
    fun `skal ikke legge til beskrivelse for endret tilordnetressurs hvis ikke satt fra før`() {
        val existingBeskrivelse = "En beskrivelse fra før"
        val hendelse = createOppgaveHendelse(id = 1, beskrivelse = existingBeskrivelse)
        val oppdaterOppgave = OppdaterOppgave(OppgaveDataForHendelse(hendelse))

        oppdaterOppgave.endreOppgavetype(OppgaveType.VUR)
        oppdaterOppgave.somHttpEntity()

        assertThat(oppdaterOppgave.beskrivelse).isEqualTo(
            "--- 10.09.2022 01:00 Automatisk jobb ---\r\n" +
                "· Oppgavetype endret fra Journalføring til Vurder dokument\r\n" +
                "\r\n\r\n$existingBeskrivelse"
        )
    }

    @Test
    fun `skal legge til beskrivelse hvis eksisterende beskrivelse er null`() {
        val hendelse = createOppgaveHendelse(id = 1, beskrivelse = null, tildeltEnhetsnr = null)
        val oppdaterOppgave = OppdaterOppgave(OppgaveDataForHendelse(hendelse))

        oppdaterOppgave.endreOppgavetype(OppgaveType.VUR)
        oppdaterOppgave.somHttpEntity()

        assertThat(oppdaterOppgave.beskrivelse).isEqualTo(
            "--- 10.09.2022 01:00 Automatisk jobb ---\r\n" +
                "· Oppgavetype endret fra Journalføring til Vurder dokument\r\n\r\n\r\n"
        )
    }

    @Test
    fun `skal legge til beskrivelse for endret enhet`() {
        val existingBeskrivelse = "En beskrivelse fra før"
        val tilordnetRessurs = "Z99999"
        val hendelse = createOppgaveHendelse(id = 1, beskrivelse = existingBeskrivelse, tilordnetRessurs = tilordnetRessurs, tildeltEnhetsnr = "4888")
        val oppdaterOppgave = OppdaterOppgave(OppgaveDataForHendelse(hendelse))

        oppdaterOppgave.overforTilEnhet("4806")
        oppdaterOppgave.somHttpEntity()

        assertThat(oppdaterOppgave.beskrivelse).isEqualTo(
            "--- 10.09.2022 01:00 Automatisk jobb ---\r\n" +
                "· Oppgave overført fra enhet 4888 til 4806\r\n" +
                "· Saksbehandler endret fra $tilordnetRessurs til ikke valgt\r\n" +
                "\r\n\r\n$existingBeskrivelse"
        )
    }

    @Test
    fun `skal ikke legge til beskrivelse hvis ikke endret`() {
        val existingBeskrivelse = "En beskrivelse fra før"
        val tilordnetRessurs = "Z99999"
        val hendelse = createOppgaveHendelse(id = 1, beskrivelse = existingBeskrivelse, tilordnetRessurs = tilordnetRessurs, tildeltEnhetsnr = "4888")
        val oppdaterOppgave = OppdaterOppgave(OppgaveDataForHendelse(hendelse))

        oppdaterOppgave.somHttpEntity()

        assertThat(oppdaterOppgave.beskrivelse).isNull()
    }

    @Test
    fun `skal mappe til json`() {
        val existingBeskrivelse = "En beskrivelse fra før"
        val tilordnetRessurs = "Z99999"
        val hendelse = createOppgaveHendelse(id = 1, beskrivelse = existingBeskrivelse, tilordnetRessurs = tilordnetRessurs, tildeltEnhetsnr = "4888")
        val oppdaterOppgave = OppdaterOppgave(OppgaveDataForHendelse(hendelse))

        assertThat(ObjectMapper().writeValueAsString(oppdaterOppgave)).isEqualTo("{\"id\":1,\"versjon\":1,\"endretAvEnhetsnr\":\"9999\"}")
    }

    @Test
    fun `skal mappe til json med endring`() {
        val existingBeskrivelse = "En beskrivelse fra før"
        val tilordnetRessurs = "Z99999"
        val hendelse = createOppgaveHendelse(id = 1, beskrivelse = existingBeskrivelse, tilordnetRessurs = tilordnetRessurs, tildeltEnhetsnr = "4888", oppgavetype = OppgaveType.VUR.name)
        val oppdaterOppgave = OppdaterOppgave(OppgaveDataForHendelse(hendelse))
        oppdaterOppgave.endreOppgavetype(OppgaveType.JFR)
        oppdaterOppgave.somHttpEntity()
        assertThat(ObjectMapper().writeValueAsString(oppdaterOppgave)).isEqualTo(
            "{" +
                "\"id\":1," +
                "\"versjon\":1," +
                "\"endretAvEnhetsnr\":\"9999\"," +
                "\"oppgavetype\":\"JFR\"," +
                "\"tilordnetRessurs\":\"\"," +
                "\"beskrivelse\":\"--- 10.09.2022 01:00 Automatisk jobb ---\\r\\n· Oppgavetype endret fra Vurder dokument til Journalføring\\r\\n· Saksbehandler endret fra Z99999 til ikke valgt\\r\\n\\r\\n\\r\\nEn beskrivelse fra før\"}"
        )
    }
}
