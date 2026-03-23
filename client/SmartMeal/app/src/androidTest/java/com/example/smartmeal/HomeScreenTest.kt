//package com.example.smartmeal
//
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.ui.test.assertIsDisplayed
//import androidx.compose.ui.test.assertCountEquals
//import androidx.compose.ui.test.assertTextEquals
//import androidx.compose.ui.test.junit4.createComposeRule
//import androidx.compose.ui.test.onAllNodesWithTag
//import androidx.compose.ui.test.onNodeWithTag
//import androidx.compose.ui.test.performClick
//import com.example.smartmeal.feature.home.presentation.HomeScreenContent
//import com.example.smartmeal.feature.home.presentation.HomeUiState
//import com.example.smartmeal.feature.home.presentation.MealItem
//import com.example.smartmeal.feature.home.presentation.MealSection
//import com.example.smartmeal.ui.theme.SmartMealTheme
//import org.junit.Assert.assertEquals
//import org.junit.Assert.assertTrue
//import org.junit.Rule
//import org.junit.Test
//
//class HomeScreenTest {
//    @get:Rule
//    val composeTestRule = createComposeRule()
//
//    @Test
//    fun homeScreen_loading_showsProgress() {
//        val state = HomeUiState(isLoading = true)
//
//        composeTestRule.setContent {
//            SmartMealTheme {
//                HomeScreenContent(
//                    uiState = state,
//                    onDaySelected = {},
//                    onGenerateMenu = {},
//                    onReplaceMeal = {},
//                    onToggleFavorite = {},
//                    onLogout = {},
//                    onLogoutSuccess = {},
//                    onRecipeClick = {}
//                )
//            }
//        }
//
//        composeTestRule.onNodeWithTag("home_loading").assertIsDisplayed()
//        composeTestRule.onAllNodesWithTag("home_empty_state").assertCountEquals(0)
//    }
//
//    @Test
//    fun homeScreen_emptyState_showsGenerateButton_andCallsCallback() {
//        val state = HomeUiState(isLoading = false, hasMenu = false)
//        var generated = false
//
//        composeTestRule.setContent {
//            SmartMealTheme {
//                HomeScreenContent(
//                    uiState = state,
//                    onDaySelected = {},
//                    onGenerateMenu = { generated = true },
//                    onReplaceMeal = {},
//                    onToggleFavorite = {},
//                    onLogout = {},
//                    onLogoutSuccess = {},
//                    onRecipeClick = {}
//                )
//            }
//        }
//
//        composeTestRule.onNodeWithTag("home_empty_state").assertIsDisplayed()
//        composeTestRule.onNodeWithTag("home_generate_button").assertIsDisplayed()
//        composeTestRule.onNodeWithTag("home_generate_button").performClick()
//        assertTrue(generated)
//    }
//
//    @Test
//    fun homeScreen_withMenu_showsMealSections() {
//        val sections = listOf(
//            MealSection(
//                id = "breakfast",
//                title = "Завтрак",
//                meal = MealItem(
//                    id = "1",
//                    title = "Овсянка",
//                    cookTime = "15 мин",
//                    imageRes = com.example.smartmeal.R.drawable.food,
//                    isFavorite = false
//                )
//            ),
//            MealSection(
//                id = "dinner",
//                title = "Ужин",
//                meal = MealItem(
//                    id = "2",
//                    title = "Салат",
//                    cookTime = "10 мин",
//                    imageRes = com.example.smartmeal.R.drawable.food,
//                    isFavorite = false
//                )
//            )
//        )
//
//        val state = HomeUiState(
//            isLoading = false,
//            hasMenu = true,
//            selectedDay = "Пн",
//            selectedDateDisplay = "Понедельник - 10 марта 2026 г.",
//            mealSections = sections
//        )
//
//        composeTestRule.setContent {
//            SmartMealTheme {
//                HomeScreenContent(
//                    uiState = state,
//                    onDaySelected = {},
//                    onGenerateMenu = {},
//                    onReplaceMeal = {},
//                    onToggleFavorite = {},
//                    onLogout = {},
//                    onLogoutSuccess = {},
//                    onRecipeClick = {}
//                )
//            }
//        }
//
//        composeTestRule.onNodeWithTag("home_meal_list").assertIsDisplayed()
//        composeTestRule.onAllNodesWithTag("home_section_breakfast").assertCountEquals(1)
//        composeTestRule.onAllNodesWithTag("home_section_dinner").assertCountEquals(1)
//    }
//
//    @Test
//    fun homeScreen_replaceButton_updatesMealTitle() {
//        val initialSections = listOf(
//            MealSection(
//                id = "breakfast",
//                title = "Завтрак",
//                meal = MealItem(
//                    id = "1",
//                    title = "Овсянка",
//                    cookTime = "15 мин",
//                    imageRes = com.example.smartmeal.R.drawable.food,
//                    isFavorite = false
//                )
//            )
//        )
//
//        val state = mutableStateOf(
//            HomeUiState(
//                isLoading = false,
//                hasMenu = true,
//                selectedDay = "Пн",
//                selectedDateDisplay = "Понедельник - 10 марта 2026 г.",
//                mealSections = initialSections
//            )
//        )
//
//        composeTestRule.setContent {
//            SmartMealTheme {
//                HomeScreenContent(
//                    uiState = state.value,
//                    onDaySelected = {},
//                    onGenerateMenu = {},
//                    onReplaceMeal = { id ->
//                        state.value = state.value.copy(
//                            mealSections = state.value.mealSections.map { section ->
//                                if (section.id == id) {
//                                    section.copy(meal = section.meal.copy(title = "${section.meal.title} (замена)"))
//                                } else {
//                                    section
//                                }
//                            }
//                        )
//                    },
//                    onToggleFavorite = {},
//                    onLogout = {},
//                    onLogoutSuccess = {},
//                    onRecipeClick = {}
//                )
//            }
//        }
//
//        composeTestRule.onNodeWithTag("home_replace_breakfast").performClick()
//        composeTestRule.onNodeWithTag("home_meal_title_1").assertTextEquals("Овсянка (замена)")
//    }
//
//    @Test
//    fun homeScreen_favoriteButton_callsCallback() {
//        val sections = listOf(
//            MealSection(
//                id = "breakfast",
//                title = "Завтрак",
//                meal = MealItem(
//                    id = "1",
//                    title = "Овсянка",
//                    cookTime = "15 мин",
//                    imageRes = com.example.smartmeal.R.drawable.food,
//                    isFavorite = false
//                )
//            )
//        )
//
//        val state = HomeUiState(
//            isLoading = false,
//            hasMenu = true,
//            selectedDay = "Пн",
//            selectedDateDisplay = "Понедельник - 10 марта 2026 г.",
//            mealSections = sections
//        )
//
//        var favoriteId: String? = null
//
//        composeTestRule.setContent {
//            SmartMealTheme {
//                HomeScreenContent(
//                    uiState = state,
//                    onDaySelected = {},
//                    onGenerateMenu = {},
//                    onReplaceMeal = {},
//                    onToggleFavorite = { id -> favoriteId = id },
//                    onLogout = {},
//                    onLogoutSuccess = {},
//                    onRecipeClick = {}
//                )
//            }
//        }
//
//        composeTestRule.onNodeWithTag("home_favorite_1").performClick()
//        assertEquals("1", favoriteId)
//    }
//
//    @Test
//    fun homeScreen_daySelector_callsOnDaySelected() {
//        val state = HomeUiState(
//            isLoading = false,
//            hasMenu = false,
//            selectedDay = "Пн",
//            selectedDateDisplay = ""
//        )
//
//        var selectedDay: String? = null
//
//        composeTestRule.setContent {
//            SmartMealTheme {
//                HomeScreenContent(
//                    uiState = state,
//                    onDaySelected = { selectedDay = it },
//                    onGenerateMenu = {},
//                    onReplaceMeal = {},
//                    onToggleFavorite = {},
//                    onLogout = {},
//                    onLogoutSuccess = {},
//                    onRecipeClick = {}
//                )
//            }
//        }
//
//        composeTestRule.onNodeWithTag("home_day_selector").assertIsDisplayed()
//        composeTestRule.onNodeWithTag("day_chip_1").performClick()
//        assertEquals("Вт", selectedDay)
//    }
//
//    @Test
//    fun homeScreen_logoutButton_callsCallback() {
//        val state = HomeUiState(isLoading = false, hasMenu = false)
//        var loggedOut = false
//
//        composeTestRule.setContent {
//            SmartMealTheme {
//                HomeScreenContent(
//                    uiState = state,
//                    onDaySelected = {},
//                    onGenerateMenu = {},
//                    onReplaceMeal = {},
//                    onToggleFavorite = {},
//                    onLogout = { loggedOut = true },
//                    onLogoutSuccess = {},
//                    onRecipeClick = {}
//                )
//            }
//        }
//
//        composeTestRule.onNodeWithTag("home_logout_button").performClick()
//        assertTrue(loggedOut)
//    }
//}
