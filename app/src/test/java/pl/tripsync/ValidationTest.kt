package pl.tripsync

import org.junit.Assert.*
import org.junit.Test
import pl.tripsync.domain.*

class ValidationTest {
    private val trip=TripData("Tatry","Zakopane","2026-10-10","2026-10-13")
    @Test fun acceptsValidTripAndEvent() {Validation.trip(trip);Validation.event(EventData("Spacer","2026-10-11","09:30"),trip)}
    @Test fun rejectsReversedDates() {assertThrows(IllegalArgumentException::class.java) {Validation.trip(trip.copy(endDate="2026-10-09"))}}
    @Test fun rejectsEventOutsideTrip() {assertThrows(IllegalArgumentException::class.java) {Validation.event(EventData("Spacer","2026-10-14","09:30"),trip)}}
    @Test fun rejectsInvalidDatesAndHours() {
        listOf("2026-02-30","10/10/2026","").forEach {assertThrows(IllegalArgumentException::class.java) {Validation.date(it)}}
        listOf("24:00","12:60","9:00","").forEach {assertThrows(IllegalArgumentException::class.java) {Validation.time(it)}}
    }
    @Test fun rejectsEmptyAndOverlongTitles() {listOf(" ","a".repeat(121)).forEach {assertThrows(IllegalArgumentException::class.java) {Validation.title(it)}}}
}
