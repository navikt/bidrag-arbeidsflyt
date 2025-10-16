package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.persistence.entity.Behandling
import no.nav.bidrag.arbeidsflyt.persistence.repository.BehandlingRepository
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
) {
    @Transactional
    fun finnBehandling(oppgave: OppgaveData): Behandling? = finnForBehandlingsidEllerSøknadsid(oppgave.behandlingsid?.toLong(), oppgave.søknadsid?.toLong())

    @Transactional
    fun finnForBehandlingsidEllerSøknadsid(
        behandlingId: Long?,
        søknadsid: Long?,
    ): Behandling? =
        when {
            behandlingId == null && søknadsid == null -> null
            behandlingId == null -> behandlingRepository.finnForSøknadId(søknadsid!!)
            else -> {
                behandlingRepository.finnForBehandlingId(behandlingId) ?: behandlingRepository.finnForSøknadId(søknadsid!!)
            }
        }

    @Transactional
    fun lagre(behandling: Behandling) = behandlingRepository.save(behandling)

    @Transactional
    fun oppdaterBehandlingEnhet(oppgave: OppgaveData) {
        val behandling = finnForBehandlingsidEllerSøknadsid(oppgave.behandlingsid?.toLong(), oppgave.søknadsid!!.toLong()) ?: return

        behandling.enhet = oppgave.tildeltEnhetsnr ?: behandling.enhet
        behandling.oppgave?.oppgaver?.forEach {
            it.enhet = oppgave.tildeltEnhetsnr ?: it.enhet
        }
    }

    @Transactional
    fun oppdaterStatusPåOppgaverBehandlingTilFerdigstilt(oppgave: OppgaveData) {
        if (!oppgave.erStatusKategoriAvsluttet) return
        val behandlinger = behandlingRepository.finnBehandlingSomHarOppgave(oppgave.id.toString())

        behandlinger.forEach { behandling ->
            behandling.oppgave?.oppgaver?.forEach {
                if (it.oppgaveId == oppgave.id) {
                    it.ferdigstilt = true
                }
            }
        }
    }
}
