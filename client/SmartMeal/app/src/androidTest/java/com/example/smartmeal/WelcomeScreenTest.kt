package com.example.smartmeal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.smartmeal.ui.screens.auth.WelcomeScreen
import org.junit.Rule
import org.junit.Test
class WelcomeScreenTest {
// Тесты работают с API 36.0 и ниже
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_displaysTextButtonImage() {

        composeTestRule.setContent {
            WelcomeScreen()
        }
        composeTestRule
            .onNodeWithTag("food_image")
            .assertExists()

        composeTestRule
            .onNodeWithText("SmartMeal")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Сгенерируйте своё недельное\nменю за пару минут")
            .assertExists()

        composeTestRule
            .onNodeWithText("Начать")
            .assertIsDisplayed()
    }
    @Test
    fun welcomeScreen_buttonClick_callsNavigation() {

        var clicked = false

        composeTestRule.setContent {
            WelcomeScreen(
                onNavigateNext = { clicked = true }
            )
        }

        composeTestRule
            .onNodeWithText("Начать")
            .performClick()

        assert(clicked)
    }
}