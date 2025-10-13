package no.nav.bidrag.arbeidsflyt.hendelse

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.bidrag.arbeidsflyt.dto.METADATA_NØKKEL_BEHANDLING_ID
import no.nav.bidrag.arbeidsflyt.dto.METADATA_NØKKEL_NORM_DATO
import no.nav.bidrag.arbeidsflyt.dto.METADATA_NØKKEL_SØKNAD_ID
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveType
import no.nav.bidrag.arbeidsflyt.dto.Prioritet
import no.nav.bidrag.arbeidsflyt.hendelse.dto.BehandlingHendelse
import no.nav.bidrag.arbeidsflyt.hendelse.dto.BehandlingHendelseBarn
import no.nav.bidrag.arbeidsflyt.hendelse.dto.BehandlingHendelseType
import no.nav.bidrag.arbeidsflyt.hendelse.dto.BehandlingStatusType
import no.nav.bidrag.arbeidsflyt.hendelse.dto.OppgaveKafkaHendelse
import no.nav.bidrag.arbeidsflyt.persistence.entity.Behandling
import no.nav.bidrag.arbeidsflyt.persistence.entity.BehandlingBarn
import no.nav.bidrag.arbeidsflyt.persistence.repository.BehandlingRepository
import no.nav.bidrag.arbeidsflyt.service.BehandleBehandlingHendelseService
import no.nav.bidrag.arbeidsflyt.utils.AKTOER_ID
import no.nav.bidrag.arbeidsflyt.utils.JOURNALPOST_ID_1
import no.nav.bidrag.arbeidsflyt.utils.OPPGAVE_ID_1
import no.nav.bidrag.arbeidsflyt.utils.PERSON_IDENT_1
import no.nav.bidrag.arbeidsflyt.utils.opprettSakForBehandling
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.særbidrag.Særbidragskategori
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.transport.dokument.Sporingsdata
import no.nav.bidrag.transport.dokumentmaler.Barn
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class BehandlingHendelseTest : AbstractBehandleHendelseTest() {
    @Autowired
    lateinit var behandleHendelseService: BehandleBehandlingHendelseService

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Test
    fun `skal opprette oppgave for behandlinghendelse`() {
        val behandlingsid = 123123L

        val hendelse = opprettHendelse(behandlingsid)
        val barnHendelse = hendelse.barn.first()
        stubHentOppgaveSok(emptyList())
        stubHentSak(
            opprettSakForBehandling(barnHendelse),
        )
        behandleHendelseService.behandleHendelse(hendelse)
        val behandling = behandlingRepository.finnForBehandlingEllerSøknadId(behandlingsid)
        behandling.shouldNotBeNull()
        assertSoftly(behandling) {
            this.behandlingsid shouldBe behandlingsid
            søknadsid shouldBe 123
            status shouldBe BehandlingStatusType.UNDER_BEHANDLING
            mottattDato shouldBe LocalDate.parse("2020-06-01")
            enhet shouldBe "4806"
            barn.shouldNotBeNull()
            barn!!.barn shouldHaveSize 1
        }
        val oppgaveRequest = getOppgaveOpprettRequest()
        oppgaveRequest.shouldNotBeNull()
        assertSoftly(oppgaveRequest) {
            saksreferanse shouldBe "123456"
            tildeltEnhetsnr shouldBe "4806"
            tema shouldBe "BID"
            beskrivelse shouldContain "test (test, 4806) ---\r\nEndring - Barnebidrag"
            personident shouldBe "123123"
            oppgavetype shouldBe OppgaveType.BEH_SAK
            prioritet shouldBe Prioritet.LAV.name
            metadata?.get(METADATA_NØKKEL_BEHANDLING_ID) shouldBe behandlingsid.toString()
            metadata?.get(METADATA_NØKKEL_SØKNAD_ID) shouldBe "123"
            metadata?.get(METADATA_NØKKEL_NORM_DATO) shouldBe "09.09.2020"
        }
    }

    @Test
    fun `skal opprette oppgave for behandlinghendelse særbidrag`() {
        val behandlingsid = 123123L

        val hendelse =
            opprettHendelse(behandlingsid)
                .copy(
                    barn =
                        listOf(
                            opprettBarn()
                                .copy(
                                    engangsbeløptype = Engangsbeløptype.SÆRBIDRAG,
                                    stønadstype = null,
                                    særbidragskategori = Særbidragskategori.KONFIRMASJON,
                                ),
                        ),
                )
        val barnHendelse = hendelse.barn.first()
        stubHentOppgaveSok(emptyList())
        stubHentSak(
            opprettSakForBehandling(barnHendelse),
        )
        behandleHendelseService.behandleHendelse(hendelse)
        val behandling = behandlingRepository.finnForBehandlingEllerSøknadId(behandlingsid)
        behandling.shouldNotBeNull()
        assertSoftly(behandling) {
            this.behandlingsid shouldBe behandlingsid
            søknadsid shouldBe 123
            status shouldBe BehandlingStatusType.UNDER_BEHANDLING
            mottattDato shouldBe LocalDate.parse("2020-06-01")
            enhet shouldBe "4806"
            barn.shouldNotBeNull()
            barn!!.barn shouldHaveSize 1
        }
        val oppgaveRequest = getOppgaveOpprettRequest()
        oppgaveRequest.shouldNotBeNull()
        assertSoftly(oppgaveRequest) {
            saksreferanse shouldBe "123456"
            tildeltEnhetsnr shouldBe "4806"
            tema shouldBe "BID"
            beskrivelse shouldContain "test (test, 4806) ---\r\nEndring - Særtilskudd, Konfirmasjon"
            personident shouldBe "123123"
            oppgavetype shouldBe OppgaveType.BEH_SAK
            prioritet shouldBe Prioritet.LAV.name
            metadata?.get(METADATA_NØKKEL_BEHANDLING_ID) shouldBe behandlingsid.toString()
            metadata?.get(METADATA_NØKKEL_SØKNAD_ID) shouldBe "123"
            metadata?.get(METADATA_NØKKEL_NORM_DATO) shouldBe "30.08.2020"
        }
    }

    @Test
    fun `skal opprette oppgave for behandlinghendelse for flere saker`() {
        val behandlingsid = 123123L

        val hendelse =
            opprettHendelse(behandlingsid)
                .copy(
                    barn =
                        listOf(
                            opprettBarn()
                                .copy(
                                    saksnummer = "123123",
                                    søknadsid = 123123,
                                ),
                            opprettBarn()
                                .copy(
                                    saksnummer = "53545",
                                    søknadsid = 454545,
                                ),
                        ),
                )
        val barnHendelse1 = hendelse.barn.first()
        val barnHendelse2 = hendelse.barn[1]
        stubHentOppgaveSok(emptyList())
        stubHentSak(
            opprettSakForBehandling(barnHendelse1),
        )
        stubHentSak(
            opprettSakForBehandling(barnHendelse2),
        )
        behandleHendelseService.behandleHendelse(hendelse)
        val behandling = behandlingRepository.finnForBehandlingEllerSøknadId(behandlingsid)
        behandling.shouldNotBeNull()
        assertSoftly(behandling) {
            this.behandlingsid shouldBe behandlingsid
            søknadsid shouldBe 123
            status shouldBe BehandlingStatusType.UNDER_BEHANDLING
            mottattDato shouldBe LocalDate.parse("2020-06-01")
            enhet shouldBe "4806"
            barn.shouldNotBeNull()
            barn!!.barn shouldHaveSize 2
        }
        val oppgaveRequestBarn1 = getOppgaveOpprettRequest(barnHendelse1.saksnummer)
        oppgaveRequestBarn1.shouldNotBeNull()
        assertSoftly(oppgaveRequestBarn1) {
            saksreferanse shouldBe barnHendelse1.saksnummer
            tildeltEnhetsnr shouldBe "4806"
            tema shouldBe "BID"
            personident shouldBe "123123"
            oppgavetype shouldBe OppgaveType.BEH_SAK
            prioritet shouldBe Prioritet.LAV.name
            metadata?.get(METADATA_NØKKEL_BEHANDLING_ID) shouldBe behandlingsid.toString()
            metadata?.get(METADATA_NØKKEL_SØKNAD_ID) shouldBe barnHendelse1.søknadsid.toString()
            metadata?.get(METADATA_NØKKEL_NORM_DATO) shouldBe "09.09.2020"
        }
        val oppgaveRequestBarn2 = getOppgaveOpprettRequest(barnHendelse2.saksnummer)
        oppgaveRequestBarn2.shouldNotBeNull()
        assertSoftly(oppgaveRequestBarn2) {
            saksreferanse shouldBe barnHendelse2.saksnummer
            tildeltEnhetsnr shouldBe "4806"
            tema shouldBe "BID"
            personident shouldBe "123123"
            oppgavetype shouldBe OppgaveType.BEH_SAK
            prioritet shouldBe Prioritet.LAV.name
            metadata?.get(METADATA_NØKKEL_BEHANDLING_ID) shouldBe behandlingsid.toString()
            metadata?.get(METADATA_NØKKEL_SØKNAD_ID) shouldBe barnHendelse2.søknadsid.toString()
            metadata?.get(METADATA_NØKKEL_NORM_DATO) shouldBe "09.09.2020"
        }
    }

    @Test
    fun `skal opprette oppgave for behandlinghendelse med motregning`() {
        val behandlingsid = 123123L

        val hendelse =
            opprettHendelse(behandlingsid)
                .copy(
                    barn =
                        listOf(
                            opprettBarn().copy(
                                behandlingstema = Behandlingstema.MOTREGNING,
                                status = Behandlingstatus.UNDER_BEHANDLING,
                                stønadstype = Stønadstype.MOTREGNING,
                            ),
                        ),
                )
        val barnHendelse = hendelse.barn.first()
        stubHentOppgaveSok(emptyList())
        stubHentSak(
            opprettSakForBehandling(barnHendelse),
        )
        behandleHendelseService.behandleHendelse(hendelse)
        val behandling = behandlingRepository.finnForBehandlingEllerSøknadId(behandlingsid)
        behandling.shouldNotBeNull()
        assertSoftly(behandling) {
            this.behandlingsid shouldBe behandlingsid
            søknadsid shouldBe 123
            status shouldBe BehandlingStatusType.UNDER_BEHANDLING
            mottattDato shouldBe LocalDate.parse("2020-06-01")
            enhet shouldBe "4806"
            barn.shouldNotBeNull()
            barn!!.barn shouldHaveSize 1
        }
        val oppgaveRequest = getOppgaveOpprettRequest()
        oppgaveRequest.shouldNotBeNull()
        assertSoftly(oppgaveRequest) {
            saksreferanse shouldBe "123456"
            tildeltEnhetsnr shouldBe "4806"
            tema shouldBe "BII"
            personident shouldBe "123123"
            oppgavetype shouldBe OppgaveType.IN
            prioritet shouldBe Prioritet.LAV.name
            metadata?.get(METADATA_NØKKEL_BEHANDLING_ID) shouldBe behandlingsid.toString()
            metadata?.get(METADATA_NØKKEL_SØKNAD_ID) shouldBe "123"
            metadata?.get(METADATA_NØKKEL_NORM_DATO) shouldBe "30.08.2020"
        }
    }

    @Test
    fun `skal opprette oppgave for behandlinghendelse med avskriving`() {
        val behandlingsid = 123123L
        val hendelse =
            opprettHendelse(behandlingsid)
                .copy(
                    barn =
                        listOf(
                            opprettBarn().copy(
                                behandlingstype = Behandlingstype.ENDRING,
                                behandlingstema = Behandlingstema.AVSKRIVNING,
                            ),
                        ),
                )
        val barnHendelse = hendelse.barn.first()

        stubHentOppgaveSok(emptyList())
        stubHentSak(
            opprettSakForBehandling(barnHendelse),
        )
        behandleHendelseService.behandleHendelse(hendelse)
        val behandling = behandlingRepository.finnForBehandlingEllerSøknadId(behandlingsid)
        behandling.shouldNotBeNull()
        assertSoftly(behandling) {
            this.behandlingsid shouldBe behandlingsid
            søknadsid shouldBe 123
            status shouldBe BehandlingStatusType.UNDER_BEHANDLING
            mottattDato shouldBe LocalDate.parse("2020-06-01")
            enhet shouldBe "4806"
            barn.shouldNotBeNull()
            barn!!.barn shouldHaveSize 1
        }
        val oppgaveRequest = getOppgaveOpprettRequest()
        oppgaveRequest.shouldNotBeNull()
        assertSoftly(oppgaveRequest) {
            saksreferanse shouldBe "123456"
            tildeltEnhetsnr shouldBe "4806"
            tema shouldBe "BID"
            personident shouldBe "123123"
            oppgavetype shouldBe OppgaveType.GEN
            prioritet shouldBe Prioritet.LAV.name
            metadata?.get(METADATA_NØKKEL_BEHANDLING_ID) shouldBe behandlingsid.toString()
            metadata?.get(METADATA_NØKKEL_SØKNAD_ID) shouldBe "123"
            metadata?.get(METADATA_NØKKEL_NORM_DATO) shouldBe "09.09.2020"
        }
    }

    @Test
    fun `skal ikke opprette oppgave for behandlinghendelse hvis status er åpen`() {
        val behandlingsid = 123123L
        val hendelse =
            opprettHendelse(behandlingsid)
                .copy(
                    status = BehandlingStatusType.ÅPEN,
                    vedtakstype = Vedtakstype.ENDRING,
                    barn =
                        listOf(
                            opprettBarn().copy(
                                status = Behandlingstatus.FAR_UKJENT,
                            ),
                        ),
                )
        val barnHendelse = hendelse.barn.first()
        stubHentOppgaveSok(emptyList())
        stubHentSak(
            opprettSakForBehandling(barnHendelse),
        )
        behandleHendelseService.behandleHendelse(hendelse)
        val behandling = behandlingRepository.finnForBehandlingEllerSøknadId(behandlingsid)
        behandling.shouldNotBeNull()
        assertSoftly(behandling) {
            this.behandlingsid shouldBe behandlingsid
            søknadsid shouldBe 123
            status shouldBe BehandlingStatusType.ÅPEN
            mottattDato shouldBe LocalDate.parse("2020-06-01")
            enhet shouldBe "4806"
            barn.shouldNotBeNull()
            barn!!.barn shouldHaveSize 1
        }
        val oppgaveRequest = getOppgaveOpprettRequest()
        oppgaveRequest.shouldBeNull()
    }

    @Test
    fun `skal opprette oppgave med norm dato dagens dato hvis behandling hadde status åpen fra før`() {
        val behandlingsid = 123123L
        val behandlingFør =
            Behandling(
                barn = BehandlingBarn(),
                enhet = "4806",
                behandlingsid = behandlingsid,
                mottattDato = LocalDate.parse("2020-06-01"),
                status = BehandlingStatusType.ÅPEN,
            )
        behandlingRepository.save(behandlingFør)
        val hendelse = opprettHendelse(behandlingsid)
        val barnHendelse = hendelse.barn.first()

        stubHentOppgaveSok(emptyList())
        stubHentSak(
            opprettSakForBehandling(barnHendelse),
        )
        behandleHendelseService.behandleHendelse(hendelse)
        val behandling = behandlingRepository.finnForBehandlingEllerSøknadId(behandlingsid)
        behandling.shouldNotBeNull()
        behandling.normDato shouldBe LocalDate.now()
        val oppgaveRequest = getOppgaveOpprettRequest()
        oppgaveRequest.shouldNotBeNull()
        assertSoftly(oppgaveRequest) {
            saksreferanse shouldBe "123456"
            tildeltEnhetsnr shouldBe "4806"
            tema shouldBe "BID"
            personident shouldBe "123123"
            oppgavetype shouldBe OppgaveType.BEH_SAK
            prioritet shouldBe Prioritet.LAV.name
            metadata?.get(METADATA_NØKKEL_BEHANDLING_ID) shouldBe behandlingsid.toString()
            metadata?.get(METADATA_NØKKEL_SØKNAD_ID) shouldBe "123"
            metadata?.get(METADATA_NØKKEL_NORM_DATO) shouldBe LocalDate.now().plusDays(100).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }
    }

    @Test
    fun `skal lukke oppgaver hvis status avsluttet`() {
        val behandlingsid = 123123L
        val behandlingFør =
            Behandling(
                barn = BehandlingBarn(),
                enhet = "4806",
                behandlingsid = behandlingsid,
                mottattDato = LocalDate.parse("2020-06-01"),
                status = BehandlingStatusType.UNDER_BEHANDLING,
            )
        behandlingRepository.save(behandlingFør)
        val hendelse =
            opprettHendelse(behandlingsid)
                .copy(
                    type = BehandlingHendelseType.AVSLUTTET,
                    status = BehandlingStatusType.VEDTAK_FATTET,
                    vedtakstype = Vedtakstype.ENDRING,
                    barn =
                        listOf(
                            opprettBarn().copy(
                                status = Behandlingstatus.VEDTAK_FATTET,
                            ),
                        ),
                )
        val barnHendelse = hendelse.barn.first()

        stubHentOppgaveSok(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    journalpostId = JOURNALPOST_ID_1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "BEH_SAK",
                    tema = "BID",
                    tildeltEnhetsnr = "4833",
                    metadata =
                        mapOf(
                            METADATA_NØKKEL_BEHANDLING_ID to behandlingsid.toString(),
                            METADATA_NØKKEL_SØKNAD_ID to "123",
                        ),
                ),
            ),
        )
        stubHentSak(
            opprettSakForBehandling(barnHendelse),
        )
        behandleHendelseService.behandleHendelse(hendelse)
        val behandling = behandlingRepository.finnForBehandlingEllerSøknadId(behandlingsid)
        behandling.shouldNotBeNull()
        behandling.status shouldBe BehandlingStatusType.VEDTAK_FATTET
        val oppgaveRequest = getOppgaveOpprettRequest()
        oppgaveRequest.shouldBeNull()
        val oppgaveEndretRequest = getOppgaveEndretRequest(OPPGAVE_ID_1)
        oppgaveEndretRequest.shouldNotBeNull()
        assertSoftly(oppgaveEndretRequest) {
            status shouldBe "FERDIGSTILT"
        }
    }

    @Test
    fun `skal lukke oppgaver uten referanse når søknad opprettes`() {
        val behandlingsid = 123123L
        val hendelse = opprettHendelse(behandlingsid)
        val barnHendelse = hendelse.barn.first()
        stubHentOppgaveSok(
            listOf(
                OppgaveData(
                    id = OPPGAVE_ID_1,
                    versjon = 1,
                    journalpostId = JOURNALPOST_ID_1,
                    aktoerId = AKTOER_ID,
                    oppgavetype = "BEH_SAK",
                    tema = "BID",
                    tildeltEnhetsnr = "4833",
                    metadata =
                        mapOf(),
                ),
            ),
            emptyList(),
        )
        stubHentSak(
            opprettSakForBehandling(barnHendelse),
        )
        behandleHendelseService.behandleHendelse(hendelse)
        val behandling = behandlingRepository.finnForBehandlingEllerSøknadId(behandlingsid)
        behandling.shouldNotBeNull()
        behandling.status shouldBe BehandlingStatusType.UNDER_BEHANDLING
        val oppgaveRequest = getOppgaveOpprettRequest()
        oppgaveRequest.shouldNotBeNull()
        val oppgaveEndretRequest = getOppgaveEndretRequest(OPPGAVE_ID_1)
        oppgaveEndretRequest.shouldNotBeNull()
        assertSoftly(oppgaveEndretRequest) {
            status shouldBe "FERDIGSTILT"
        }
    }

    private fun opprettHendelse(
        behandlingsid: Long,
        hendelseStatus: BehandlingStatusType = BehandlingStatusType.UNDER_BEHANDLING,
        barnStatus: Behandlingstatus = Behandlingstatus.UNDER_BEHANDLING,
        behandlingstema: Behandlingstema = Behandlingstema.BIDRAG,
    ): BehandlingHendelse =
        BehandlingHendelse(
            type = BehandlingHendelseType.OPPRETTET,
            status = hendelseStatus,
            vedtakstype = Vedtakstype.ENDRING,
            behandlingsid = behandlingsid,
            opprettetTidspunkt = LocalDateTime.now(),
            endretTidspunkt = LocalDateTime.now(),
            behandlerEnhet = "4806",
            søknadsid = 123,
            mottattDato = LocalDate.parse("2020-06-01"),
            sporingsdata = Sporingsdata("test", "test", "test", enhetsnummer = "4806"),
            barn =
                listOf(
                    opprettBarn(barnStatus, behandlingstema),
                ),
        )

    fun opprettBarn(
        barnStatus: Behandlingstatus = Behandlingstatus.UNDER_BEHANDLING,
        behandlingstema: Behandlingstema = Behandlingstema.BIDRAG,
    ) = BehandlingHendelseBarn(
        saksnummer = "123456",
        behandlingstype = Behandlingstype.ENDRING,
        behandlingstema = behandlingstema,
        status = barnStatus,
        stønadstype = Stønadstype.BIDRAG,
        engangsbeløptype = null,
        søktAv = SøktAvType.BIDRAGSMOTTAKER,
        søktFraDato = LocalDate.parse("2020-06-01"),
        ident = "123213",
        søknadsid = 123,
        behandlerEnhet = "4806",
    )
}
