package com.example.smartmeal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.smartmeal.feature.setup.data.models.DietTypeDto
import com.example.smartmeal.feature.setup.presentation.SetupState
import com.example.smartmeal.feature.setup.presentation.SetupStep1Content
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupStep1ScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun setupStep1_showsDietTypes_andHandlesButtons() {
        val state = SetupState(
            dietTypes = listOf(
                DietTypeDto(1, "Кето"),
                DietTypeDto(2, "Вегетарианское")
            ),
            selectedDietTypeId = 1,
            portionSize = 2
        )

        var backClicked = false
        var nextClicked = false
        var incremented = 0
        var decremented = 0
        var selectedId: Int? = null

        composeTestRule.setContent {
            SmartMealTheme {
                SetupStep1Content(
                    state = state,
                    onBack = { backClicked = true },
                    onNext = { nextClicked = true },
                    onDietTypeClick = { selectedId = it },
                    onIncrement = { incremented++ },
                    onDecrement = { decremented++ }
                )
            }
        }

        composeTestRule.onNodeWithTag("setup_step1_diet_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("setup_step1_portion_value").assertIsDisplayed()

        composeTestRule.onNodeWithTag("setup_step1_back").performClick()
        composeTestRule.onNodeWithTag("setup_step1_next").performClick()
        composeTestRule.onNodeWithTag("setup_step1_portion_inc").performClick()
        composeTestRule.onNodeWithTag("setup_step1_portion_dec").performClick()
        composeTestRule.onNodeWithTag("setup_step1_diet_2").performClick()

        assertTrue(backClicked)
        assertTrue(nextClicked)
        assertEquals(1, incremented)
        assertEquals(1, decremented)
        assertEquals(2, selectedId)
    }
}
