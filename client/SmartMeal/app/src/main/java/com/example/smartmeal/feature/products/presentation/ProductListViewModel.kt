package com.example.smartmeal.feature.products.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import com.example.smartmeal.feature.home.data.menu.RecipeDetailDto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class ProductListViewModel(
    private val menuApi: MenuApi,
    private val preferences: SetupPreferences
) : ViewModel() {

    private data class RecipeCacheKey(
        val recipeId: Int,
        val servings: Int,
    )

    init {
        viewModelScope.launch {
            com.example.smartmeal.data.manager.DateManager.dateUpdates.collect { date ->
                val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
                if (dateKey in availableDateKeys && selectedStartDateKey != dateKey) {
                    selectedStartDateKey = dateKey
                    selectedEndDateKey = null
                    updateDateRangeText()
                }
            }
        }
        viewModelScope.launch {
            com.example.smartmeal.data.manager.ProfileManager.profileUpdates.collect {
                recipeCache.clear()
                if (lastMenuItems.isNotEmpty()) {
                    if (isLoading) {
                        pendingProductsRefresh = true
                    } else {
                        generateProductsFromMenuItems(lastMenuItems)
                    }
                }
            }
        }
    }

    // Recipe cache: (recipeId, servings) -> full recipe details
    private val recipeCache = mutableMapOf<RecipeCacheKey, RecipeDetailDto>()
    private var lastMenuItems: List<MenuItemDto> = emptyList()
    private var lastMenuSignature: String? = null
    private var pendingProductsRefresh = false

    var products by mutableStateOf<List<ProductUiModel>>(emptyList())
        private set

    var selectedStartDateKey by mutableStateOf<String?>(null)
        private set

    var selectedEndDateKey by mutableStateOf<String?>(null)
        private set

    var availableDateKeys by mutableStateOf<List<String>>(emptyList())
        private set

    var dateRangeText by mutableStateOf("Выберите диапазон дней")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var hasNoAvailableDays by mutableStateOf(false)
        private set

    private val checkedMap: SnapshotStateMap<String, Boolean> = mutableStateMapOf()

    private val categoryNormalizeMap = mapOf(
        "Овощи" to "Овощи и фрукты",
        "Фрукты" to "Овощи и фрукты",
        "Мясо" to "Мясо и рыба",
        "Рыба" to "Мясо и рыба",
        "Молочные продукты" to "Молочные продукты",
        "Бакалея" to "Бакалея и молочные продукты",
        "Специи" to "Специи",
        "Консервы" to "Консервированные продукты",
        "Консервированные продукты" to "Консервированные продукты",
        "Паста" to "Пасты",
        "Пасты" to "Пасты",
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
        "Консервированные продукты" to "🥫",
        "Пасты" to "🍝",
        "Овощи, зелень и грибы" to "🥬",
        "Приправы и специи" to "🌿",
        "Соусы" to "🫙",
        "Орехи и семена" to "🥜",
        "Рыба и морепродукты" to "🐟",
        "Молочные продукты и яйца" to "🥚",
        "Мука и мучные изделия" to "🥖",
        "Добавки для приготовления блюд" to "🍽️"
    )

    fun clearProducts() {
        products = emptyList()
        selectedStartDateKey = null
        selectedEndDateKey = null
        availableDateKeys = emptyList()
        dateRangeText = "Выберите диапазон дней"
        errorMessage = null
        hasNoAvailableDays = false
        lastMenuItems = emptyList()
        lastMenuSignature = null
        pendingProductsRefresh = false
        recipeCache.clear() // Clear the cache when the menu changes.
    }

    fun selectDateRange(dateKey: String) {
        if (dateKey !in availableDateKeys) return

        when {
            selectedStartDateKey == null -> {
                selectedStartDateKey = dateKey
                selectedEndDateKey = null
            }
            selectedEndDateKey == null -> {
                val currentStart = selectedStartDateKey ?: dateKey
                if (dateKey < currentStart) {
                    selectedStartDateKey = dateKey
                    selectedEndDateKey = currentStart
                } else {
                    selectedEndDateKey = dateKey
                }
            }
            else -> {
                selectedStartDateKey = dateKey
                selectedEndDateKey = null
            }
        }

        updateDateRangeText()
        
        // Notify other screens about the selected range.
        val startDate = selectedStartDateKey?.let { parseApiDate(it) }
        val endDate = selectedEndDateKey?.let { parseApiDate(it) }
        
        if (startDate != null) {
            com.example.smartmeal.data.manager.DateManager.notifyDateSelected(startDate, endDate)
        }
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
            if (isLoading) return@launch
            isLoading = true
            errorMessage = null
            lastMenuItems = menuItems
            try {
                val menuSignature = buildMenuSignature(menuItems)
                if (menuSignature != lastMenuSignature) {
                    recipeCache.clear()
                    lastMenuSignature = menuSignature
                }

                val todayKey = currentApiDateKey()
                val upcomingMenuItems = menuItems.filter { it.actual_date >= todayKey }
                hasNoAvailableDays = menuItems.isNotEmpty() && upcomingMenuItems.isEmpty()

                if (upcomingMenuItems.isEmpty()) {
                    products = emptyList()
                    initializeRangeFromProducts(emptyList())
                    return@launch
                }

                val globalPortionSize = preferences.getPortionSize()
                val requiredKeys = upcomingMenuItems
                    .map { menuItem ->
                        val overrideServings = preferences.getMenuItemServings(menuItem.id)
                        val effectiveServings = if (overrideServings > 0) overrideServings else globalPortionSize
                        RecipeCacheKey(menuItem.recipe, effectiveServings)
                    }
                    .distinct()
                val missingKeys = requiredKeys.filter { it !in recipeCache }

                if (missingKeys.isNotEmpty()) {
                    val deferred = missingKeys.map { cacheKey ->
                        async {
                            val response = menuApi.getRecipe(cacheKey.recipeId, servings = cacheKey.servings)
                            if (response.isSuccessful && response.body() != null) {
                                cacheKey to response.body()!!
                            } else {
                                null
                            }
                        }
                    }
                    deferred.awaitAll().filterNotNull().forEach { (cacheKey, recipe) ->
                        recipeCache[cacheKey] = recipe
                    }
                }

                val rawProducts = mutableListOf<ProductUiModel>()
                val orderedDateKeys = upcomingMenuItems
                    .map { it.actual_date }
                    .distinct()
                    .sorted()

                val dateIndexMap = orderedDateKeys.withIndex().associate { it.value to it.index }

                for (menuItem in upcomingMenuItems) {
                    val dateIndex = dateIndexMap[menuItem.actual_date] ?: 0
                    
                    // Per-meal custom servings still have priority over the global value.
                    val overrideServings = preferences.getMenuItemServings(menuItem.id)
                    val effectiveServings = if (overrideServings > 0) overrideServings else globalPortionSize

                    val recipe = recipeCache[RecipeCacheKey(menuItem.recipe, effectiveServings)] ?: continue

                    (recipe.ingredients ?: emptyList()).forEachIndexed { ingredientIndex, ingredient ->
                        if (isExcludedIngredient(ingredient.ingredient_name)) {
                            return@forEachIndexed
                        }

                        val rawCategory = ingredient.category_name ?: "Разное"
                        val normalizedCategory = categoryNormalizeMap[rawCategory] ?: rawCategory
                        val icon = categoryIconMap[normalizedCategory] ?: "🛒"

                        val isEgg = ingredient.ingredient_name.trim().lowercase(Locale("ru")).contains("яйцо") || 
                                     ingredient.ingredient_name.trim().lowercase(Locale("ru")).contains("яйца")

                        val amountString = if (isEgg) {
                            // Для яиц ВСЕГДА показываем количество в штуках, как в рецепте
                            val roundedAmount = kotlin.math.round(ingredient.amount * 10) / 10.0
                            val formatted = if (roundedAmount % 1.0 == 0.0) roundedAmount.toInt().toString() else roundedAmount.toString()
                            "$formatted шт"
                        } else {
                            // Для всего остального используем стандартную логику веса
                            val normalizedAmountInGrams = resolveAmountInGrams(
                                amountInGrams = ingredient.amount_in_base_units,
                                fallbackAmount = ingredient.amount,
                                fallbackUnit = ingredient.unit_name
                            )
                            formatWeightDisplay(normalizedAmountInGrams)
                        }

                        val occurrenceId = buildOccurrenceId(
                            ingredientName = ingredient.ingredient_name,
                            categoryName = normalizedCategory,
                            actualDate = menuItem.actual_date,
                            mealType = menuItem.meal_type,
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
                            dayOffsets = setOf(dateIndex),
                            actualDates = setOf(menuItem.actual_date),
                            sourceIds = setOf(occurrenceId)
                        )
                    }
                }

                products = rawProducts.sortedWith(
                    compareBy<ProductUiModel> { categorySortOrder(it.categoryName) }
                        .thenBy { it.name.lowercase(Locale("ru")) }
                )

                initializeRangeFromProducts(orderedDateKeys)
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = e.localizedMessage ?: "Не удалось загрузить продукты"
                hasNoAvailableDays = false
            } finally {
                isLoading = false
                if (pendingProductsRefresh) {
                    pendingProductsRefresh = false
                    generateProductsFromMenuItems(lastMenuItems)
                }
            }
        }
    }

    private fun initializeRangeFromProducts(orderedDateKeys: List<String>) {
        availableDateKeys = orderedDateKeys

        if (orderedDateKeys.isEmpty()) {
            selectedStartDateKey = null
            selectedEndDateKey = null
            dateRangeText = "Выберите диапазон дней"
            return
        }

        val globalSelectedDate = com.example.smartmeal.data.manager.DateManager.getLastSelectedDate()
        val globalSelectedEndDate = com.example.smartmeal.data.manager.DateManager.getLastSelectedEndDate()
        
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val globalStartKey = globalSelectedDate?.let { formatter.format(it) }
        val globalEndKey = globalSelectedEndDate?.let { formatter.format(it) }

        // Приоритет: 
        // 1. Уже выбранный ключ в текущей сессии ViewModel (если он валиден)
        // 2. Глобально выбранная дата из DateManager (из Home/Stats)
        // 3. Первый доступный день (обычно сегодня)
        
        val candidateStart = selectedStartDateKey ?: globalStartKey
        selectedStartDateKey = if (candidateStart in orderedDateKeys) candidateStart else orderedDateKeys.first()
        
        val candidateEnd = selectedEndDateKey ?: globalEndKey
        selectedEndDateKey = if (candidateEnd in orderedDateKeys) candidateEnd else null
        
        updateDateRangeText()
    }

    private fun updateDateRangeText() {
        val startKey = selectedStartDateKey
        if (startKey == null) {
            dateRangeText = "Выберите диапазон дней"
            return
        }

        val endKey = selectedEndDateKey ?: startKey
        val formatter = SimpleDateFormat("d MMM", Locale("ru"))
        val startDate = parseApiDate(startKey)
        val endDate = parseApiDate(endKey)

        dateRangeText = if (startDate == null || endDate == null) {
            "Выберите диапазон дней"
        } else if (startKey == endKey) {
            formatter.format(startDate)
        } else {
            "${formatter.format(startDate)} - ${formatter.format(endDate)}"
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

internal fun buildMenuSignature(menuItems: List<MenuItemDto>): String {
    return menuItems.joinToString("|") { item ->
        "${item.id}:${item.recipe}:${item.actual_date}:${item.meal_type}"
    }
}

internal fun parseApiDate(value: String): Date? {
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)
    } catch (_: Exception) {
        null
    }
}

internal fun buildOccurrenceId(
    ingredientName: String,
    categoryName: String,
    actualDate: String,
    mealType: String,
    ingredientIndex: Int
): String {
    return "${ingredientName.trim()}_${categoryName.trim()}_${actualDate}_${mealType}_$ingredientIndex"
}

internal fun isExcludedIngredient(name: String): Boolean {
    val normalized = name.trim().lowercase(Locale("ru"))
    return normalized.contains("\u0432\u043e\u0434\u0430") ||
        normalized.contains("water")
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


internal fun currentApiDateKey(today: Date = Date()): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(today)
}

