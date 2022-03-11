package no.nav.bidrag.arbeidsflyt.utils

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class FeatureToggle {
    enum class Feature {
        KAFKA_OPPGAVE
    }

    @Value("\${FEATURE_ENABLED:}")
    private val featureEnabled: String? = null

    fun isFeatureEnabled(feature: Feature): Boolean {
        return Optional.ofNullable(featureEnabled).orElse("").matches(feature.name.toRegex())
    }
}