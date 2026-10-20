package com.shohankhan.hisaabdesk.core

import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.core.formatting.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DatesTest {

    @Test
    fun `addMonths clamps to end of month`() {
        assertEquals(LocalDate.of(2024, 2, 29), Dates.addMonths(LocalDate.of(2024, 1, 31), 1))
        assertEquals(LocalDate.of(2023, 2, 28), Dates.addMonths(LocalDate.of(2023, 1, 31), 1))
        assertEquals(LocalDate.of(2024, 4, 30), Dates.addMonths(LocalDate.of(2024, 3, 31), 1))
    }

    @Test
    fun `monthly schedule keeps end-of-month day`() {
        val dates = Dates.generateDueDates(LocalDate.of(2024, 1, 31), 4, Dates.Frequency.MONTHLY)
        assertEquals(
            listOf(
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2024, 3, 31),
                LocalDate.of(2024, 4, 30)
            ),
            dates
        )
    }

    @Test
    fun `weekly schedule advances seven days`() {
        val dates = Dates.generateDueDates(LocalDate.of(2024, 12, 27), 3, Dates.Frequency.WEEKLY)
        assertEquals(LocalDate.of(2024, 12, 27), dates[0])
        assertEquals(LocalDate.of(2025, 1, 3), dates[1])
        assertEquals(LocalDate.of(2025, 1, 10), dates[2])
    }

    @Test
    fun `custom interval days`() {
        val dates = Dates.generateDueDates(LocalDate.of(2024, 1, 1), 3, Dates.Frequency.CUSTOM_DAYS, intervalDays = 15)
        assertEquals(LocalDate.of(2024, 1, 16), dates[1])
        assertEquals(LocalDate.of(2024, 1, 31), dates[2])
    }

    @Test
    fun `leap year boundary is respected`() {
        val dates = Dates.generateDueDates(LocalDate.of(2024, 2, 29), 2, Dates.Frequency.MONTHLY)
        assertEquals(LocalDate.of(2024, 3, 29), dates[1])
    }

    @Test
    fun `dayKey encoding is stable and sortable`() {
        assertEquals(20240229, Dates.dayKey(LocalDate.of(2024, 2, 29)))
        assertTrue(Dates.dayKey(LocalDate.of(2024, 2, 1)) < Dates.dayKey(LocalDate.of(2024, 2, 2)))
    }

    @Test
    fun `week boundaries start on monday`() {
        val wednesday = LocalDate.of(2024, 5, 15)
        assertEquals(LocalDate.of(2024, 5, 13), Dates.startOfWeek(wednesday))
        assertEquals(LocalDate.of(2024, 5, 19), Dates.endOfWeek(wednesday))
    }

    @Test
    fun `month and year boundaries`() {
        assertEquals(LocalDate.of(2024, 2, 1), Dates.startOfMonth(LocalDate.of(2024, 2, 15)))
        assertEquals(LocalDate.of(2024, 2, 29), Dates.endOfMonth(LocalDate.of(2024, 2, 15)))
        assertEquals(LocalDate.of(2024, 1, 1), Dates.startOfYear(LocalDate.of(2024, 6, 1)))
        assertEquals(LocalDate.of(2024, 12, 31), Dates.endOfYear(LocalDate.of(2024, 6, 1)))
    }

    @Test
    fun `formats dates in bengali digits`() {
        val millis = Dates.millisOf(LocalDate.of(2024, 3, 7))
        assertEquals("০৭/০৩/২০২৪", Dates.format(millis, Money.DigitStyle.BENGALI))
        assertEquals("2024-03-07", Dates.format(millis, Money.DigitStyle.ENGLISH, iso = true))
    }
}
