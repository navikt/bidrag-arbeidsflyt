package no.nav.bidrag.arbeidsflyt.hendelse

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.service.BehandleOppgaveHendelseService
import no.nav.bidrag.arbeidsflyt.utils.AKTOER_ID
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVETYPE_JFR
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_1
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_1
import no.nav.bidrag.arbeidsflyt.utils.createOppgaveHendelse
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource

@TestPropertySource(properties = ["FEATURE_ENABLED=KAFKA_OPPGAVE"])
class FeatureToggleOpprettOppgave: AbstractBehandleHendelseTest() {
    @Autowired
    lateinit var behandleOppgaveHendelseService: BehandleOppgaveHendelseService
    @Test
    fun `skal ikke opprette oppgave nar oppgave ferdigstilt men journalpost status er mottatt når feature toggle av`(){
        stubHentOppgave(listOf(
            OppgaveData(
            id = OPPGAVE_ID_1,
            versjon = 1,
            journalpostId = JOURNALPOST_ID_1,
            aktoerId = AKTOER_ID,
            oppgavetype = "BEH_SAK",
            tema = "BID",
            tildeltEnhetsnr = "4833"
        )
        ))
        val oppgaveHendelse = createOppgaveHendelse(OPPGAVE_ID_1, journalpostId = JOURNALPOST_ID_1, fnr = PERSON_IDENT_1, status = OppgaveStatus.FERDIGSTILT)

        behandleOppgaveHendelseService.behandleEndretOppgave(oppgaveHendelse)

        val endretOppgaveOptional = testDataGenerator.hentOppgave(OPPGAVE_ID_1)
        Assertions.assertThat(endretOppgaveOptional.isPresent).isTrue

        Assertions.assertThat(endretOppgaveOptional).hasValueSatisfying { oppgave ->
            Assertions.assertThat(oppgave.oppgaveId).isEqualTo(OPPGAVE_ID_1)
            Assertions.assertThat(oppgave.ident).isEqualTo(PERSON_IDENT_1)
            Assertions.assertThat(oppgave.oppgavetype).isEqualTo(OPPGAVETYPE_JFR)
            Assertions.assertThat(oppgave.tema).isEqualTo("BID")
            Assertions.assertThat(oppgave.status).isEqualTo(OppgaveStatus.FERDIGSTILT.name)
        }

        verifyOppgaveNotOpprettet()
    }
}

