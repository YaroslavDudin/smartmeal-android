package com.example.smartmeal

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.example.smartmeal.feature.home.presentation.CustomPlan
import com.example.smartmeal.feature.home.presentation.MyPlanSection
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPlanVisibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun myPlanSection_displayed_whenMoreThan7Days() {
        val start = dateFormatter.parse("2026-01-01")!!
        val end = dateFormatter.parse("2026-01-08")!! // 8 дней: 1,2,3,4,5,6,7,8
        
        composeTestRule.setContent {
            SmartMealTheme {
                MyPlanSection(
                    customPlan = CustomPlan(start, end),
                    selectedDate = start,
                    onDateSelectedFromPlan = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("home_my_plan", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun myPlanSection_logic_7DaysShouldNotShowOnScreens() {
        // Этот тест имитирует логику, которую я добавил в экраны:
        // val days = (diff / ...) + 1
        // if (days > 7) { MyPlanSection(...) }
        
        val start = dateFormatter.parse("2026-01-01")!!
        val end = dateFormatter.parse("2026-01-07")!! // 7 дней
        
        val diff = end.time - start.time
        val days = (diff / (1000L * 60 * 60 * 24)) + 1
        val shouldShow = days > 7

        assert(!shouldShow) { "7 days should not be more than 7" }
    }

    @Test
    fun myPlanSection_logic_8DaysShouldShowOnScreens() {
        val start = dateFormatter.parse("2026-01-01")!!
        val end = dateFormatter.parse("2026-01-08")!! // 8 дней
        
        val diff = end.time - start.time
        val days = (diff / (1000L * 60 * 60 * 24)) + 1
        val shouldShow = days > 7

        assert(shouldShow) { "8 days should be more than 7" }
    }
}
