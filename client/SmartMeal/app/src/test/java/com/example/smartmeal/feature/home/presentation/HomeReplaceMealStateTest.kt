package com.example.smartmeal.feature.home.presentation

import com.example.smartmeal.feature.home.data.menu.MenuDto
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeReplaceMealStateTest {

    @Test
    fun mergeUpdatedMenuItemIntoState_updatesCurrentMenuAndAllMenuItems() {
        val originalBreakfast = meal(
            id = 1,
            title = "Омлет",
            mealType = "breakfast"
        )
        val originalLunch = meal(
            id = 2,
            title = "Суп",
            mealType = "lunch"
        )
        val updatedBreakfast = originalBreakfast.copy(
            recipe = 11,
            recipe_title = "Сырники",
            cook_time = 25
        )

        val state = HomeUiState(
            currentMenu = MenuDto(
                id = 7,
                period = "week",
                start_date = "2026-03-23",
                created_at = "2026-03-23T10:00:00Z",
                items = listOf(originalBreakfast, originalLunch)
            ),
            allMenuItems = listOf(originalBreakfast, originalLunch)
        )

        val result = mergeUpdatedMenuItemIntoState(state, updatedBreakfast)

        assertEquals("Сырники", result.currentMenu?.items?.first { it.id == 1 }?.recipe_title)
        assertEquals(11, result.currentMenu?.items?.first { it.id == 1 }?.recipe)
        assertEquals("Сырники", result.allMenuItems.first { it.id == 1 }.recipe_title)
        assertEquals("Суп", result.allMenuItems.first { it.id == 2 }.recipe_title)
    }

    private fun meal(
        id: Int,
        title: String,
        mealType: String
    ) = MenuItemDto(
        id = id,
        menu = 7,
        day_offset = 0,
        meal_type = mealType,
        recipe = id,
        recipe_title = title,
        actual_date = "2026-03-23",
        cook_time = 15,
        image_url = null
    )
}
