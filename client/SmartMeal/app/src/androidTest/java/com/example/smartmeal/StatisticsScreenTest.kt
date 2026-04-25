package com.example.smartmeal

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.statistics.presentation.StatisticsScreen
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class StatisticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var preferences: SetupPreferences

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        preferences = SetupPreferences(context)
        preferences.clearAll() 
        preferences.setActiveUserKey("test_user") // Важно для scopedKey
    }

    @Test
    fun statistics_hidesMyPlanSection_whenPlanIs7Days() {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance().apply { add(Calendar.DATE, 6) } // 7 дней (сегодня + 6)
        
        preferences.setPlanType(SetupPreferences.PLAN_TYPE_CUSTOM)
        preferences.setCustomPlanRange(start.timeInMillis, end.timeInMillis)

        composeTestRule.setContent {
            SmartMealTheme {
                StatisticsScreen(preferences = preferences)
            }
        }

        composeTestRule.onAllNodesWithTag("home_my_plan", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun statistics_showsMyPlanSection_whenPlanIs8Days() {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance().apply { add(Calendar.DATE, 7) } // 8 дней (сегодня + 7)
        
        preferences.setPlanType(SetupPreferences.PLAN_TYPE_CUSTOM)
        preferences.setCustomPlanRange(start.timeInMillis, end.timeInMillis)

        composeTestRule.setContent {
            SmartMealTheme {
                StatisticsScreen(preferences = preferences)
            }
        }

        composeTestRule.onNodeWithTag("home_my_plan", useUnmergedTree = true).assertIsDisplayed()
    }
}
