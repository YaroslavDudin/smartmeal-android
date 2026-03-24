package com.example.smartmeal

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import com.example.smartmeal.feature.home.presentation.HomeContent
import com.example.smartmeal.feature.home.presentation.HomeUiState
import com.example.smartmeal.feature.home.presentation.MealSection
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun home_loading_showsProgress() {
        val state = HomeUiState(isLoading = true)

        composeTestRule.setContent {
            SmartMealTheme {
                HomeContent(
                    uiState = state,
                    onDaySelected = {},
                    onGenerateMenu = {},
                    onReplaceMeal = {},
                    onToggleFavorite = {},
                    onRecipeClick = {},
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
                    onDaySelected = {},
                    onGenerateMenu = { clicked = true },
                    onReplaceMeal = {},
                    onToggleFavorite = {},
                    onRecipeClick = {},
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
            mealSections = sections
        )

        composeTestRule.setContent {
            SmartMealTheme {
                HomeContent(
                    uiState = state,
                    onDaySelected = {},
                    onGenerateMenu = {},
                    onReplaceMeal = {},
                    onToggleFavorite = {},
                    onRecipeClick = {},
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
                    onDaySelected = {},
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
                    onRecipeClick = {},
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
            selectedDay = "Пн"
        )

        composeTestRule.setContent {
            SmartMealTheme {
                HomeContent(
                    uiState = state,
                    onDaySelected = { selected = it },
                    onGenerateMenu = {},
                    onReplaceMeal = {},
                    onToggleFavorite = {},
                    onRecipeClick = {},
                    onDateSelectedFromPlan = {},
                    customPlan = null
                )
            }
        }

        composeTestRule.onNodeWithTag("home_day_selector").assertIsDisplayed()

        composeTestRule.onAllNodes(hasClickAction())[1].performClick()

        assertNotNull(selected)
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
}
