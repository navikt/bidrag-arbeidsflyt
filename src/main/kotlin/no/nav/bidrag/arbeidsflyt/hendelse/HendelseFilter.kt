package no.nav.bidrag.arbeidsflyt.hendelse

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class HendelseFilter {
    @Value("\${ENABLE_EVENTS}")
    private val stottedeHendelserFraEnv: String? = null
    val stottedeHendelser: Set<String> get() = HashSet(ArrayList(stottedeHendelserFraEnv?.split(",") ?: emptyList()))
}
