package no.nav.bidrag.arbeidsflyt.hendelse

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
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
import no.nav.bidrag.arbeidsflyt.persistence.entity.Behandling
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

    companion object {
        private const val OPPGAVE_ID = 555L
        private const val SAKSNUMMER = "123456"
        private const val SAKSBEHANDLER_SOM_OPPRETTET_FF = "Z000001"
        private const val ENHET_SOM_OPPRETTET_FF = "4812"
        private const val ANNEN_SAKSBEHANDLER = "Z111111"
        private const val ANNEN_ENHET = "4806"
    }

    @BeforeEach
    fun initUnleash() {
        enableUnleashFeature(UnleashFeatures.BEHANDLE_BEHANDLING_HENDELSE)
    }

    private fun stubHentBehandlingDetaljer(
        behandlingsid: Long,
        forholdsmessigFordeling: ForholdmessigFordelingDetaljerDto? =
            ForholdmessigFordelingDetaljerDto(
                opprettetAvSaksbehandler = SAKSBEHANDLER_SOM_OPPRETTET_FF,
                opprettetAvEnhet = ENHET_SOM_OPPRETTET_FF,
            ),
    ) {
        val respons =
            BehandlingDetaljerDtoV2(
                id = behandlingsid,
                saksnummer = SAKSNUMMER,
                opprettetAv = SaksbehandlerDto("Z999999", "Testbruker"),
                forholdsmessigFordeling = forholdsmessigFordeling,
            )
        stubFor(
            get(urlEqualTo("/behandling/api/v2/behandling/detaljer/$behandlingsid"))
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
            behandlerEnhet = ANNEN_ENHET,
            søknadsid = 123,
            mottattDato = LocalDate.parse("2020-06-01"),
            sporingsdata = Sporingsdata("test", "test", "test", enhetsnummer = ANNEN_ENHET),
            barn = listOf(opprettBarn()),
        )

    private fun opprettBarn() =
        BehandlingHendelseBarn(
            saksnummer = SAKSNUMMER,
            behandlingstype = Behandlingstype.ENDRING,
            behandlingstema = Behandlingstema.BIDRAG,
            status = Behandlingstatus.UNDER_BEHANDLING,
            stønadstype = Stønadstype.BIDRAG,
            engangsbeløptype = null,
            søktAv = SøktAvType.BIDRAGSMOTTAKER,
            søktFraDato = LocalDate.parse("2020-06-01"),
            ident = "123213",
            søknadsid = 123,
            behandlerEnhet = ANNEN_ENHET,
        )

    /**
     * Stubber oppgavesøket som både brukes for å avgjøre om det finnes åpne søknadsoppgaver
     * og for å finne oppgaver som eventuelt skal overføres etter at FF er opprettet.
     * Scoper stubben til [SAKSNUMMER] slik at et søk med feil saksnummer ikke ved en
     * feiltakelse matcher og skjuler en logikkfeil.
     */
    private fun stubOppgaveForSaken(
        tilordnetRessurs: String?,
        tildeltEnhetsnr: String?,
        status: OppgaveStatus? = OppgaveStatus.OPPRETTET,
    ) {
        stubHentOppgaveContaining(
            oppgaver =
                listOf(
                    OppgaveData(
                        id = OPPGAVE_ID,
                        versjon = 1,
                        saksreferanse = SAKSNUMMER,
                        tema = "BID",
                        tildeltEnhetsnr = tildeltEnhetsnr,
                        tilordnetRessurs = tilordnetRessurs,
                        status = status,
                        metadata = mapOf(METADATA_NØKKEL_SØKNAD_ID to "123"),
                    ),
                ),
            "saksreferanse" to SAKSNUMMER,
        )
    }

    private fun verifyHentBehandlingDetaljerKalt(
        behandlingsid: Long,
        antall: Int,
    ) {
        verify(antall, getRequestedFor(urlEqualTo("/behandling/api/v2/behandling/detaljer/$behandlingsid")))
    }

    private fun verifyOppgaveOverfortTilSaksbehandler(
        saksbehandler: String,
        enhet: String?,
    ) {
        val overførtRequest = getOppgaveEndretRequest(oppgaveId = OPPGAVE_ID)
        overførtRequest.shouldNotBeNull()
        overførtRequest.tilordnetRessurs shouldBe saksbehandler
        overførtRequest.tildeltEnhetsnr shouldBe enhet
    }

    private fun hentBehandling(behandlingsid: Long): Behandling {
        val behandling = behandlingRepository.finnForBehandlingId(behandlingsid)
        behandling.shouldNotBeNull()
        return behandling
    }

    @Test
    fun `skal overføre oppgave til saksbehandler som opprettet FF`() {
        val behandlingsid = 555555L
        val hendelse = opprettHendelse(behandlingsid)
        stubHentSak(opprettSakForBehandling(hendelse.barn.first()))
        stubOppgaveForSaken(tilordnetRessurs = ANNEN_SAKSBEHANDLER, tildeltEnhetsnr = ANNEN_ENHET)
        stubHentBehandlingDetaljer(behandlingsid)

        behandleHendelseService.behandleHendelse(hendelse)

        verifyHentBehandlingDetaljerKalt(behandlingsid, antall = 1)
        verifyOppgaveOverfortTilSaksbehandler(SAKSBEHANDLER_SOM_OPPRETTET_FF, ENHET_SOM_OPPRETTET_FF)
        hentBehandling(behandlingsid).oppgaverOverførtEtterFFOpprettet.shouldNotBeNull()
    }

    @Test
    fun `skal ikke overføre oppgave hvis den allerede er tilordnet saksbehandler som opprettet FF`() {
        val behandlingsid = 555556L
        val hendelse = opprettHendelse(behandlingsid)
        stubHentSak(opprettSakForBehandling(hendelse.barn.first()))
        stubOppgaveForSaken(tilordnetRessurs = SAKSBEHANDLER_SOM_OPPRETTET_FF, tildeltEnhetsnr = ENHET_SOM_OPPRETTET_FF)
        stubHentBehandlingDetaljer(behandlingsid)

        behandleHendelseService.behandleHendelse(hendelse)

        verifyOppgaveNotEndret()
        hentBehandling(behandlingsid).oppgaverOverførtEtterFFOpprettet.shouldNotBeNull()
    }

    @Test
    fun `skal ikke overføre oppgave hvis behandling ikke har forholdsmessig fordeling`() {
        val behandlingsid = 555557L
        val hendelse = opprettHendelse(behandlingsid)
        stubHentSak(opprettSakForBehandling(hendelse.barn.first()))
        stubOppgaveForSaken(tilordnetRessurs = ANNEN_SAKSBEHANDLER, tildeltEnhetsnr = ANNEN_ENHET)
        stubHentBehandlingDetaljer(behandlingsid, forholdsmessigFordeling = null)

        behandleHendelseService.behandleHendelse(hendelse)

        verifyOppgaveNotEndret()
        hentBehandling(behandlingsid).oppgaverOverførtEtterFFOpprettet.shouldBeNull()
    }

    @Test
    fun `skal ikke overføre oppgaver på nytt ved påfølgende hendelser for samme behandling (hindre dobbel prosessering)`() {
        val behandlingsid = 555558L
        val hendelse = opprettHendelse(behandlingsid)
        stubHentSak(opprettSakForBehandling(hendelse.barn.first()))
        stubOppgaveForSaken(tilordnetRessurs = ANNEN_SAKSBEHANDLER, tildeltEnhetsnr = ANNEN_ENHET)
        stubHentBehandlingDetaljer(behandlingsid)

        behandleHendelseService.behandleHendelse(hendelse)
        verify(1, patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/$OPPGAVE_ID")))
        verifyHentBehandlingDetaljerKalt(behandlingsid, antall = 1)

        val overførtTidspunktEtterFørsteKall = hentBehandling(behandlingsid).oppgaverOverførtEtterFFOpprettet
        overførtTidspunktEtterFørsteKall.shouldNotBeNull()

        // Send samme hendelse på nytt (feks pga replay/duplikat) med nytt endret tidspunkt
        val andreHendelse = hendelse.copy(endretTidspunkt = LocalDateTime.now().plusMinutes(1))
        behandleHendelseService.behandleHendelse(andreHendelse)

        // FF-detaljer hentes på nytt for hver hendelse ...
        verifyHentBehandlingDetaljerKalt(behandlingsid, antall = 2)
        // ... men oppgaven skal fremdeles kun ha blitt overført én gang totalt
        verify(1, patchRequestedFor(urlEqualTo("/oppgave/api/v1/oppgaver/$OPPGAVE_ID")))

        hentBehandling(behandlingsid).oppgaverOverførtEtterFFOpprettet shouldBe overførtTidspunktEtterFørsteKall
    }
}
