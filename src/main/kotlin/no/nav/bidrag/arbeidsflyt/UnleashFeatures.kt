package no.nav.bidrag.arbeidsflyt

import io.getunleash.variant.Variant
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider

enum class UnleashFeatures(
    val featureName: String,
    defaultValue: Boolean,
) {
    DEBUG_LOGGING("debug_logging", false),

    // I Q1 ved opprettelse av klage så blir alle inntekter fjernet fordi de ikke finnes i testmiljøene.
    // Dette er for å unngå de slettes ved grunnlagsinnhenting
    BEHANDLE_BEHANDLING_HENDELSE("bisys.behandle_behandling_hendelse", false),
    ;

    private var defaultValue = false

    init {
        this.defaultValue = defaultValue
    }

    val isEnabled: Boolean
        get() = UnleashFeaturesProvider.isEnabled(feature = featureName, defaultValue = defaultValue)

    val variant: Variant?
        get() = UnleashFeaturesProvider.getVariant(featureName)
}
