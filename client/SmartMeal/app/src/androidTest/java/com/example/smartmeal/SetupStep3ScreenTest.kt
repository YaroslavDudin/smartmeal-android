package com.example.smartmeal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.smartmeal.feature.setup.data.models.AllergyDto
import com.example.smartmeal.feature.setup.presentation.SetupState
import com.example.smartmeal.feature.setup.presentation.SetupStep3Content
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupStep3ScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun setupStep3_showsOptions_andHandlesClicks() {
        val state = SetupState(
            allergies = listOf(
                AllergyDto(1, "Орехи"),
                AllergyDto(2, "Рыба")
            )
        )

        var backClicked = false
        var submitClicked = false
        var toggledAllergyId: Int? = null
        var eatAllValue: Boolean? = null
        var cookPref: String? = null

        composeTestRule.setContent {
            SmartMealTheme {
                SetupStep3Content(
                    state = state,
                    onBack = { backClicked = true },
                    onSubmit = { submitClicked = true },
                    onToggleAllergy = { toggledAllergyId = it },
                    onSetEatAll = { eatAllValue = it },
                    onSelectCookTime = { cookPref = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("setup_step3_allergy_grid").assertIsDisplayed()
        composeTestRule.onNodeWithTag("setup_step3_allergy_1").performClick()
        composeTestRule.onNodeWithTag("setup_step3_allergy_all").performClick()
        composeTestRule.onNodeWithTag("setup_step3_cook_under30").performClick()
        composeTestRule.onNodeWithTag("setup_step3_submit").performClick()
        composeTestRule.onNodeWithTag("setup_step3_back").performClick()

        assertEquals(1, toggledAllergyId)
        assertEquals(true, eatAllValue)
        assertEquals("under30", cookPref)
        assertTrue(submitClicked)
        assertTrue(backClicked)
    }
}
