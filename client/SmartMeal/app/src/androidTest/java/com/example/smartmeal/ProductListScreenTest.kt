package com.example.smartmeal

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartmeal.data.api.RetrofitClient
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.presentation.CustomPlan
import com.example.smartmeal.feature.products.presentation.ProductListScreen
import com.example.smartmeal.feature.products.presentation.ProductListViewModel
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class ProductListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun products_hidesMyPlanSection_whenPlanIs7Days() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = SetupPreferences(context)
        val menuApi = RetrofitClient.createService(MenuApi::class.java)
        
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProductListViewModel(menuApi, preferences) as T
            }
        }

        composeTestRule.setContent {
            val vm: ProductListViewModel = viewModel(factory = factory)
            SmartMealTheme {
                ProductListScreen(
                    viewModel = vm,
                    products = emptyList(),
                    selectedDate = null,
                    selectedStartDateKey = null,
                    selectedEndDateKey = null,
                    dateRangeText = "",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> },
                    onReselectPlan = {},
                    customPlan = CustomPlan(
                        startDate = dateFormatter.parse("2099-03-10")!!,
                        endDate = dateFormatter.parse("2099-03-16")!! // 7 дней
                    )
                )
            }
        }

        composeTestRule.onAllNodesWithTag("home_my_plan", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun products_showsMyPlanSection_whenPlanIs8Days() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = SetupPreferences(context)
        val menuApi = RetrofitClient.createService(MenuApi::class.java)
        
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProductListViewModel(menuApi, preferences) as T
            }
        }

        composeTestRule.setContent {
            val vm: ProductListViewModel = viewModel(factory = factory)
            SmartMealTheme {
                ProductListScreen(
                    viewModel = vm,
                    products = emptyList(),
                    selectedDate = null,
                    selectedStartDateKey = null,
                    selectedEndDateKey = null,
                    dateRangeText = "",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> },
                    onReselectPlan = {},
                    customPlan = CustomPlan(
                        startDate = dateFormatter.parse("2099-03-10")!!,
                        endDate = dateFormatter.parse("2099-03-17")!! // 8 дней
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag("home_my_plan", useUnmergedTree = true).assertIsDisplayed()
    }
}
