package com.example.smartmeal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.smartmeal.feature.auth.presentation.WelcomeScreen
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_displaysKeyContent() {
        composeTestRule.setContent {
            SmartMealTheme {
                WelcomeScreen()
            }
        }

        composeTestRule.onNodeWithTag("food_image").assertIsDisplayed()

        composeTestRule.onNodeWithTag("welcome_title").assertIsDisplayed()

        composeTestRule.onNodeWithTag("welcome_subtitle").assertIsDisplayed()

        composeTestRule.onNodeWithTag("welcome_start_button").assertIsDisplayed()
    }

    @Test
    fun welcomeScreen_smallScreen_fitsWithoutScroll() {
        composeTestRule.setContent {
            SmartMealTheme {
                Box(
                    modifier = Modifier
                        .size(320.dp, 480.dp)
                        .clipToBounds()
                ) {
                    WelcomeScreen()
                }
            }
        }

        composeTestRule.onNodeWithTag("food_image").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_subtitle").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_start_button").assertIsDisplayed()
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

        composeTestRule.onNodeWithTag("welcome_start_button").performClick()

        assert(clicked)
    }
}
