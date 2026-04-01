package com.example.smartmeal.feature.statistics.presentation

import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsMealOrderingTest {

    @Test
    fun sortMealsForStatistics_ordersBreakfastLunchDinner() {
        val sorted = sortMealsForStatistics(
            listOf(
                item(id = 3, mealType = "dinner", title = "Dinner"),
                item(id = 1, mealType = "breakfast", title = "Breakfast"),
                item(id = 2, mealType = "lunch", title = "Lunch")
            )
        )

        assertEquals(listOf("breakfast", "lunch", "dinner"), sorted.map { it.meal_type })
    }

    @Test
    fun sortMealsForStatistics_putsUnknownMealsAfterStandardOnes() {
        val sorted = sortMealsForStatistics(
            listOf(
                item(id = 1, mealType = "snack", title = "Snack"),
                item(id = 2, mealType = "breakfast", title = "Breakfast")
            )
        )

        assertEquals(listOf("breakfast", "snack"), sorted.map { it.meal_type })
    }

    private fun item(id: Int, mealType: String, title: String) = MenuItemDto(
        id = id,
        day_offset = 0,
        meal_type = mealType,
        recipe = id,
        recipe_title = title,
        actual_date = "2026-04-01",
        cook_time = 15,
        image_url = null
    )
}
