package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.persistence.repository.BehandlingRepository
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
) {
    @Transactional
    fun oppdaterBehandlingEnhet(oppgave: OppgaveData) {
        val behandling = behandlingRepository.finnForBehandlingEllerSøknadId(oppgave.behandlingsid!!.toLong(), oppgave.søknadsid?.toLong()) ?: return

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
