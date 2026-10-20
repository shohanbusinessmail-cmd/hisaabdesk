package com.shohankhan.hisaabdesk.core.formatting

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Date policy for HisaabDesk:
 * - Date-only fields are stored as epoch milliseconds at local midnight of the chosen date.
 * - Day-level filters use [dayKey] (yyyyMMdd as Int) for index-friendly range queries.
 * - Month arithmetic is calendar aware (LocalDate.plusMonths clamps to end-of-month),
 *   so Jan 31 + 1 month = Feb 28/29 — never a naive "+30 days".
 */
object Dates {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun todayMillis(): Long = atStartOfDay(System.currentTimeMillis())

    fun atStartOfDay(epochMillis: Long): Long =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    fun localDateOf(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun millisOf(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun dayKey(epochMillis: Long): Int = dayKey(localDateOf(epochMillis))

    fun dayKey(date: LocalDate): Int =
        date.year * 10000 + date.monthValue * 100 + date.dayOfMonth

    fun dayKeyRange(from: LocalDate, to: LocalDate): IntRange = dayKey(from)..dayKey(to)

    fun dayKeys(fromMillis: Long, toMillis: Long): IntRange =
        dayKey(localDateOf(fromMillis))..dayKey(localDateOf(toMillis))

    /** Calendar-aware month addition with end-of-month clamping. */
    fun addMonths(date: LocalDate, months: Long): LocalDate = date.plusMonths(months)

    fun addWeeks(date: LocalDate, weeks: Long): LocalDate = date.plusWeeks(weeks)

    fun addDays(date: LocalDate, days: Long): LocalDate = date.plusDays(days)

    /**
     * Generates due dates: start, then frequency applied N times.
     * MONTHLY uses calendar months (end-of-month safe); WEEKLY uses 7-day steps;
     * CUSTOM_DAYS uses an explicit day interval.
     */
    enum class Frequency { WEEKLY, MONTHLY, CUSTOM_DAYS }

    fun generateDueDates(start: LocalDate, count: Int, frequency: Frequency, intervalDays: Int = 0): List<LocalDate> {
        require(count in 0..10000) { "invalid count" }
        val result = ArrayList<LocalDate>(count)
        var current = start
        for (i in 0 until count) {
            result.add(current)
            current = when (frequency) {
                Frequency.MONTHLY -> addMonths(current, 1)
                Frequency.WEEKLY -> addWeeks(current, 1)
                Frequency.CUSTOM_DAYS -> {
                    require(intervalDays > 0) { "intervalDays must be > 0" }
                    addDays(current, intervalDays.toLong())
                }
            }
        }
        return result
    }

    fun startOfWeek(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

    fun endOfWeek(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY).plusDays(6)

    fun startOfMonth(date: LocalDate): LocalDate = date.withDayOfMonth(1)

    fun endOfMonth(date: LocalDate): LocalDate = date.withDayOfMonth(date.lengthOfMonth())

    fun startOfYear(date: LocalDate): LocalDate = date.withDayOfYear(1)

    fun endOfYear(date: LocalDate): LocalDate = date.withDayOfYear(date.lengthOfYear())

    fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

    fun format(epochMillis: Long, style: Money.DigitStyle, iso: Boolean = false): String {
        val d = localDateOf(epochMillis)
        val pattern = if (iso) "yyyy-MM-dd" else "dd/MM/yyyy"
        val s = d.format(DateTimeFormatter.ofPattern(pattern))
        return if (style == Money.DigitStyle.BENGALI) Money.toBengaliDigits(s) else s
    }

    fun format(date: LocalDate, style: Money.DigitStyle, iso: Boolean = false): String {
        val pattern = if (iso) "yyyy-MM-dd" else "dd/MM/yyyy"
        val s = date.format(DateTimeFormatter.ofPattern(pattern))
        return if (style == Money.DigitStyle.BENGALI) Money.toBengaliDigits(s) else s
    }

    fun formatDayName(date: LocalDate, style: Money.DigitStyle): String {
        val name = when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> "শনিবার"
            DayOfWeek.SUNDAY -> "রবিবার"
            DayOfWeek.MONDAY -> "সোমবার"
            DayOfWeek.TUESDAY -> "মঙ্গলবার"
            DayOfWeek.WEDNESDAY -> "বুধবার"
            DayOfWeek.THURSDAY -> "বৃহস্পতিবার"
            DayOfWeek.FRIDAY -> "শুক্রবার"
        }
        return name
    }

    fun relativeLabel(epochMillis: Long): String {
        val today = LocalDate.now()
        val d = localDateOf(epochMillis)
        return when (d) {
            today -> "আজ"
            today.minusDays(1) -> "গতকাল"
            today.plusDays(1) -> "আগামীকাল"
            else -> format(d, Money.DigitStyle.BENGALI)
        }
    }
}
