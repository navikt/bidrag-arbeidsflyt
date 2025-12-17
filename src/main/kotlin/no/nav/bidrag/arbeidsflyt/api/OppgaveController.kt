package no.nav.bidrag.arbeidsflyt.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.arbeidsflyt.service.OppgaveService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class OppgaveController(
    private val oppgaveService: OppgaveService,
) {
    @PostMapping("/gjenoppprett/{oppgaveId}")
    @Operation(
        description = "Gjenopprett oppgave som er ferdigstillt",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "400",
                description = "Dokument ble bestilt med ugyldig data",
            ),
        ],
    )
    fun gjenoopprettOppgave(
        @PathVariable oppgaveId: Long,
    ): Long = oppgaveService.gjenopprettOppgave(oppgaveId)
}
