package pl.tripsync.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object Dates {
    private val polish = Locale.forLanguageTag("pl-PL")
    fun display(value: String): String = runCatching { LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMM yyyy", polish)) }.getOrDefault(value)
    fun day(value: String): String = runCatching { LocalDate.parse(value).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", polish)) }.getOrDefault(value)
    fun range(start: String, end: String): String = "${display(start)} – ${display(end)}"
    fun millis(value: String): Long = LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    fun iso(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
    fun time(hour: Int, minute: Int): String = LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("HH:mm"))
}
