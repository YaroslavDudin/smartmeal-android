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
import com.example.smartmeal.feature.products.presentation.ProductListViewModel
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.home.data.api.MenuApi
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smartmeal.ui.theme.SmartMealTheme
import retrofit2.Response
import okhttp3.ResponseBody
import com.example.smartmeal.feature.home.data.api.RecalculateCartRequest
import com.example.smartmeal.feature.home.data.api.UpdateCartItemRequest
import com.example.smartmeal.feature.home.data.api.ToggleFavoriteRequest
import com.example.smartmeal.feature.home.data.api.ExportCartRequest
import com.example.smartmeal.feature.home.data.api.SetRecipeRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val dummyViewModel: ProductListViewModel by lazy {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = SetupPreferences(context)
        val api = object : MenuApi {
            override suspend fun getMenus(): Response<List<com.example.smartmeal.feature.home.data.menu.MenuDto>> = TODO()
            override suspend fun getMenu(id: Int): Response<com.example.smartmeal.feature.home.data.menu.MenuDto> = TODO()
            override suspend fun deleteMenu(id: Int): Response<Unit> = TODO()
            override suspend fun getMenuItems(): Response<List<com.example.smartmeal.feature.home.data.menu.MenuItemDto>> = TODO()
            override suspend fun deleteMenuItem(id: Int): Response<Unit> = TODO()
            override suspend fun replaceMenuItem(
                id: Int, 
                cookTimeRange: String?,
                totalCalories: Int?,
                mealCalories: String?,
                calorieMargin: Int?
            ): Response<com.example.smartmeal.feature.home.data.menu.MenuItemDto> = TODO()
            override suspend fun getRecipes(search: String?): Response<List<com.example.smartmeal.feature.home.data.menu.RecipeShortDto>> = TODO()
            override suspend fun getRecipe(id: Int, servings: Int?): Response<com.example.smartmeal.feature.home.data.menu.RecipeDetailDto> = TODO()
            override suspend fun getCart(): Response<Map<String, List<com.example.smartmeal.feature.home.data.menu.CartItemDto>>> = TODO()
            override suspend fun recalculateCart(request: RecalculateCartRequest): Response<Unit> = TODO()
            override suspend fun exportCart(all: Boolean, request: ExportCartRequest): Response<ResponseBody> = TODO()
            override suspend fun updateCartItem(id: Int, request: UpdateCartItemRequest): Response<com.example.smartmeal.feature.home.data.menu.CartItemDto> = TODO()
            override suspend fun deleteCartItem(id: Int): Response<Unit> = TODO()
            override suspend fun getFavorites(): Response<List<com.example.smartmeal.feature.home.data.api.UserFavoriteDto>> = TODO()
            override suspend fun toggleFavorite(request: ToggleFavoriteRequest): Response<com.example.smartmeal.feature.home.data.api.ToggleFavoriteResponse> = TODO()
            override suspend fun setRecipeToMenuItem(id: Int, request: SetRecipeRequest): Response<com.example.smartmeal.feature.home.data.menu.MenuItemDto> = TODO()
        }
        ProductListViewModel(api, prefs)
    }

    @Test
    fun productListScreen_titleIsDisplayed() {
        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    viewModel = dummyViewModel,
                    products = emptyList(),
                    selectedDate = null,
                    selectedStartDateKey = null,
                    selectedEndDateKey = null,
                    dateRangeText = "Выберите диапазон дней",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> },
                    onReselectPlan = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("title").assertIsDisplayed()
    }

    @Test
    fun productListScreen_showsMonthYearAboveDateSelector() {
        val date = Date() // Текущая дата
        val monthLabel = SimpleDateFormat("LLLL", Locale("ru")).format(date).replaceFirstChar { it.titlecase(Locale("ru")) }

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    viewModel = dummyViewModel,
                    products = listOf(
                        ProductUiModel(
                            id = "1",
                            name = "Product 1",
                            amount = "100 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            actualDates = setOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date))
                        ),
                        // Добавляем еще одну дату, чтобы DateSelector НЕ считал, что дата всего одна
                        ProductUiModel(
                            id = "2",
                            name = "Product 2",
                            amount = "100 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            actualDates = setOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 86400000)))
                        )
                    ),
                    selectedDate = date,
                    selectedStartDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date),
                    selectedEndDateKey = null,
                    dateRangeText = "Сегодня",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> },
                    onReselectPlan = {}
                )
            }
        }

        // Ищем по названию текущего месяца
        composeTestRule.onNodeWithText(monthLabel, substring = true).assertIsDisplayed()
    }

    @Test
    fun productListScreen_singleAvailableDate_showsFullDateSummary() {
        val selectedDate = dateFormatter.parse("2099-03-27") ?: Date()
        // formatSelectedDateLabel дает "Пятница - 27 Марта 2099" (или похожее)
        val datePart = "27 марта"

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    viewModel = dummyViewModel,
                    products = listOf(
                        ProductUiModel(
                            id = "1",
                            name = "Product 1",
                            amount = "100 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            actualDates = setOf("2099-03-27")
                        )
                    ),
                    selectedDate = selectedDate,
                    selectedStartDateKey = "2099-03-27",
                    selectedEndDateKey = null,
                    dateRangeText = "27 марта 2099",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> },
                    onReselectPlan = {}
                )
            }
        }

        composeTestRule.onNodeWithText(datePart, substring = true, ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun productListScreen_showsMonthRangeWhenSelectionSpansMonths() {
        val selectedDate = dateFormatter.parse("2099-03-30") ?: Date()
        // formatMonthYearRangeForSelector дает "Март - Апрель 2099"
        val rangePart = "Март - Апрель"

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    viewModel = dummyViewModel,
                    products = listOf(
                        ProductUiModel(
                            id = "1",
                            name = "Product 1",
                            amount = "100 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            actualDates = setOf("2099-03-30", "2099-04-05")
                        )
                    ),
                    selectedDate = selectedDate,
                    selectedStartDateKey = "2099-03-30",
                    selectedEndDateKey = "2099-04-05",
                    dateRangeText = "30 марта - 5 апреля 2099",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> },
                    onReselectPlan = {}
                )
            }
        }

        composeTestRule.onNodeWithText(rangePart, substring = true, ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun productListScreen_displaysProductsForSelectedRange() {
        val date = dateFormatter.parse("2099-03-10") ?: Date()
        val visibleName = "Visible product"
        val hiddenName = "Hidden product"

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    viewModel = dummyViewModel,
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
                            actualDates = setOf("2099-03-10")
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
                            actualDates = setOf("2099-03-11")
                        )
                    ),
                    selectedDate = date,
                    selectedStartDateKey = "2099-03-10",
                    selectedEndDateKey = "2099-03-10",
                    dateRangeText = "10 марта 2099",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> },
                    onReselectPlan = {}
                )
            }
        }

        composeTestRule.onNodeWithText(visibleName).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(hiddenName).assertCountEquals(0)
    }

    @Test
    fun productListScreen_dayClick_callsOnDateSelected() {
        var selectedDate: String? = null
        // Чтобы DateSelector отобразил чипы, даты должны быть в будущем (или сегодня)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val tomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 86400000))

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    viewModel = dummyViewModel,
                    products = listOf(
                        ProductUiModel(
                            id = "1",
                            name = "Product 1",
                            amount = "100 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            actualDates = setOf(today)
                        ),
                        ProductUiModel(
                            id = "2",
                            name = "Product 2",
                            amount = "100 g",
                            category = "cat",
                            icon = "",
                            categoryName = "Category",
                            categoryIcon = "",
                            actualDates = setOf(tomorrow)
                        )
                    ),
                    selectedDate = null,
                    selectedStartDateKey = null,
                    selectedEndDateKey = null,
                    dateRangeText = "Март 2026",
                    onDateSelected = { date -> selectedDate = date },
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> },
                    onReselectPlan = {}
                )
            }
        }

        // Чип с завтрашней датой будет вторым (индекс 1)
        composeTestRule.onNodeWithTag("date_chip_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals(tomorrow, selectedDate)
    }

    @Test
    fun productListScreen_checkAllButton_togglesAllVisibleProducts() {
        var checkedIds = emptyList<String>()
        var checkedValue = false

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    viewModel = dummyViewModel,
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
                    dateRangeText = "Март 2099",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { ids, checked ->
                        checkedIds = ids.toList()
                        checkedValue = checked
                    },
                    onReselectPlan = {}
                )
            }
        }

        // Вместо тега ищем по тексту "Выбрать всё"
        composeTestRule.onNodeWithText("Выбрать всё").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("1", "2"), checkedIds.sorted())
        assertEquals(true, checkedValue)
    }

    @Test
    fun productListScreen_categoriesGroupedCorrectly() {
        val regularCategory = "Овощи и фрукты"
        val checkedCategory = "Покупки"

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    viewModel = dummyViewModel,
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
                            categoryName = "Бакалея",
                            categoryIcon = "",
                            checked = true
                        )
                    ),
                    selectedDate = null,
                    selectedStartDateKey = null,
                    selectedEndDateKey = null,
                    dateRangeText = "Март 2099",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> },
                    onReselectPlan = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("category-$regularCategory").assertIsDisplayed()
        // Заголовок "Покупки" появляется автоматически для всех checked продуктов
        composeTestRule.onNodeWithTag("category-$checkedCategory").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun productListScreen_expiredDays_showsReselectPlanState() {
        var clicked = false

        composeTestRule.setContent {
            SmartMealTheme {
                ProductListScreen(
                    viewModel = dummyViewModel,
                    products = emptyList(),
                    selectedDate = null,
                    selectedStartDateKey = null,
                    selectedEndDateKey = null,
                    dateRangeText = "",
                    onDateSelected = {},
                    onProductChecked = { _, _ -> },
                    onCheckAll = { _, _ -> },
                    onReselectPlan = { clicked = true },
                    hasNoAvailableDays = true
                )
            }
        }

        // Вместо тегов ищем по тексту
        composeTestRule.onNodeWithText("Доступные дни закончились").assertIsDisplayed()
        composeTestRule.onNodeWithText("Выбрать заново").performClick()
        assertTrue(clicked)
    }
}
