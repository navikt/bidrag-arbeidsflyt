package no.nav.bidrag.arbeidsflyt.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DateUtilsTest {

    var SATURDAY = "2021-10-16"
    var SUNDAY = "2021-10-17"
    var MONDAY = "2021-10-18"
    var TUESDAY = "2021-10-19"
    var DAY_BEFORE_EASTER = "2021-03-31"
    var WORKDAY_AFTER_EASTER = "2021-04-06"
    var NORWEGIAN_HOLIDAY = "2021-05-17"

    @Test
    fun `should return next workday from saturday`(){
        val candidate = LocalDate.parse(SATURDAY)
        val result = DateUtils.finnNesteArbeidsdagEtterDato(candidate)
        assertThat(result.toString()).isEqualTo(MONDAY)
    }

    @Test
    fun `should return next workday from sunday`(){
        val candidate = LocalDate.parse(SUNDAY)
        val result = DateUtils.finnNesteArbeidsdagEtterDato(candidate)
        assertThat(result.toString()).isEqualTo(MONDAY)
    }

    @Test
    fun `should return next workday from a workday`(){
        val candidate = LocalDate.parse(TUESDAY)
        val result = DateUtils.finnNesteArbeidsdagEtterDato(candidate)
        assertThat(result.toString()).isEqualTo(candidate.plusDays(1).toString());
    }

    @Test
    fun `should return next workday after easter`(){
        val candidate = LocalDate.parse(DAY_BEFORE_EASTER)
        val result = DateUtils.finnNesteArbeidsdagEtterDato(candidate)
        assertThat(result.toString()).isEqualTo(WORKDAY_AFTER_EASTER)
    }

    @Test
    fun `should skip norwegian holiday as working day`(){
        val norwegianHoliday = LocalDate.parse(NORWEGIAN_HOLIDAY)
        val candidate = norwegianHoliday.minusDays(1)
        val result = DateUtils.finnNesteArbeidsdagEtterDato(candidate)
        assertThat(result.toString()).isEqualTo(norwegianHoliday.plusDays(1).toString())
    }
}