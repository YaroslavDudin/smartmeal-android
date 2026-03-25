package com.example.smartmeal.feature.products.presentation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class ProductMonthYearFormattingTest {

    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun formatMonthYearRangeForSelector_returnsSingleMonthWhenDatesInSameMonth() {
        val start = formatter.parse("2026-03-10")!!
        val end = formatter.parse("2026-03-25")!!

        assertEquals("Март 2026", formatMonthYearRangeForSelector(start, end))
    }

    @Test
    fun formatMonthYearRangeForSelector_returnsMonthRangeWhenDatesSpanMonths() {
        val start = formatter.parse("2026-03-30")!!
        val end = formatter.parse("2026-04-05")!!

        assertEquals("Март - Апрель 2026", formatMonthYearRangeForSelector(start, end))
    }

    @Test
    fun formatMonthYearRangeForSelector_returnsFullRangeWhenDatesSpanYears() {
        val start = formatter.parse("2026-12-30")!!
        val end = formatter.parse("2027-01-05")!!

        assertEquals("Декабрь 2026 - Январь 2027", formatMonthYearRangeForSelector(start, end))
    }
}
