package com.example.smartmeal

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.smartmeal.feature.home.data.menu.MenuDto
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import com.example.smartmeal.feature.home.presentation.HomeContent
import com.example.smartmeal.feature.home.presentation.HomeUiState
import com.example.smartmeal.feature.home.presentation.MealSection
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun home_loading_showsProgress() {
        val state = HomeUiState(isLoading = true)

        composeTestRule.setContent {
            SmartMealTheme {
                HomeContent(
                    uiState = state,
                    onDateSelected = {},
                    onGenerateMenu = {},
                    onReplaceMeal = {},
                    onToggleFavorite = {},
                    onRecipeClick = { _, _ -> },
                    onDateSelectedFromPlan = {},
                    customPlan = null
                )
            }
        }

        composeTestRule.onNodeWithTag("home_loading").assertIsDisplayed()
    }

    @Test
    fun home_empty_showsButton_andClickWorks() {
        val state = HomeUiState(isLoading = false, hasMenu = false)

        var clicked = false

        composeTestRule.setContent {
            SmartMealTheme {
                HomeContent(
                    uiState = state,
                    onDateSelected = {},
                    onGenerateMenu = { clicked = true },
                    onReplaceMeal = {},
                    onToggleFavorite = {},
                    onRecipeClick = { _, _ -> },
                    onDateSelectedFromPlan = {},
                    customPlan = null
                )
            }
        }

        composeTestRule.onNodeWithTag("home_empty_state").assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("home_generate_button")
            .performClick()

        assertTrue(clicked)
    }

    @Test
    fun home_withMenu_showsSections() {
        val sections = listOf(
            MealSection(
                id = "breakfast",
                title = "Завтрак",
                meal = fakeMeal("Омлет", "breakfast")
            ),
            MealSection(
                id = "dinner",
                title = "Ужин",
                meal = fakeMeal("Паста", "dinner")
            )
        )

        val state = HomeUiState(
            hasMenu = true,
            mealSections = sections,
            currentMenu = menuWithDates("2026-03-10", "2026-03-11")
        )

        composeTestRule.setContent {
            SmartMealTheme {
                HomeContent(
                    uiState = state,
                    onDateSelected = {},
                    onGenerateMenu = {},
                    onReplaceMeal = {},
                    onToggleFavorite = {},
                    onRecipeClick = { _, _ -> },
                    onDateSelectedFromPlan = {},
                    customPlan = null
                )
            }
        }

        composeTestRule.onNodeWithTag("home_meal_list").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_replace_breakfast").assertExists()
        composeTestRule.onNodeWithTag("home_replace_dinner").assertExists()
    }

    @Test
    fun home_replace_click_changesState() {
        val state = mutableStateOf(
            HomeUiState(
                hasMenu = true,
                currentMenu = menuWithDates("2026-03-10"),
                mealSections = listOf(
                    MealSection(
                        id = "breakfast",
                        title = "Завтрак",
                        meal = fakeMeal("Тест", "breakfast")
                    )
                )
            )
        )

        composeTestRule.setContent {
            SmartMealTheme {
                HomeContent(
                    uiState = state.value,
                    onDateSelected = {},
                    onGenerateMenu = {},
                    onReplaceMeal = { id ->
                        state.value = state.value.copy(
                            mealSections = state.value.mealSections.map {
                                if (it.id == id) {
                                    it.copy(
                                        meal = it.meal.copy(
                                            recipe_title = "Обновлено"
                                        )
                                    )
                                } else it
                            }
                        )
                    },
                    onToggleFavorite = {},
                    onRecipeClick = { _, _ -> },
                    onDateSelectedFromPlan = {},
                    customPlan = null
                )
            }
        }

        composeTestRule
            .onNodeWithTag("home_replace_breakfast")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Обновлено")
            .assertIsDisplayed()
    }

    @Test
    fun home_daySelector_click() {
        var selected: String? = null

        val state = HomeUiState(
            selectedDay = "Пн",
            currentMenu = menuWithDates("2026-03-10", "2026-03-11")
        )

        composeTestRule.setContent {
            SmartMealTheme {
                HomeContent(
                    uiState = state,
                    onDateSelected = { selected = dateFormatter.format(it) },
                    onGenerateMenu = {},
                    onReplaceMeal = {},
                    onToggleFavorite = {},
                    onRecipeClick = { _, _ -> },
                    onDateSelectedFromPlan = {},
                    customPlan = null
                )
            }
        }

        composeTestRule.onNodeWithTag("home_day_selector").assertIsDisplayed()
        composeTestRule.onNodeWithTag("date_chip_1").performClick()

        assertEquals("2026-03-11", selected)
    }

    @Test
    fun home_showsMonthYearAboveDateSelector() {
        val state = HomeUiState(
            currentMenu = menuWithDates("2026-03-10", "2026-03-11")
        )

        composeTestRule.setContent {
            SmartMealTheme {
                HomeContent(
                    uiState = state,
                    onDateSelected = {},
                    onGenerateMenu = {},
                    onReplaceMeal = {},
                    onToggleFavorite = {},
                    onRecipeClick = { _, _ -> },
                    onDateSelectedFromPlan = {},
                    customPlan = null
                )
            }
        }

        composeTestRule.onNodeWithTag("home_month_year").assertIsDisplayed()
        composeTestRule.onNodeWithText("Март 2026").assertIsDisplayed()
    }

    @Test
    fun home_singleAvailableDate_showsFullDateSummary() {
        val state = HomeUiState(
            currentMenu = menuWithDates("2026-03-27"),
            selectedDate = dateFormatter.parse("2026-03-27")
        )

        composeTestRule.setContent {
            SmartMealTheme {
                HomeContent(
                    uiState = state,
                    onDateSelected = {},
                    onGenerateMenu = {},
                    onReplaceMeal = {},
                    onToggleFavorite = {},
                    onRecipeClick = { _, _ -> },
                    onDateSelectedFromPlan = {},
                    customPlan = null
                )
            }
        }

        composeTestRule.onNodeWithTag("home_selected_date_summary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Пятница - 27 марта 2026").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("home_month_year").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("date_chip_0").assertCountEquals(0)
    }

    private fun fakeMeal(
        title: String,
        type: String
    ) = MenuItemDto(
        id = 1,
        recipe = 1,
        recipe_title = title,
        cook_time = 15,
        meal_type = type,
        day_offset = 0,
        actual_date = "2026-03-10"
    )

    private fun menuWithDates(vararg dates: String): MenuDto {
        return MenuDto(
            id = 1,
            period = "week",
            start_date = dates.firstOrNull() ?: "2026-03-10",
            created_at = "2026-03-10T00:00:00Z",
            items = dates.mapIndexed { index, date ->
                MenuItemDto(
                    id = index + 1,
                    recipe = index + 1,
                    recipe_title = "Meal $index",
                    cook_time = 15,
                    meal_type = if (index % 2 == 0) "breakfast" else "dinner",
                    day_offset = index,
                    actual_date = date
                )
            }
        )
    }
}