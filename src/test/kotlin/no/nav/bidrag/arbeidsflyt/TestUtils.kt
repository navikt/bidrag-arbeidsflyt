package no.nav.bidrag.arbeidsflyt

import org.mockito.Mockito.mockStatic
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class TestUtils {

    fun mockTime() {
        val instantExpected = "2014-12-22T10:15:30Z"
        val clock: Clock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"))
        val instant = Instant.now(clock)

        mockStatic(Instant::class.java).use { mockedStatic ->
            mockedStatic.`when`<Any> { Instant.now() }.thenReturn(instant)
            val now = Instant.now()
        }
    }
}
