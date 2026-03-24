package com.example.smartmeal.feature.products.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

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

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val checkedMap: SnapshotStateMap<String, Boolean> = mutableStateMapOf()
    private val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

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
        "Покупки" to "🛍️",
        "Фрукты и ягоды" to "🍓",
        "Мясо и мясная продукция" to "🍖",
        "Бобовые" to "🫘",
        "Овощи, зелень и грибы" to "🥬",
        "Приправы и специи" to "🌿",
        "Соусы" to "🫙",
        "Орехи и семена" to "🥜",
        "Рыба и морепродукты" to "🐟",
        "Молочные продукты и яйца" to "🥚",
        "Мука и мучные изделия" to "🥖",
        "Добавки для приготовления блюд" to "🍽️"
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

    fun clearProducts() {
        products = emptyList()
        selectedStartDayIndex = null
        selectedEndDayIndex = null
        dateRangeText = "Выберите диапазон дней"
        errorMessage = null
    }

    fun selectDayRange(dayLabel: String) {
        val dayIndex = dayNames.indexOf(dayLabel)
        if (dayIndex == -1) return

        when {
            selectedStartDayIndex == null -> {
                selectedStartDayIndex = dayIndex
                selectedEndDayIndex = null
            }
            selectedEndDayIndex == null -> {
                val currentStart = selectedStartDayIndex ?: dayIndex
                if (dayIndex < currentStart) {
                    selectedStartDayIndex = dayIndex
                    selectedEndDayIndex = currentStart
                } else {
                    selectedEndDayIndex = dayIndex
                }
            }
            else -> {
                selectedStartDayIndex = dayIndex
                selectedEndDayIndex = null
            }
        }

        updateDateRangeText()
    }

    fun toggleCheckAllProducts(productIds: Collection<String>, checked: Boolean) {
        if (productIds.isEmpty()) return
        productIds.forEach { productId ->
            checkedMap[productId] = checked
        }
        products = products.map { product ->
            if (product.id in productIds) {
                product.copy(checked = checked)
            } else {
                product
            }
        }
    }

    fun onProductChecked(productIds: Collection<String>, checked: Boolean) {
        if (productIds.isEmpty()) return
        productIds.forEach { productId ->
            checkedMap[productId] = checked
        }
        products = products.map { product ->
            if (product.id in productIds) {
                product.copy(checked = checked)
            } else {
                product
            }
        }
    }

    fun generateProductsFromMenuItems(menuItems: List<MenuItemDto>) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val rawProducts = mutableListOf<ProductUiModel>()

                for (menuItem in menuItems) {
                    val weekdayIndex = weekdayIndexFromDate(menuItem.actual_date)
                    val response = menuApi.getRecipe(menuItem.recipe)
                    if (!response.isSuccessful) continue

                    response.body()?.ingredients?.forEachIndexed { ingredientIndex, ingredient ->
                        if (isExcludedIngredient(ingredient.ingredient_name)) {
                            return@forEachIndexed
                        }

                        val rawCategory = ingredient.category_name ?: "Разное"
                        val normalizedCategory = categoryNormalizeMap[rawCategory] ?: rawCategory
                        val icon = categoryIconMap[normalizedCategory] ?: "🛒"
                        val normalizedAmountInGrams = resolveAmountInGrams(
                            amountInGrams = ingredient.amount_in_grams,
                            fallbackAmount = ingredient.amount,
                            fallbackUnit = ingredient.unit_name
                        )
                        val amountString = formatWeightDisplay(normalizedAmountInGrams)
                        val occurrenceId = buildOccurrenceId(
                            ingredientName = ingredient.ingredient_name,
                            categoryName = normalizedCategory,
                            actualDate = menuItem.actual_date,
                            ingredientIndex = ingredientIndex
                        )

                        rawProducts += ProductUiModel(
                            id = occurrenceId,
                            name = ingredient.ingredient_name,
                            amount = amountString,
                            category = normalizedCategory,
                            icon = "",
                            categoryName = normalizedCategory,
                            categoryIcon = icon,
                            checked = checkedMap[occurrenceId] ?: false,
                            dayOffsets = setOf(weekdayIndex),
                            actualDates = setOf(menuItem.actual_date),
                            sourceIds = setOf(occurrenceId)
                        )
                    }
                }

                products = rawProducts.sortedWith(
                    compareBy<ProductUiModel> { categorySortOrder(it.categoryName) }
                        .thenBy { it.name.lowercase(Locale("ru")) }
                )

                initializeRangeFromProducts(products)
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = e.localizedMessage ?: "Не удалось загрузить продукты"
            } finally {
                isLoading = false
            }
        }
    }

    private fun initializeRangeFromProducts(products: List<ProductUiModel>) {
        val availableDayIndices = products
            .flatMap { it.dayOffsets }
            .distinct()
            .sorted()

        if (availableDayIndices.isEmpty()) {
            selectedStartDayIndex = null
            selectedEndDayIndex = null
            dateRangeText = "Выберите диапазон дней"
            return
        }

        selectedStartDayIndex = availableDayIndices.first()
        selectedEndDayIndex = availableDayIndices.last()
        updateDateRangeText()
    }

    private fun updateDateRangeText() {
        val startIndex = selectedStartDayIndex
        if (startIndex == null) {
            dateRangeText = "Выберите диапазон дней"
            return
        }

        val endIndex = selectedEndDayIndex ?: startIndex
        dateRangeText = if (startIndex == endIndex) {
            dayNames[startIndex]
        } else {
            "${dayNames[startIndex]}-${dayNames[endIndex]}"
        }
    }

    private fun categorySortOrder(categoryName: String): Int {
        val categoryOrder = listOf(
            "Овощи и фрукты",
            "Мясо и рыба",
            "Молочные продукты",
            "Бакалея и молочные продукты",
            "Специи",
            "Масла",
            "Напитки",
            "Зерновые",
            "Сладости",
            "Разное",
            "Покупки"
        )
        return categoryOrder.indexOf(categoryName).let { index ->
            if (index == -1) Int.MAX_VALUE else index
        }
    }
}

internal fun buildOccurrenceId(
    ingredientName: String,
    categoryName: String,
    actualDate: String,
    ingredientIndex: Int
): String {
    return "${ingredientName.trim()}_${categoryName.trim()}_${actualDate}_$ingredientIndex"
}

internal fun isExcludedIngredient(name: String): Boolean {
    val normalized = name.trim().lowercase(Locale("ru"))
    return normalized.contains("вода") || normalized.contains("water")
}

internal fun resolveAmountInGrams(
    amountInGrams: Double?,
    fallbackAmount: Double,
    fallbackUnit: String
): Double {
    if (amountInGrams != null && amountInGrams > 0.0) {
        return amountInGrams
    }
    return when (fallbackUnit.trim().lowercase()) {
        "г", "грамм", "гр", "gram" -> fallbackAmount
        "кг", "килограмм", "kilogram" -> fallbackAmount * 1000
        "мл", "миллилитр", "milliliter" -> fallbackAmount
        "л", "литр", "liter" -> fallbackAmount * 1000
        "ст.л.", "столовая ложка", "tablespoon", "ст. ложка" -> fallbackAmount * 15
        "ч.л.", "чайная ложка", "teaspoon", "ч. ложка" -> fallbackAmount * 5
        "щепотка", "pinch" -> fallbackAmount
        else -> fallbackAmount
    }
}

internal fun formatWeightDisplay(amountInGrams: Double): String {
    val roundedGrams = kotlin.math.round(amountInGrams * 10) / 10.0
    return if (roundedGrams >= 1000.0) {
        val kilograms = roundedGrams / 1000.0
        val formatted = if (kilograms % 1.0 == 0.0) {
            kilograms.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", kilograms)
        }
        "$formatted кг"
    } else {
        val formatted = if (roundedGrams % 1.0 == 0.0) {
            roundedGrams.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", roundedGrams)
        }
        "$formatted г"
    }
}

internal fun parseWeightToGrams(amount: String): Double {
    val value = amount.substringBeforeLast(' ').replace(',', '.').toDoubleOrNull() ?: 0.0
    val unit = amount.substringAfterLast(' ').trim().lowercase()
    return when (unit) {
        "кг" -> value * 1000
        else -> value
    }
}
