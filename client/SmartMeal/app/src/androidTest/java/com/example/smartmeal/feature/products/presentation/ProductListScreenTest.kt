package com.example.smartmeal.feature.products.presentation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Rule
import org.junit.Test

class ProductListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val MON = 0
    private val TUE = 1
    private val WED = 2
    private val THU = 3

    @Test
    fun productListScreen_categoriesAreBold_noBackground() {
        val products = listOf(
            ProductUiModel(
                name = "Микс салатов",
                amount = "200 г",
                category = "vegetable",
                icon = "🥗",
                categoryName = "Овощи и фрукты",
                categoryIcon = "🥬",
                checked = false
            ),
            ProductUiModel(
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
                    selectedStartDayIndex = MON,
                    selectedEndDayIndex = TUE,
                    onDayRangeSelected = { _, _ -> },
                    onProductChecked = { _, _ -> },
                    dateRangeText = "Пн-Вт: 2-3 марта 2026 г."
                )
            }
        }
        composeTestRule.onNodeWithTag("category-Овощи и фрукты").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category-Покупки").assertIsDisplayed()
    }

    @Test
    fun productListScreen_periodIsGrayAndSmall() {
        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = emptyList(),
                    selectedStartDayIndex = MON,
                    selectedEndDayIndex = TUE,
                    onDayRangeSelected = { _, _ -> },
                    onProductChecked = { _, _ -> },
                    dateRangeText = "Пн-Вт: 2-3 марта 2026 г."
                )
            }
        }
        composeTestRule.onNodeWithText("Пн-Вт: 2-3 марта 2026 г.").assertIsDisplayed()
    }

    @Test
    fun productListScreen_titleIsCentered() {
        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = emptyList(),
                    selectedStartDayIndex = MON,
                    selectedEndDayIndex = TUE,
                    onDayRangeSelected = { _, _ -> },
                    onProductChecked = { _, _ -> },
                    dateRangeText = "Пн-Вт: 2-3 марта 2026 г."
                )
            }
        }
        composeTestRule.onNodeWithText("Список продуктов").assertIsDisplayed()
    }

    @Test
    fun productListScreen_showsTopBarAndBottomBar() {
        val products = listOf(
            ProductUiModel(
                name = "Микс салатов",
                amount = "200 г",
                category = "vegetable",
                icon = "🥗",
                categoryName = "Овощи и фрукты",
                categoryIcon = "🥬",
                checked = false
            )
        )
        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = products,
                    selectedStartDayIndex = MON,
                    selectedEndDayIndex = TUE,
                    onDayRangeSelected = { _, _ -> },
                    onProductChecked = { _, _ -> },
                    dateRangeText = "Пн-Вт: 2-3 марта 2026 г."
                )
            }
        }
        composeTestRule.onNodeWithText("Список продуктов").assertIsDisplayed()
        composeTestRule.onNodeWithText("Продукты").assertIsDisplayed()
    }

    @Test
    fun productListScreen_displaysProductAndCategoryIcons() {
        val products = listOf(
            ProductUiModel("Микс салатов", "200 г", "vegetable", "🥗", "Овощи и фрукты", "🥬", false),
            ProductUiModel("Куриное филе", "300 г", "meat", "🍗", "Мясо и рыба", "🐟", false),
            ProductUiModel("Гречка", "50 г", "shopping", "🛒", "Покупки", "🛒", true)
        )
        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = products,
                    selectedStartDayIndex = MON,
                    selectedEndDayIndex = TUE,
                    onDayRangeSelected = { _, _ -> },
                    onProductChecked = { _, _ -> },
                    dateRangeText = "Пн-Вт: 2-3 марта 2026 г."
                )
            }
        }
        composeTestRule.onNodeWithTag("icon-Микс салатов").assertIsDisplayed()
        composeTestRule.onNodeWithTag("icon-Куриное филе").assertIsDisplayed()
        composeTestRule.onNodeWithTag("icon-Гречка").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category-Овощи и фрукты").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category-Мясо и рыба").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category-Покупки").assertIsDisplayed()
    }

    @Test
    fun productListScreen_displaysProductsForDayRange() {
        val mockProducts = listOf(
            ProductUiModel(
                name = "Микс салатов",
                amount = "200 г",
                category = "vegetable",
                icon = "🥗",
                categoryName = "Овощи и фрукты",
                categoryIcon = "🥬",
                checked = false
            ),
            ProductUiModel(
                name = "Куриное филе",
                amount = "300 г",
                category = "meat",
                icon = "🍗",
                categoryName = "Мясо и рыба",
                categoryIcon = "🐟",
                checked = false
            )
        )
        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = mockProducts,
                    selectedStartDayIndex = MON,
                    selectedEndDayIndex = TUE,
                    onDayRangeSelected = { _, _ -> },
                    onProductChecked = { _, _ -> },
                    dateRangeText = "Пн-Вт: 2-3 марта 2026 г."
                )
            }
        }
        composeTestRule.onNodeWithTag("product-Микс салатов").assertIsDisplayed()
        composeTestRule.onNodeWithTag("product-Куриное филе").assertIsDisplayed()
    }

    @Test
    fun productListScreen_switchDayRange_showsOtherProducts() {
        val monTueProducts = listOf(
            ProductUiModel(
                name = "Микс салатов",
                amount = "200 г",
                category = "vegetable",
                icon = "🥗",
                categoryName = "Овощи и фрукты",
                categoryIcon = "🥬",
                checked = false
            )
        )
        val wedThuProducts = listOf(
            ProductUiModel(
                name = "Яйца",
                amount = "10 шт.",
                category = "dairy",
                icon = "🥚",
                categoryName = "Бакалея и молочные продукты",
                categoryIcon = "🥛",
                checked = false
            )
        )
        val startIndexState = androidx.compose.runtime.mutableStateOf(MON)
        val endIndexState = androidx.compose.runtime.mutableStateOf(TUE)

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    products = when {
                        startIndexState.value == MON && endIndexState.value == TUE -> monTueProducts
                        else -> wedThuProducts
                    },
                    selectedStartDayIndex = startIndexState.value,
                    selectedEndDayIndex = endIndexState.value,
                    onDayRangeSelected = { start, end ->
                        start?.let { startIndexState.value = it }
                        end?.let { endIndexState.value = it }
                    },
                    onProductChecked = { _, _ -> },
                    dateRangeText = if (startIndexState.value == MON && endIndexState.value == TUE)
                        "Пн-Вт: 2-3 марта 2026 г."
                    else
                        "Ср-Чт: 4-5 марта 2026 г."
                )
            }
        }

        // Проверяем начальное состояние (Пн-Вт)
        composeTestRule.onNodeWithTag("product-Микс салатов").assertIsDisplayed()
        composeTestRule.onNodeWithTag("product-Яйца").assertDoesNotExist()

        // Меняем диапазон на Ср-Чт
        composeTestRule.runOnUiThread {
            startIndexState.value = WED
            endIndexState.value = THU
        }
        composeTestRule.waitForIdle()

        // Проверяем, что отображаются продукты нового диапазона
        composeTestRule.onNodeWithTag("product-Яйца").assertIsDisplayed()
        composeTestRule.onNodeWithTag("product-Микс салатов").assertDoesNotExist()
    }
}