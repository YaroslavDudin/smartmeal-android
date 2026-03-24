package com.example.smartmeal.feature.products.presentation

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.feature.home.data.api.MenuApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.smartmeal.feature.home.data.menu.MenuItemDto

class ProductListViewModel(
    private val menuApi: MenuApi
) : ViewModel() {

    var products by mutableStateOf<List<ProductUiModel>>(emptyList())
        private set

    var selectedStartDayIndex by mutableStateOf<Int?>(null)
        private set

    var selectedEndDayIndex by mutableStateOf<Int?>(null)
        private set

    var dateRangeText by mutableStateOf("Выберите диапазон дней")
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val checkedMap = mutableStateMapOf<String, Boolean>()


    private val categoryNormalizeMap = mapOf(
        "Овощи" to "Овощи и фрукты",
        "Фрукты" to "Овощи и фрукты",
        "Мясо" to "Мясо и рыба",
        "Рыба" to "Мясо и рыба",
        "Молочные продукты" to "Молочные продукты",
        "Бакалея" to "Бакалея и молочные продукты",
        "Специи" to "Специи",
        "Масла" to "Масла",
        "Напитки" to "Напитки",
        "Зерновые" to "Зерновые",
        "Сладости" to "Сладости"
    )

    private val categoryIconMap = mapOf(
        "Мясо и рыба" to "🐟",
        "Овощи и фрукты" to "🥗",
        "Молочные продукты" to "🥛",
        "Бакалея и молочные продукты" to "🧀",
        "Специи" to "🌿",
        "Масла" to "🫒",
        "Напитки" to "🥤",
        "Зерновые" to "🌾",
        "Сладости" to "🍬",
        "Разное" to "🛒",
        "Покупки" to "🛍️"
    )

    private fun weekdayIndexFromDate(dateString: String): Int {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString) ?: Date()
        val calendar = Calendar.getInstance().apply { time = date }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    fun toggleCheckAllProducts() {
        val allChecked = products.all { it.checked }

        if (allChecked) {
            products.forEach { product ->
                checkedMap[product.id] = false
            }
            products = products.map { it.copy(checked = false) }
        } else {
            products.forEach { product ->
                checkedMap[product.id] = true
            }
            products = products.map { it.copy(checked = true) }
        }
    }

    fun generateProductsFromMenuItems(menuItems: List<MenuItemDto>) {
        viewModelScope.launch {
            isLoading = true
            try {
                val tempMap = mutableMapOf<String, MutableList<ProductUiModel>>()
                for (menuItem in menuItems) {
                    val weekdayIndex = weekdayIndexFromDate(menuItem.actual_date)
                    val response = menuApi.getRecipe(menuItem.recipe)
                    if (response.isSuccessful) {
                        response.body()?.ingredients?.forEach { ing ->
                            val rawCategory = ing.category_name ?: "Разное"
                            val normalizedCategory = categoryNormalizeMap[rawCategory] ?: rawCategory
                            val icon = categoryIconMap[normalizedCategory] ?: "🛒"
                            val (normalizedAmount, normalizedUnit) = normalizeAmount(ing.amount, ing.unit_name)
                            val amountStr = "$normalizedAmount $normalizedUnit"
                            val key = "${ing.ingredient_name}_${normalizedCategory}_${normalizedUnit}"
                            val product = ProductUiModel(
                                id = key,
                                name = ing.ingredient_name,
                                amount = amountStr,
                                category = normalizedCategory,
                                icon = "",
                                categoryName = normalizedCategory,
                                categoryIcon = icon,
                                checked = checkedMap[key] ?: false,
                                dayOffsets = setOf(weekdayIndex),
                                actualDates = setOf(menuItem.actual_date)
                            )
                            tempMap.getOrPut(key) { mutableListOf() }.add(product)
                        }
                    }
                }
                val mergedProducts = tempMap.map { (_, list) ->
                    val first = list.first()
                    first.copy(
                        amount = sumAmountsSafe(list),
                        checked = list.any { it.checked },
                        dayOffsets = list.flatMap { it.dayOffsets }.toSet(),
                        actualDates = list.flatMap { it.actualDates }.toSet()
                    )
                }
                products = mergedProducts
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    private fun sumAmountsSafe(list: List<ProductUiModel>): String {
        val unit = list.first().amount.substringAfterLast(' ').trim()
        val total = list.sumOf {
            it.amount.substringBeforeLast(' ').toDoubleOrNull() ?: 0.0
        }
        val formatted = if (total % 1.0 == 0.0) total.toInt().toString() else "%.1f".format(total)
        return "$formatted $unit"
    }


    private fun normalizeAmount(amount: Double, unit: String): Pair<Double, String> {
        val normalizedUnit = unit.trim().lowercase()
        return when (normalizedUnit) {
            "г", "грамм", "гр", "gram" -> amount to "г"
            "кг", "килограмм", "kilogram" -> (amount * 1000) to "г"
            "мл", "миллилитр", "milliliter" -> amount to "мл"
            "л", "литр", "liter" -> (amount * 1000) to "мл"
            "ст.л.", "столовая ложка", "tablespoon", "ст. ложка" -> (amount * 15) to "г"
            "ч.л.", "чайная ложка", "teaspoon", "ч. ложка" -> (amount * 5) to "г"
            "щепотка", "pinch" -> (amount * 1) to "г"
            "шт.", "штука", "шт", "piece" -> amount to "шт."
            "пучок", "bunch" -> amount to "пучок"
            else -> amount to unit
        }
    }
    fun onProductChecked(productId: String, checked: Boolean) {
        checkedMap[productId] = checked
        products = products.map {
            if (it.id == productId) it.copy(checked = checked) else it
        }
    }
}