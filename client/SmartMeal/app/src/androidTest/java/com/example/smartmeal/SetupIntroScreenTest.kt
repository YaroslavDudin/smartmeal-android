package com.example.smartmeal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.smartmeal.feature.setup.presentation.SetupIntroContent
import com.example.smartmeal.feature.setup.presentation.SetupState
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupIntroScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun setupIntro_loading_showsSpinner() {
        val state = SetupState(isCheckingUser = true)

        composeTestRule.setContent {
            SmartMealTheme {
                SetupIntroContent(
                    state = state,
                    onStartSetup = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("setup_intro_loading").assertIsDisplayed()
    }

    @Test
    fun setupIntro_startButton_callsCallback() {
        val state = SetupState(isCheckingUser = false)
        var started = false

        composeTestRule.setContent {
            SmartMealTheme {
                SetupIntroContent(
                    state = state,
                    onStartSetup = { started = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("setup_intro_start").performClick()
        assertTrue(started)
    }
}
