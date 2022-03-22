package no.nav.bidrag.arbeidsflyt

import com.github.benmanes.caffeine.cache.Caffeine
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
        const val PERSON_CACHE = "PERSON_CACHE"
    }

    @Bean
    fun cacheManager(): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.registerCustomCache(PERSON_CACHE, Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build())
        return caffeineCacheManager;
    }

}