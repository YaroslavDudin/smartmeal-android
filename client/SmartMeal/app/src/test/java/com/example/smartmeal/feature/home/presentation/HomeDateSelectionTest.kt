package com.example.smartmeal.feature.home.presentation

import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeDateSelectionTest {

    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun resolveDateForSelectedDay_customPlanAcrossMonth_returnsNextMonthDate() {
        val baseDate = formatter.parse("2026-03-30")!!

        val result = resolveDateForSelectedDay(baseDate, "Вс")

        assertEquals("2026-04-05", formatter.format(result!!))
    }

    @Test
    fun resolveDateForSelectedDay_sameWeekday_returnsSameDate() {
        val baseDate = formatter.parse("2026-03-30")!!

        val result = resolveDateForSelectedDay(baseDate, "Пн")

        assertEquals("2026-03-30", formatter.format(result!!))
    }

    @Test
    fun resolveDateForSelectedDay_unknownDay_returnsNull() {
        val baseDate = formatter.parse("2026-03-30")!!

        val result = resolveDateForSelectedDay(baseDate, "???")

        assertNull(result)
    }

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
        val dates = buildAvailableDates(
            menuItems = listOf(
                item("2026-03-25"),
                item("2026-03-30"),
                item("2026-04-01"),
                item("2026-03-25")
            ),
            customPlan = null
        )

        assertEquals(
            listOf("2026-03-25", "2026-03-30", "2026-04-01"),
            dates.map(formatter::format)
        )
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
