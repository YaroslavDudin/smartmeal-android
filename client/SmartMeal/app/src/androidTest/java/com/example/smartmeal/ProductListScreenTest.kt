package com.example.smartmeal

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.smartmeal.feature.products.presentation.ProductListScreen
import com.example.smartmeal.feature.products.presentation.ProductUiModel
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun productListScreen_titleIsDisplayed() {
        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = emptyList(),
                    selectedDate = null,
                    selectedStartDateKey = null,
                    selectedEndDateKey = null,
                    dateRangeText = "Выберите диапазон дней",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("title").assertIsDisplayed()
    }

    @Test
    fun productListScreen_showsMonthYearAboveDateSelector() {
        val date = dateFormatter.parse("2026-03-10") ?: Date()

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = emptyList(),
                    selectedDate = date,
                    selectedStartDateKey = null,
                    selectedEndDateKey = null,
                    dateRangeText = "Март 2026",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("products_month_year").assertIsDisplayed()
    }

    @Test
    fun productListScreen_singleAvailableDate_showsFullDateSummary() {
        val selectedDate = dateFormatter.parse("2026-03-27") ?: Date()

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = listOf(
                        ProductUiModel(
                            id = "1",
                            name = "Product 1",
                            amount = "100 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            actualDates = setOf("2026-03-27")
                        )
                    ),
                    selectedDate = selectedDate,
                    selectedStartDateKey = "2026-03-27",
                    selectedEndDateKey = null,
                    dateRangeText = "Пятница, 27 марта 2026",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("products_selected_date_summary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Пятница - 27 марта 2026").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("products_month_year").assertCountEquals(0)
    }

    @Test
    fun productListScreen_showsMonthRangeWhenSelectionSpansMonths() {
        val selectedDate = dateFormatter.parse("2026-03-30") ?: Date()

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = listOf(
                        ProductUiModel(
                            id = "1",
                            name = "Product 1",
                            amount = "100 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            actualDates = setOf("2026-03-30", "2026-04-05")
                        )
                    ),
                    selectedDate = selectedDate,
                    selectedStartDateKey = "2026-03-30",
                    selectedEndDateKey = "2026-04-05",
                    dateRangeText = "30 Марта - 5 Апреля 2026",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("products_month_year").assertIsDisplayed()
    }

    @Test
    fun productListScreen_displaysProductsForSelectedRange() {
        val date = dateFormatter.parse("2026-03-10") ?: Date()
        val visibleName = "Visible product"
        val hiddenName = "Hidden product"

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = listOf(
                        ProductUiModel(
                            id = "1",
                            name = visibleName,
                            amount = "100 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            checked = false,
                            dayOffsets = setOf(1),
                            actualDates = setOf("2026-03-10")
                        ),
                        ProductUiModel(
                            id = "2",
                            name = hiddenName,
                            amount = "200 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            checked = false,
                            dayOffsets = setOf(2),
                            actualDates = setOf("2026-03-11")
                        )
                    ),
                    selectedDate = date,
                    selectedStartDateKey = "2026-03-10",
                    selectedEndDateKey = "2026-03-10",
                    dateRangeText = "10 Марта 2026",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText(visibleName).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(hiddenName).assertCountEquals(0)
    }

    @Test
    fun productListScreen_dayClick_callsOnDateSelected() {
        var selectedDate: String? = null

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = listOf(
                        ProductUiModel(
                            id = "1",
                            name = "Product 1",
                            amount = "100 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            actualDates = setOf("2026-03-10")
                        ),
                        ProductUiModel(
                            id = "2",
                            name = "Product 2",
                            amount = "100 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            actualDates = setOf("2026-03-11")
                        )
                    ),
                    selectedDate = null,
                    selectedStartDateKey = null,
                    selectedEndDateKey = null,
                    dateRangeText = "Март 2026",
                    onDateSelected = { date -> selectedDate = date },
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("date_chip_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals("2026-03-11", selectedDate)
    }

    @Test
    fun productListScreen_checkAllButton_togglesAllVisibleProducts() {
        var checkedIds = emptyList<String>()
        var checkedValue = false

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = listOf(
                        ProductUiModel(
                            id = "1",
                            name = "Product 1",
                            amount = "100 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            checked = false,
                            sourceIds = setOf("1")
                        ),
                        ProductUiModel(
                            id = "2",
                            name = "Product 2",
                            amount = "200 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            checked = false,
                            sourceIds = setOf("2")
                        )
                    ),
                    selectedDate = null,
                    selectedStartDateKey = null,
                    selectedEndDateKey = null,
                    dateRangeText = "Март 2026",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { ids, checked ->
                        checkedIds = ids.toList()
                        checkedValue = checked
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("checkAllButton").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("1", "2"), checkedIds.sorted())
        assertEquals(true, checkedValue)
    }

    @Test
    fun productListScreen_categoriesGroupedCorrectly() {
        val regularCategory = "Vegetables"
        val checkedCategory = "Groceries"

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = listOf(
                        ProductUiModel(
                            id = "1",
                            name = "Salad mix",
                            amount = "200 g",
                            category = "vegetable",
                            icon = "",
                            categoryName = regularCategory,
                            categoryIcon = "",
                            checked = false
                        ),
                        ProductUiModel(
                            id = "2",
                            name = "Buckwheat",
                            amount = "50 g",
                            category = "shopping",
                            icon = "",
                            categoryName = checkedCategory,
                            categoryIcon = "",
                            checked = true
                        )
                    ),
                    selectedDate = null,
                    selectedStartDateKey = null,
                    selectedEndDateKey = null,
                    dateRangeText = "Март 2026",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> }
                )
            }
        }
        composeTestRule.onNodeWithTag("category-$regularCategory").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category-Покупки").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(checkedCategory).performScrollTo().assertIsDisplayed()
    }
}
