package com.example.smartmeal

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
                    selectedStartDayIndex = null,
                    selectedEndDayIndex = null,
                    dateRangeText = "Выберите диапазон дней",
                    onDaySelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("title").assertIsDisplayed()
    }

    @Test
    fun productListScreen_showsSelectedDate() {
        val date = dateFormatter.parse("2026-03-10") ?: Date()

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = emptyList(),
                    selectedDate = date,
                    selectedStartDayIndex = null,
                    selectedEndDayIndex = null,
                    dateRangeText = "Пн-Вт",
                    onDaySelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("selectedDateText").assertIsDisplayed()
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
                    selectedStartDayIndex = 1,
                    selectedEndDayIndex = 1,
                    dateRangeText = "Вт",
                    onDaySelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText(visibleName).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(hiddenName).assertCountEquals(0)
    }

    @Test
    fun productListScreen_dayClick_callsOnDaySelected() {
        var selectedDay: String? = null

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = emptyList(),
                    selectedDate = null,
                    selectedStartDayIndex = null,
                    selectedEndDayIndex = null,
                    dateRangeText = "Выберите диапазон дней",
                    onDaySelected = { day -> selectedDay = day },
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("day_chip_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Вт", selectedDay)
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
                    selectedStartDayIndex = null,
                    selectedEndDayIndex = null,
                    dateRangeText = "Выберите диапазон дней",
                    onDaySelected = {},
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
        val checkedCategory = "Покупки"

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
                            categoryName = "Groceries",
                            categoryIcon = "",
                            checked = true
                        )
                    ),
                    selectedDate = null,
                    selectedStartDayIndex = null,
                    selectedEndDayIndex = null,
                    dateRangeText = "Выберите диапазон дней",
                    onDaySelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("category-$regularCategory").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category-$checkedCategory").assertIsDisplayed()
    }
}
