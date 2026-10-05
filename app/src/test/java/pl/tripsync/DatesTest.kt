package pl.tripsync

import org.junit.Assert.*
import org.junit.Test
import pl.tripsync.domain.Dates
import java.util.TimeZone

class DatesTest {
    @Test fun calendarDatesDoNotShiftWithDeviceTimezone() {
        val before=TimeZone.getDefault()
        try {
            listOf("Pacific/Kiritimati","America/Los_Angeles","Europe/Warsaw").forEach {zone ->
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                listOf("2026-03-29","2026-10-25","2024-02-29").forEach {date -> assertEquals(date,Dates.iso(Dates.millis(date))) }
            }
        } finally {TimeZone.setDefault(before)}
    }
    @Test fun datesUsePolishMonthNamesAndHandleOldInvalidValues() {
        assertEquals("5 paź 2026",Dates.display("2026-10-05"))
        assertEquals("poniedziałek, 5 października",Dates.day("2026-10-05"))
        assertEquals("bad",Dates.display("bad"))
    }
    @Test fun clockAlwaysUsesTwentyFourHoursAndLeadingZeros() {
        assertEquals("00:05",Dates.time(0,5))
        assertEquals("23:59",Dates.time(23,59))
    }
}
