package no.nav.bidrag.arbeidsflyt.hendelse

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.verify
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.bidrag.arbeidsflyt.UnleashFeatures
import no.nav.bidrag.arbeidsflyt.consumer.BehandlingDetaljerDtoV2
import no.nav.bidrag.arbeidsflyt.consumer.ForholdmessigFordelingDetaljerDto
import no.nav.bidrag.arbeidsflyt.dto.METADATA_NØKKEL_SØKNAD_ID
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.persistence.repository.BehandlingRepository
import no.nav.bidrag.arbeidsflyt.service.BehandleBehandlingHendelseService
import no.nav.bidrag.arbeidsflyt.utils.enableUnleashFeature
import no.nav.bidrag.arbeidsflyt.utils.opprettSakForBehandling
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.organisasjon.dto.SaksbehandlerDto
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelse
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelseBarn
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelseType
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import no.nav.bidrag.transport.dokument.Sporingsdata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Tester for [BehandleBehandlingHendelseService] sin overføring av oppgaver til saksbehandleren
 * som opprettet en forholdsmessig fordeling (FF) på en behandling.
 */
internal class BehandlingHendelseFFOverforingTest : AbstractBehandleHendelseTest() {
    @Autowired
    lateinit var behandleHendelseService: BehandleBehandlingHendelseService

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    private val saksbehandlerSomOpprettetFF = "Z000001"
    private val enhetSomOpprettetFF = "4812"

    @BeforeEach
    fun initUnleash() {
        enableUnleashFeature(UnleashFeatures.BEHANDLE_BEHANDLING_HENDELSE)
    }

    private fun stubHentBehandlingDetaljer(
        behandlingsid: Long,
        forholdsmessigFordeling: ForholdmessigFordelingDetaljerDto? =
            ForholdmessigFordelingDetaljerDto(
                opprettetAvSaksbehandler = saksbehandlerSomOpprettetFF,
                opprettetAvEnhet = enhetSomOpprettetFF,
            ),
    ) {
        val respons =
            BehandlingDetaljerDtoV2(
                id = behandlingsid,
                saksnummer = "123456",
                opprettetAv = SaksbehandlerDto("Z999999", "Testbruker"),
                forholdsmessigFordeling = forholdsmessigFordeling,
            )
        stubFor(
            get(urlMatching("/behandling/api/v2/behandling/detaljer/$behandlingsid"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.CONNECTION, "close")
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withStatus(HttpStatus.OK.value())
                        .withBody(objectMapper.writeValueAsString(respons)),
                ),
        )
    }

    private fun opprettHendelse(behandlingsid: Long): BehandlingHendelse =
        BehandlingHendelse(
            type = BehandlingHendelseType.OPPRETTET,
            status = BehandlingStatusType.UNDER_BEHANDLING,
            vedtakstype = Vedtakstype.ENDRING,
            behandlingsid = behandlingsid,
            opprettetTidspunkt = LocalDateTime.now(),
            endretTidspunkt = LocalDateTime.now(),
            behandlerEnhet = "4806",
            søknadsid = 123,
            mottattDato = LocalDate.parse("2020-06-01"),
            sporingsdata = Sporingsdata("test", "test", "test", enhetsnummer = "4806"),
            barn = listOf(opprettBarn()),
        )

    private fun opprettBarn() =
        BehandlingHendelseBarn(
            saksnummer = "123456",
            behandlingstype = Behandlingstype.ENDRING,
            behandlingstema = Behandlingstema.BIDRAG,
            status = Behandlingstatus.UNDER_BEHANDLING,
            stønadstype = Stønadstype.BIDRAG,
            engangsbeløptype = null,
            søktAv = SøktAvType.BIDRAGSMOTTAKER,
            søktFraDato = LocalDate.parse("2020-06-01"),
            ident = "123213",
            søknadsid = 123,
            behandlerEnhet = "4806",
        )

    private fun stubOppgaveForSaken(
        tilordnetRessurs: String? = "Z111111",
        tildeltEnhetsnr: String? = "4806",
        status: OppgaveStatus? = OppgaveStatus.OPPRETTET,
    ) {
        stubHentOppgaveSok(
            oppgaver =
                listOf(
                    OppgaveData(
                        id = 555L,
                        versjon = 1,
                        saksreferanse = "123456",
                        tema = "BID",
                        tildeltEnhetsnr = tildeltEnhetsnr,
                        tilordnetRessurs = tilordnetRessurs,
                        status = status,
                        metadata = mapOf(METADATA_NØKKEL_SØKNAD_ID to "123"),
                    ),
                ),
        )
    }

    @Test
    fun `skal overføre oppgave til saksbehandler som opprettet FF`() {
        val behandlingsid = 555555L
        val hendelse = opprettHendelse(behandlingsid)
        stubHentSak(opprettSakForBehandling(hendelse.barn.first()))
        stubOppgaveForSaken(tilordnetRessurs = "Z111111", tildeltEnhetsnr = "4806")
        stubHentBehandlingDetaljer(behandlingsid)

        behandleHendelseService.behandleHendelse(hendelse)

        verify(1, getRequestedFor(urlMatching("/behandling/api/v2/behandling/detaljer/.*")))
        val overførtRequest = getOppgaveEndretRequest(oppgaveId = 555L)
        overførtRequest.shouldNotBeNull()
        overførtRequest!!.tilordnetRessurs shouldBe saksbehandlerSomOpprettetFF
        overførtRequest.tildeltEnhetsnr shouldBe enhetSomOpprettetFF

        val behandling = behandlingRepository.finnForBehandlingId(behandlingsid)
        behandling.shouldNotBeNull()
        behandling!!.oppgaverOverførtEtterFFOpprettet.shouldNotBeNull()
    }

    @Test
    fun `skal ikke overføre oppgave hvis den allerede er tilordnet saksbehandler som opprettet FF`() {
        val behandlingsid = 555556L
        val hendelse = opprettHendelse(behandlingsid)
        stubHentSak(opprettSakForBehandling(hendelse.barn.first()))
        stubOppgaveForSaken(tilordnetRessurs = saksbehandlerSomOpprettetFF, tildeltEnhetsnr = enhetSomOpprettetFF)
        stubHentBehandlingDetaljer(behandlingsid)

        behandleHendelseService.behandleHendelse(hendelse)

        verifyOppgaveNotEndret()

        val behandling = behandlingRepository.finnForBehandlingId(behandlingsid)
        behandling.shouldNotBeNull()
        behandling!!.oppgaverOverførtEtterFFOpprettet.shouldNotBeNull()
    }

    @Test
    fun `skal ikke overføre oppgave hvis behandling ikke har forholdsmessig fordeling`() {
        val behandlingsid = 555557L
        val hendelse = opprettHendelse(behandlingsid)
        stubHentSak(opprettSakForBehandling(hendelse.barn.first()))
        stubOppgaveForSaken(tilordnetRessurs = "Z111111", tildeltEnhetsnr = "4806")
        stubHentBehandlingDetaljer(behandlingsid, forholdsmessigFordeling = null)

        behandleHendelseService.behandleHendelse(hendelse)

        verifyOppgaveNotEndret()

        val behandling = behandlingRepository.finnForBehandlingId(behandlingsid)
        behandling.shouldNotBeNull()
        behandling!!.oppgaverOverførtEtterFFOpprettet.shouldBeNull()
    }

    @Test
    fun `skal ikke overføre oppgaver på nytt ved påfølgende hendelser for samme behandling (hindre dobbel prosessering)`() {
        val behandlingsid = 555558L
        val hendelse = opprettHendelse(behandlingsid)
        stubHentSak(opprettSakForBehandling(hendelse.barn.first()))
        stubOppgaveForSaken(tilordnetRessurs = "Z111111", tildeltEnhetsnr = "4806")
        stubHentBehandlingDetaljer(behandlingsid)

        behandleHendelseService.behandleHendelse(hendelse)
        verify(1, patchRequestedFor(urlMatching("/oppgave/api/v1/oppgaver/.*")))

        val behandlingEtterFørsteKall = behandlingRepository.finnForBehandlingId(behandlingsid)
        behandlingEtterFørsteKall.shouldNotBeNull()
        val overførtTidspunktEtterFørsteKall = behandlingEtterFørsteKall!!.oppgaverOverførtEtterFFOpprettet
        overførtTidspunktEtterFørsteKall.shouldNotBeNull()

        // Send samme hendelse på nytt (feks pga replay/duplikat) med nytt endret tidspunkt
        val andreHendelse = hendelse.copy(endretTidspunkt = LocalDateTime.now().plusMinutes(1))
        behandleHendelseService.behandleHendelse(andreHendelse)

        // Fremdeles kun én overføring skal ha skjedd totalt
        verify(1, patchRequestedFor(urlMatching("/oppgave/api/v1/oppgaver/.*")))

        val behandlingEtterAndreKall = behandlingRepository.finnForBehandlingId(behandlingsid)
        behandlingEtterAndreKall.shouldNotBeNull()
        behandlingEtterAndreKall!!.oppgaverOverførtEtterFFOpprettet shouldBe overførtTidspunktEtterFørsteKall
    }
}
