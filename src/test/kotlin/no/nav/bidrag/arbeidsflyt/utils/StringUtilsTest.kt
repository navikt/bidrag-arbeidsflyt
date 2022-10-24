package no.nav.bidrag.arbeidsflyt.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class StringUtilsTest {

    @Test
    fun shouldRemoveNonNumericCharacters(){
        assertThat("asda123asda".numericOnly()).isEqualTo("123")
        assertThat("1232132!".numericOnly()).isEqualTo("1232132")
        assertThat("123213 213213".numericOnly()).isEqualTo("123213213213")
        assertThat("! !??. ++´´´----§§§!'''!!.,,--4444A 5555@*)(/\\".numericOnly()).isEqualTo("44445555")
        assertThat("123 123 123".numericOnly()).isEqualTo("123123123")
    }
}