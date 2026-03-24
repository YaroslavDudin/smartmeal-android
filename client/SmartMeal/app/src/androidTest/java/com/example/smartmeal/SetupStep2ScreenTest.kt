package com.example.smartmeal

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.smartmeal.feature.setup.presentation.PeriodType
import com.example.smartmeal.feature.setup.presentation.SetupState
import com.example.smartmeal.feature.setup.presentation.SetupStep2Content
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class SetupStep2ScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun setupStep2_showsControls_andHandlesClicks() {
        val selectedDateMillis = Calendar.getInstance().apply {
            set(2026, 8, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val state = SetupState(
            periodType = PeriodType.WEEKLY,
            calendarYear = 2026,
            calendarMonth = 8,
            selectedStartDateMillis = selectedDateMillis
        )

        var backClicked = false
        var selectedType: PeriodType? = null

        composeTestRule.setContent {
            SmartMealTheme {
                SetupStep2Content(
                    state = state,
                    onBack = { backClicked = true },
                    onNext = {},
                    onSelectPeriodType = { selectedType = it },
                    onSelectDay = { _, _, _ -> },
                    onPreviousMonth = {},
                    onNextMonth = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("setup_step2_period_daily").assertIsDisplayed()
        composeTestRule.onNodeWithTag("setup_step2_period_weekly").assertIsDisplayed()
        composeTestRule.onNodeWithTag("setup_step2_period_custom").assertIsDisplayed()
        composeTestRule.onNodeWithTag("setup_step2_calendar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("setup_step2_next")
            .assertIsEnabled()
            .assertHasClickAction()

        composeTestRule.onNodeWithTag("setup_step2_back").performClick()
        composeTestRule.onNodeWithTag("setup_step2_period_custom").performClick()

        assertTrue(backClicked)
        assertEquals(PeriodType.CUSTOM, selectedType)
    }
}
