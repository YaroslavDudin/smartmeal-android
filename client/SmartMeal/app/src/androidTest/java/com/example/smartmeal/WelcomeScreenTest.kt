package com.example.smartmeal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.smartmeal.ui.screens.auth.WelcomeScreen
import org.junit.Rule
import org.junit.Test
import com.example.smartmeal.ui.theme.SmartMealTheme

class WelcomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_displaysTextButtonImage() {
        composeTestRule.setContent {
            SmartMealTheme {
                WelcomeScreen()
            }
        }
        composeTestRule
            .onNodeWithTag("food_image")
            .performScrollTo()
            .assertExists()

        composeTestRule
            .onNodeWithText("SmartMeal")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Сгенерируйте своё недельное\nменю за пару минут")
            .performScrollTo()
            .assertExists()

        composeTestRule
            .onNodeWithText("Начать")
            .performScrollTo()
            .assertIsDisplayed()
    }
    @Test
    fun welcomeScreen_buttonClick_callsNavigation() {
        var clicked = false
        composeTestRule.setContent {
            SmartMealTheme {
                WelcomeScreen(
                    onNavigateNext = { clicked = true }
                )
            }
        }

        

        assert(clicked)
    }
}