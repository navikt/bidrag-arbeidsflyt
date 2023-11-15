package no.nav.bidrag.arbeidsflyt

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.commons.cache.InvaliderCacheFørStartenAvArbeidsdag
import org.slf4j.LoggerFactory
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
        const val JOURNALFORENDE_ENHET_CACHE = "JOURNALFORENDE_ENHET_CACHE"
        const val ENHET_INFO_CACHE = "ENHET_INFO_CACHE"
        private val LOGGER = LoggerFactory.getLogger(CacheConfig::class.java)
    }

    @Bean
    fun cacheManager(): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.registerCustomCache(
            PERSON_CACHE,
            Caffeine.newBuilder()
                .expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            GEOGRAFISK_ENHET_CACHE,
            Caffeine.newBuilder()
                .expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            JOURNALFORENDE_ENHET_CACHE,
            Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build(),
        )
        caffeineCacheManager.registerCustomCache(ENHET_INFO_CACHE, Caffeine.newBuilder().expireAfterWrite(7, TimeUnit.DAYS).build())
        caffeineCacheManager.registerCustomCache(TILGANG_TEMA_CACHE, Caffeine.newBuilder().expireAfterWrite(7, TimeUnit.DAYS).build())
        return caffeineCacheManager
    }
}
