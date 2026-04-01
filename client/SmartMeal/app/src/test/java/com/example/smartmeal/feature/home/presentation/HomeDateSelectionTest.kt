package com.example.smartmeal.feature.home.presentation

import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDateSelectionTest {

    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun resolveGenerationStartDateString_usesSelectedPlanDate() {
        val selectedDate = formatter.parse("2026-03-30")!!
        val fallbackDate = formatter.parse("2026-03-24")!!

        val result = resolveGenerationStartDateString(
            formatter = formatter,
            selectedPlanDate = selectedDate,
            fallbackDate = fallbackDate
        )

        assertEquals("2026-03-30", result)
    }

    @Test
    fun buildAvailableDates_returnsSequentialDatesAcrossMonthBoundary() {
        val today = formatter.parse("2026-03-24")!!
        val dates = buildAvailableDates(
            menuItems = listOf(
                item("2026-03-25"),
                item("2026-03-30"),
                item("2026-04-01"),
                item("2026-03-25")
            ),
            customPlan = null,
            today = today
        )

        assertEquals(
            listOf("2026-03-25", "2026-03-30", "2026-04-01"),
            dates.map(formatter::format)
        )
    }

    @Test
    fun buildAvailableDates_filtersPastDatesBeforeToday() {
        val today = formatter.parse("2026-03-29")!!

        val dates = buildAvailableDates(
            menuItems = listOf(
                item("2026-03-27"),
                item("2026-03-29"),
                item("2026-03-30")
            ),
            customPlan = null,
            today = today
        )

        assertEquals(
            listOf("2026-03-29", "2026-03-30"),
            dates.map(formatter::format)
        )
    }

    @Test
    fun buildAvailableDates_customPlan_canSpanMoreThanSingleMonth() {
        val plan = CustomPlan(
            startDate = formatter.parse("2026-04-01")!!,
            endDate = formatter.parse("2026-05-10")!!
        )

        val dates = buildAvailableDates(
            menuItems = emptyList(),
            customPlan = plan,
            today = formatter.parse("2026-04-01")!!
        )

        assertEquals(40, dates.size)
        assertEquals("2026-04-01", formatter.format(dates.first()))
        assertEquals("2026-05-10", formatter.format(dates.last()))
    }

    @Test
    fun trimCustomPlanToToday_movesStartForwardToToday() {
        val plan = CustomPlan(
            startDate = formatter.parse("2026-03-25")!!,
            endDate = formatter.parse("2026-03-31")!!
        )

        val trimmed = trimCustomPlanToToday(plan, formatter.parse("2026-03-29")!!)

        assertEquals("2026-03-29", formatter.format(trimmed!!.startDate))
        assertEquals("2026-03-31", formatter.format(trimmed.endDate))
    }

    @Test
    fun resolveCustomDays_countsInclusiveRange() {
        val range = formatter.parse("2026-03-29")!!.time to formatter.parse("2026-04-02")!!.time

        val days = resolveCustomDays(range)

        assertEquals(5, days)
    }

    private fun item(actualDate: String) = MenuItemDto(
        id = 1,
        day_offset = 0,
        meal_type = "breakfast",
        recipe = 1,
        recipe_title = "Test",
        actual_date = actualDate,
        cook_time = 15
    )
}
