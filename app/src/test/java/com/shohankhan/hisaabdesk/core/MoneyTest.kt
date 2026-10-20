package com.shohankhan.hisaabdesk.core

import com.shohankhan.hisaabdesk.core.formatting.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun `formats with bengali digits and asian grouping`() {
        assertEquals("৳১,২৫০.০০", Money.format(125000))
        assertEquals("৳১২,৫০,০০০.০০", Money.format(12_500_000_00))
        assertEquals("৳১,০০,০০,০০০.০০", Money.format(10_000_000_00))
    }

    @Test
    fun `formats with english digits when requested`() {
        assertEquals("৳1,250.00", Money.format(125000, Money.DigitStyle.ENGLISH))
    }

    @Test
    fun `formats negative amounts with sign before symbol`() {
        assertEquals("-৳১২৩.৪৫", Money.format(-12345))
    }

    @Test
    fun `western grouping places comma every three digits`() {
        assertEquals("৳1,250,000.00", Money.format(125_000_000, Money.DigitStyle.ENGLISH, Money.Grouping.WESTERN))
    }

    @Test
    fun `parses bengali digits with separators`() {
        assertEquals(125050L, Money.parsePaisa("১,২৫০.৫০"))
        assertEquals(125000L, Money.parsePaisa("৳1250"))
        assertEquals(100L, Money.parsePaisa("1"))
    }

    @Test
    fun `rejects malformed input`() {
        assertNull(Money.parsePaisa(""))
        assertNull(Money.parsePaisa("abc"))
        assertNull(Money.parsePaisa("12.345"))
        assertNull(Money.parsePaisa("--5"))
    }

    @Test
    fun `taka to paisa conversion`() {
        assertEquals(500L, Money.takaToPaisa(5))
        assertEquals(12_345L, Money.takaToPaisa(123) + 45)
    }

    @Test
    fun `percentOf rounds half up exactly`() {
        assertEquals(51L, Money.percentOf(101, 50.0))
        assertEquals(7500L, Money.percentOf(10000, 75.0))
        assertEquals(1L, Money.percentOf(1, 50.0)) // 0.5 rounds half-up to 1
        assertEquals(0L, Money.percentOf(1, 49.0))
    }

    @Test
    fun `safe arithmetic rejects overflow beyond bound`() {
        var threw = false
        try {
            Money.safeAdd(Money.MAX_ABS_PAISA, 1)
        } catch (e: Exception) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `bengali digit round trip`() {
        val text = "১২৩৪৫৬৭৮৯০"
        assertEquals(text, Money.toBengaliDigits("1234567890"))
        assertEquals("1234567890", Money.toEnglishDigits(text))
    }
}
