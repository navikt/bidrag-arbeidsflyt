package no.nav.bidrag.arbeidsflyt

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class CacheConfigTest {
    @Test
    fun `should register all configured caches`() {
        val cacheManager = CacheConfig().cacheManager()
        val expectedCacheNames =
            listOf(
                CacheConfig.PERSON_CACHE,
                CacheConfig.GEOGRAFISK_ENHET_CACHE,
                CacheConfig.BBM_SØKNAD_CACHE,
                CacheConfig.SAK_CACHE,
                CacheConfig.JOURNALFORENDE_ENHET_CACHE,
                CacheConfig.ENHET_INFO_CACHE,
                CacheConfig.TILGANG_TEMA_CACHE,
            )

        assertThat(cacheManager.cacheNames).containsExactlyInAnyOrderElementsOf(expectedCacheNames)
        expectedCacheNames.forEach { cacheName ->
            assertThat(cacheManager.getCache(cacheName))
                .describedAs("Expected cache %s to be registered", cacheName)
                .isNotNull
        }
    }
}
