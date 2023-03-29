package no.nav.bidrag.arbeidsflyt.utils

import de.jollyday.HolidayCalendar
import de.jollyday.HolidayManager
import de.jollyday.ManagerParameters
import java.time.DayOfWeek
import java.time.LocalDate

class DateUtils {
    companion object {
        fun finnNesteArbeidsdag(): LocalDate {
            return this.finnNesteArbeidsdagEtterDato(LocalDate.now())
        }
        fun finnNesteArbeidsdagEtterDato(fromDate: LocalDate): LocalDate {
            val holidayManager = HolidayManager.getInstance(ManagerParameters.create(HolidayCalendar.NORWAY))
            var candidate = fromDate.plusDays(1)

            while (holidayManager.isHoliday(candidate) ||
                candidate.dayOfWeek == DayOfWeek.SATURDAY ||
                candidate.dayOfWeek == DayOfWeek.SUNDAY
            ) {
                candidate = candidate.plusDays(1)
            }
            return candidate
        }
    }
}
