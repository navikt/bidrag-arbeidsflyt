package no.nav.bidrag.arbeidsflyt.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.persistence.repository.BehandlingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger {}

/**
 * Handles per-behandling processing during scheduled runs.
 * Each method uses REQUIRES_NEW so that a failure in one behandling
 * does not roll back statusSjekketTidspunkt updates for other behandlinger.
 */
@Service
class BehandlingSchedulerService(
    private val behandlingRepository: BehandlingRepository,
    private val behandleBehandlingHendelseService: BehandleBehandlingHendelseService,
) {
    /**
     * Processes a single behandling and unconditionally updates statusSjekketTidspunkt
     * in its own independent transaction so the timestamp is always committed,
     * regardless of whether [BehandleBehandlingHendelseService.behandleHendelse] succeeds or fails.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun behandleOgOppdaterStatusSjekket(behandlingId: Long) {
        val behandling =
            behandlingRepository.findById(behandlingId).orElseThrow {
                IllegalStateException("Behandling med id=$behandlingId ikke funnet")
            }

        try {
            behandleBehandlingHendelseService.behandleHendelse(behandling.hendelse!!, true)
        } catch (e: Exception) {
            LOGGER.error(e) { "Feil ved behandling av hendelse for behandling med id=$behandlingId" }
        } finally {
            behandling.statusSjekketTidspunkt = LocalDateTime.now()
        }
    }
}
