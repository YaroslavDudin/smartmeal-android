package com.example.smartmeal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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
                    onDaySelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Список продуктов").assertIsDisplayed()
    }

    @Test
    fun productListScreen_showsSelectedDate() {
        val date = dateFormatter.parse("2026-03-10") ?: Date()
        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = emptyList(),
                    selectedDate = date,
                    onDaySelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = {}
                )
            }
        }
        // Проверяем, что текст содержит "10 марта" (часть даты)
        composeTestRule.onNode(hasText("10 марта", substring = true)).assertIsDisplayed()
    }

    @Test
    fun productListScreen_displaysProductsForSelectedDate() {
        val date = dateFormatter.parse("2026-03-10") ?: Date()
        val products = listOf(
            ProductUiModel(
                id = "1",
                name = "Продукт 1",
                amount = "100 г",
                category = "cat",
                icon = "",
                categoryName = "Категория",
                categoryIcon = "",
                checked = false,
                actualDates = setOf("2026-03-10")
            ),
            ProductUiModel(
                id = "2",
                name = "Продукт 2",
                amount = "200 г",
                category = "cat",
                icon = "",
                categoryName = "Категория",
                categoryIcon = "",
                checked = false,
                actualDates = setOf("2026-03-11")
            )
        )
        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = products,
                    selectedDate = date,
                    onDaySelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Продукт 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Продукт 2").assertDoesNotExist()
    }

    @Test
    fun productListScreen_dayClick_callsOnDaySelected() {
        var selectedDay: String? = null
        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = emptyList(),
                    selectedDate = null,
                    onDaySelected = { day -> selectedDay = day },
                    onProductChecked = { _, _ -> },
                    onCheckAll = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Вт").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Вт", selectedDay)
    }

    @Test
    fun productListScreen_checkAllButton_togglesAll() {
        var allChecked = false
        val products = listOf(
            ProductUiModel(
                id = "1",
                name = "Продукт 1",
                amount = "100 г",
                category = "cat",
                icon = "",
                categoryName = "Категория",
                categoryIcon = "",
                checked = false
            ),
            ProductUiModel(
                id = "2",
                name = "Продукт 2",
                amount = "200 г",
                category = "cat",
                icon = "",
                categoryName = "Категория",
                categoryIcon = "",
                checked = false
            )
        )
        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = products,
                    selectedDate = null,
                    onDaySelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { allChecked = true }
                )
            }
        }
        composeTestRule.onNodeWithTag("checkAllButton").performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, allChecked)
    }

    @Test
    fun productListScreen_categoriesGroupedCorrectly() {
        val products = listOf(
            ProductUiModel(
                id = "1",
                name = "Микс салатов",
                amount = "200 г",
                category = "vegetable",
                icon = "🥗",
                categoryName = "Овощи и фрукты",
                categoryIcon = "🥬",
                checked = false
            ),
            ProductUiModel(
                id = "2",
                name = "Гречка",
                amount = "50 г",
                category = "shopping",
                icon = "🛒",
                categoryName = "Покупки",
                categoryIcon = "🛒",
                checked = true
            )
        )
        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = products,
                    selectedDate = null,
                    onDaySelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("category-Овощи и фрукты").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category-Покупки").assertIsDisplayed()
    }
}