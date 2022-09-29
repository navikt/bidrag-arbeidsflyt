package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveSokResponse
import no.nav.bidrag.arbeidsflyt.dto.PatchOppgaveRequest
import no.nav.bidrag.arbeidsflyt.service.ArbeidsfordelingService
import no.nav.bidrag.arbeidsflyt.service.OppgaveService
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import no.nav.bidrag.dokument.dto.Sporingsdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@DisplayName("OppdaterOppgaver")
@ActiveProfiles("test")
internal class BehandleJournalpostHendelseTest {

    private val enhetsnummerFraSporingsdata = "1001"

    @Autowired
    private lateinit var oppgaveService: OppgaveService

    @Autowired
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockBean
    private lateinit var httpHeaderRestTemplateMock: HttpHeaderRestTemplate


    private lateinit var behandleJournalpostHendelse: BehandleJournalpostHendelse
    private val journalpostHendelse = JournalpostHendelse(sporing = Sporingsdata(enhetsnummer = enhetsnummerFraSporingsdata))

    @BeforeEach
    fun `init OppdaterOppgaver med oppgavesøk`() {

        whenever(
            httpHeaderRestTemplateMock.exchange(
                anyString(),
                eq(HttpMethod.GET),
                anyOrNull(),
                eq(OppgaveSokResponse::class.java)
            )
        ).thenReturn(ResponseEntity.ok(OppgaveSokResponse(antallTreffTotalt = 1, listOf(OppgaveData(oppgavetype = JOURNALFORINGSOPPGAVE)))))

        behandleJournalpostHendelse = BehandleJournalpostHendelse(
            journalpostHendelse = journalpostHendelse,
            oppgaveService = oppgaveService,
            arbeidsfordelingService = arbeidsfordelingService
        )
    }

    @Test
    fun `skal sette endretAvEnhetsnummer når oppdatering av eksternt fagområde gjøres`() {
        journalpostHendelse.fagomrade = "IKKE_BIDRAG"

        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(OppgaveData::class.java)))
            .thenReturn(ResponseEntity.ok(OppgaveData()))

        // then
        behandleJournalpostHendelse.oppdaterEksterntFagomrade()

        val patchEntityCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)

        verify(httpHeaderRestTemplateMock).exchange(
            anyString(),
            eq(HttpMethod.PATCH),
            patchEntityCaptor.capture(),
            eq(OppgaveData::class.java)
        )

        val patchRequest = patchEntityCaptor.value.body as PatchOppgaveRequest

        assertThat(patchRequest).`as`("PatchOppgaveRequest").isNotNull
        assertThat(patchRequest.endretAvEnhetsnr).`as`("PatchOppgaveRequest.endretAvEnhetsnummer").isEqualTo(enhetsnummerFraSporingsdata)
    }

    @Test
    fun `skal sette endretAvEnhetsnummer når oppdatering av enhetsnummer gjøres`() {
        journalpostHendelse.enhet = "1234"

        whenever(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(OppgaveData::class.java)))
            .thenReturn(ResponseEntity.ok(OppgaveData()))

        // then
        behandleJournalpostHendelse.oppdaterEndretEnhetsnummer()

        val patchEntityCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)

        verify(httpHeaderRestTemplateMock).exchange(
            anyString(),
            eq(HttpMethod.PATCH),
            patchEntityCaptor.capture(),
            eq(OppgaveData::class.java)
        )

        val patchRequest = patchEntityCaptor.value.body as PatchOppgaveRequest

        assertThat(patchRequest).`as`("PatchOppgaveRequest").isNotNull
        assertThat(patchRequest.endretAvEnhetsnr).`as`("PatchOppgaveRequest.endretAvEnhetsnummer").isEqualTo(enhetsnummerFraSporingsdata)
    }
}