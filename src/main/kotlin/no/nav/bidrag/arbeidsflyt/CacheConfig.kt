package no.nav.bidrag.arbeidsflyt

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.commons.cache.InvaliderCacheFørStartenAvArbeidsdag
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
@Profile(value = [PROFILE_NAIS, "local"])
class CacheConfig {
    companion object {
        const val TILGANG_TEMA_CACHE = "TILGANG_TEMA_CACHE"
        const val PERSON_CACHE = "PERSON_CACHE"
        const val GEOGRAFISK_ENHET_CACHE = "GEOGRAFISK_ENHET_CACHE"
        const val BBM_SØKNAD_CACHE = "BBM_SØKNAD_CACHE"
        const val SAK_CACHE = "SAK_CACHE"
        const val JOURNALFORENDE_ENHET_CACHE = "JOURNALFORENDE_ENHET_CACHE"
        const val ENHET_INFO_CACHE = "ENHET_INFO_CACHE"

        private const val BBM_SØKNAD_CACHE_TTL_SECONDS = 10L
        private const val SAK_CACHE_TTL_MINUTES = 10L
        private const val JOURNALFORENDE_ENHET_CACHE_TTL_DAYS = 30L
        private const val ENHET_INFO_CACHE_TTL_DAYS = 7L
        private const val TILGANG_TEMA_CACHE_TTL_DAYS = 7L
    }

    @Bean
    fun cacheManager(): CacheManager {
        val cacheDefinitions =
            listOf(
                CacheDefinition(PERSON_CACHE, dailyInvalidatedCache()),
                CacheDefinition(GEOGRAFISK_ENHET_CACHE, dailyInvalidatedCache()),
                CacheDefinition(BBM_SØKNAD_CACHE, expireAfterWriteCache(BBM_SØKNAD_CACHE_TTL_SECONDS, TimeUnit.SECONDS)),
                CacheDefinition(SAK_CACHE, expireAfterWriteCache(SAK_CACHE_TTL_MINUTES, TimeUnit.MINUTES)),
                CacheDefinition(JOURNALFORENDE_ENHET_CACHE, expireAfterWriteCache(JOURNALFORENDE_ENHET_CACHE_TTL_DAYS, TimeUnit.DAYS)),
                CacheDefinition(ENHET_INFO_CACHE, expireAfterWriteCache(ENHET_INFO_CACHE_TTL_DAYS, TimeUnit.DAYS)),
                CacheDefinition(TILGANG_TEMA_CACHE, expireAfterWriteCache(TILGANG_TEMA_CACHE_TTL_DAYS, TimeUnit.DAYS)),
            )

        return CaffeineCacheManager().also { cacheManager ->
            cacheDefinitions.forEach { cacheDefinition ->
                cacheManager.registerCustomCache(cacheDefinition.name, cacheDefinition.cache)
            }
        }
    }

    private fun dailyInvalidatedCache(): Cache<Any, Any> =
        Caffeine
            .newBuilder()
            .expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
            .build()

    private fun expireAfterWriteCache(
        duration: Long,
        timeUnit: TimeUnit,
    ): Cache<Any, Any> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(duration, timeUnit)
            .build()

    private data class CacheDefinition(
        val name: String,
        val cache: Cache<Any, Any>,
    )
}
