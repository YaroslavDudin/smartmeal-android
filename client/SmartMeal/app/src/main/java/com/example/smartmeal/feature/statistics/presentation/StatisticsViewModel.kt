package com.example.smartmeal.feature.statistics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.data.api.RetrofitClient
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.home.data.MenuRepository
import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import com.example.smartmeal.feature.home.data.menu.RecipeDetailDto
import com.example.smartmeal.feature.recipes.data.api.RecipeApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyStats(
    val date: Date,
    val totalCalories: Double = 0.0,
    val totalProteins: Double = 0.0,
    val totalFats: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val meals: List<MenuItemDto> = emptyList()
)

data class StatisticsUiState(
    val isLoading: Boolean = false,
    val dailyStats: List<DailyStats> = emptyList(),
    val error: String? = null,
    val selectedIndex: Int = 0
)

class StatisticsViewModel(private val preferences: SetupPreferences) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val menuApi = RetrofitClient.createService(MenuApi::class.java)
    private val recipeApi = RetrofitClient.createService(RecipeApi::class.java)
    private val menuRepository = MenuRepository(menuApi)
    
    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    // Кэш рецептов: ID рецепта -> Данные рецепта
    private val recipeCache = mutableMapOf<Int, RecipeDetailDto>()

    init {
        loadStatistics()
        viewModelScope.launch {
            com.example.smartmeal.data.manager.DateManager.dateUpdates.collect { date ->
                updateSelectedIndexForDate(date)
            }
        }
    }

    private fun updateSelectedIndexForDate(date: Date) {
        val targetTime = normalizeDate(date).time
        val index = _uiState.value.dailyStats.indexOfFirst {
            normalizeDate(it.date).time == targetTime
        }
        if (index != -1 && index != _uiState.value.selectedIndex) {
            _uiState.update { it.copy(selectedIndex = index) }
        }
    }

    fun refresh() {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Получаем ВСЕ элементы меню пользователя для охвата всех дней
                val allItems = menuRepository.getMenuItems()
                
                // Получаем кастомный план из настроек
                val planType = preferences.getPlanType()
                val planRange = preferences.getCustomPlanRange()
                val customPlan = if (planType == SetupPreferences.PLAN_TYPE_CUSTOM && planRange != null) {
                    Date(planRange.first) to Date(planRange.second)
                } else {
                    null
                }

                val itemsByDate = allItems.groupBy { it.actual_date }
                
                // 1. Загружаем все уникальные рецепты в кэш параллельно
                val uniqueRecipeIds = allItems.map { it.recipe }.distinct()
                val missingIds = uniqueRecipeIds.filter { it !in recipeCache }
                
                if (missingIds.isNotEmpty()) {
                    val deferred = missingIds.map { id ->
                        async {
                            try {
                                val response = recipeApi.getRecipeDetail(id)
                                val body = response.body()
                                if (response.isSuccessful && body != null) {
                                    id to body
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                    deferred.awaitAll().filterNotNull().forEach { (id, recipe) ->
                        recipeCache[id] = recipe
                    }
                }

                // 2. Определяем диапазон дат
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val startDate: Date
                val endDate: Date

                if (customPlan != null) {
                    startDate = normalizeDate(customPlan.first)
                    endDate = normalizeDate(customPlan.second)
                } else {
                    if (allItems.isNotEmpty()) {
                        val sortedDateStrings = itemsByDate.keys.sorted()
                        val firstDate = apiDateFormatter.parse(sortedDateStrings.first()) ?: Date()
                        val lastMenuDate = apiDateFormatter.parse(sortedDateStrings.last()) ?: Date()
                        
                        startDate = normalizeDate(firstDate)
                        endDate = if (lastMenuDate.before(today)) today else normalizeDate(lastMenuDate)
                    } else {
                        startDate = today
                        endDate = today
                    }
                }
                
                // 3. Генерируем список DailyStats для КАЖДОГО дня в диапазоне
                val statsList = mutableListOf<DailyStats>()
                val currentCal = Calendar.getInstance().apply { time = startDate }
                
                while (!currentCal.time.after(endDate)) {
                    val currentDate = currentCal.time
                    val dateKey = apiDateFormatter.format(currentDate)
                    val itemsForDay = itemsByDate[dateKey] ?: emptyList()
                    
                    var dayCalories = 0.0
                    var dayProteins = 0.0
                    var dayFats = 0.0
                    var dayCarbs = 0.0

                    for (item in itemsForDay) {
                        recipeCache[item.recipe]?.let { recipe ->
                            dayCalories += recipe.per_serving_calories
                            dayProteins += recipe.per_serving_proteins
                            dayFats += recipe.per_serving_fats
                            dayCarbs += recipe.per_serving_carbs
                        }
                    }

                    statsList.add(
                        DailyStats(
                            date = currentDate,
                            totalCalories = dayCalories,
                            totalProteins = dayProteins,
                            totalFats = dayFats,
                            totalCarbs = dayCarbs,
                            meals = itemsForDay
                        )
                    )
                    currentCal.add(Calendar.DATE, 1)
                }

                // 4. Находим индекс выбранного дня, сегодняшнего дня или наиболее близкого
                val lastSelected = com.example.smartmeal.data.manager.DateManager.getLastSelectedDate()
                val targetDate = lastSelected ?: today

                var targetIndex = statsList.indexOfFirst { 
                    normalizeDate(it.date).time == normalizeDate(targetDate).time 
                }
                
                if (targetIndex == -1) {
                    // Если не в диапазоне, выбираем ближайший к today или 0
                    targetIndex = statsList.indexOfFirst { normalizeDate(it.date).time == today.time }
                    if (targetIndex == -1) {
                        targetIndex = if (today.before(startDate)) 0 else statsList.size - 1
                    }
                }

                _uiState.update { it.copy(
                    isLoading = false,
                    dailyStats = statsList,
                    selectedIndex = targetIndex.coerceIn(0, maxOf(0, statsList.size - 1))
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Ошибка: ${e.localizedMessage}") }
            }
        }
    }

    private fun normalizeDate(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    fun setSelectedIndex(index: Int) {
        if (index in 0 until _uiState.value.dailyStats.size) {
            _uiState.update { it.copy(selectedIndex = index) }
            // Уведомляем другие экраны о смене даты
            val selectedDate = _uiState.value.dailyStats[index].date
            com.example.smartmeal.data.manager.DateManager.notifyDateSelected(selectedDate)
        }
    }
}

class StatisticsViewModelFactory(private val preferences: SetupPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
