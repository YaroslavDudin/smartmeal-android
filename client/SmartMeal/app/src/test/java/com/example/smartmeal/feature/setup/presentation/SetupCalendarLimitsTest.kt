package com.example.smartmeal.feature.setup.presentation

import java.util.Calendar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupCalendarLimitsTest {

    @Test
    fun dateWithin256Days_isSelectable() {
        val today = createNormalizedCalendar(2026, Calendar.MARCH, 28)
        val maxDate = createMaxSelectableCalendar(today)

        assertTrue(
            isPlanDateSelectable(
                year = maxDate.get(Calendar.YEAR),
                month = maxDate.get(Calendar.MONTH),
                day = maxDate.get(Calendar.DAY_OF_MONTH),
                today = today,
            )
        )
    }

    @Test
    fun dateAfter256Days_isNotSelectable() {
        val today = createNormalizedCalendar(2026, Calendar.MARCH, 28)
        val afterLimit = createMaxSelectableCalendar(today).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }

        assertFalse(
            isPlanDateSelectable(
                year = afterLimit.get(Calendar.YEAR),
                month = afterLimit.get(Calendar.MONTH),
                day = afterLimit.get(Calendar.DAY_OF_MONTH),
                today = today,
            )
        )
    }

    @Test
    fun previousMonthBeforeToday_isNotNavigable() {
        val today = createNormalizedCalendar(2026, Calendar.MARCH, 28)

        assertFalse(
            canNavigateToOffsetMonth(
                currentYear = 2026,
                currentMonth = Calendar.MARCH,
                monthOffset = -1,
                today = today,
            )
        )
    }

    @Test
    fun monthContainingMaxDate_isDisplayable() {
        val today = createNormalizedCalendar(2026, Calendar.MARCH, 28)
        val maxDate = createMaxSelectableCalendar(today)

        assertTrue(
            canDisplayMonthForPlanSelection(
                year = maxDate.get(Calendar.YEAR),
                month = maxDate.get(Calendar.MONTH),
                today = today,
            )
        )
    }

    @Test
    fun monthAfterMaxDate_isNotDisplayable() {
        val today = createNormalizedCalendar(2026, Calendar.MARCH, 28)
        val afterMaxMonth = createMaxSelectableCalendar(today).apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        assertFalse(
            canDisplayMonthForPlanSelection(
                year = afterMaxMonth.get(Calendar.YEAR),
                month = afterMaxMonth.get(Calendar.MONTH),
                today = today,
            )
        )
    }
}
