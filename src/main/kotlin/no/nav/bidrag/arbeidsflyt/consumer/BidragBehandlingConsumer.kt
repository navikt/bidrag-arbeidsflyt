package no.nav.bidrag.arbeidsflyt.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.organisasjon.dto.SaksbehandlerDto
import no.nav.bidrag.transport.behandling.behandling.HentÅpneBehandlingerRequest
import no.nav.bidrag.transport.behandling.behandling.HentÅpneBehandlingerRespons
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private val LOGGER = KotlinLogging.logger { }

data class BehandlingDetaljerDtoV2(
    val id: Long,
    val saksnummer: String,
    val opprettetAv: SaksbehandlerDto,
    val forholdsmessigFordeling: ForholdmessigFordelingDetaljerDto? = null,
)

data class ForholdmessigFordelingDetaljerDto(
    val opprettetAvSaksbehandler: String? = null,
    val opprettetAvEnhet: String? = null,
)

@Service
class BidragBehandlingConsumer(
    @param:Value($$"${BIDRAG_BEHANDLING_URL}") val url: URI,
    @param:Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-behandling") {
    private fun createUri(path: String?) =
        UriComponentsBuilder
            .fromUri(url)
            .path(path ?: "")
            .build()
            .toUri()

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    fun hentBehandling(behandlingId: Long): BehandlingDetaljerDtoV2 =
        try {
            getForNonNullEntity<BehandlingDetaljerDtoV2>(
                createUri("/api/v2/behandling/detaljer/$behandlingId"),
            )
        } catch (
            e: HttpStatusCodeException,
        ) {
            LOGGER.error(e) { "Det skjedde en feil ved henting av behandling $behandlingId" }
            throw e
        }
}
