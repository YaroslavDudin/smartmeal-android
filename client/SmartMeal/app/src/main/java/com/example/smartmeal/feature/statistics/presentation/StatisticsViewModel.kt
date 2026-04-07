package com.example.smartmeal.feature.statistics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.data.api.RetrofitClient
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.home.data.MenuRepository
import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
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

internal fun sortMealsForStatistics(items: List<MenuItemDto>): List<MenuItemDto> {
    fun mealOrder(mealType: String): Int = when (mealType) {
        "breakfast" -> 0
        "lunch" -> 1
        "dinner" -> 2
        else -> 3
    }

    return items.sortedWith(
        compareBy<MenuItemDto> { mealOrder(it.meal_type) }
            .thenBy { it.meal_type }
            .thenBy { it.recipe_title.lowercase(Locale("ru")) }
    )
}

class StatisticsViewModel(private val preferences: SetupPreferences) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val menuApi = RetrofitClient.createService(MenuApi::class.java)
    private val menuRepository = MenuRepository(menuApi)
    
    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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
                // Получаем ВСЕ элементы меню пользователя. 
                // Теперь они сразу содержат КБЖУ за одну порцию.
                val allItems = menuRepository.getMenuItems()
                
                val planType = preferences.getPlanType()
                val planRange = preferences.getCustomPlanRange()
                val customPlan = if (planType == SetupPreferences.PLAN_TYPE_CUSTOM && planRange != null) {
                    Date(planRange.first) to Date(planRange.second)
                } else {
                    null
                }

                val itemsByDate = allItems.groupBy { it.actual_date }
                
                // Определяем диапазон дат для отображения
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
                
                val statsList = mutableListOf<DailyStats>()
                val currentCal = Calendar.getInstance().apply { time = startDate }
                
                while (!currentCal.time.after(endDate)) {
                    val currentDate = currentCal.time
                    val dateKey = apiDateFormatter.format(currentDate)
                    val itemsForDay = itemsByDate[dateKey] ?: emptyList()
                    
                    // ДЕДУПЛИКАЦИЯ: только один прием пищи каждого типа.
                    // РАСЧЕТ: строго по одной порции (per_serving), игнорируя настройки пользователя.
                    val uniqueItemsForDay = itemsForDay.sortedByDescending { it.id }.distinctBy { it.meal_type }
                    
                    var dayCalories = 0.0
                    var dayProteins = 0.0
                    var dayFats = 0.0
                    var dayCarbs = 0.0

                    for (item in uniqueItemsForDay) {
                        // Используем значения за одну порцию напрямую из API
                        dayCalories += item.per_serving_calories
                        dayProteins += item.per_serving_proteins
                        dayFats += item.per_serving_fats
                        dayCarbs += item.per_serving_carbs
                    }

                    statsList.add(
                        DailyStats(
                            date = currentDate,
                            totalCalories = dayCalories,
                            totalProteins = dayProteins,
                            totalFats = dayFats,
                            totalCarbs = dayCarbs,
                            meals = sortMealsForStatistics(uniqueItemsForDay)
                        )
                    )
                    currentCal.add(Calendar.DATE, 1)
                }

                val lastSelected = com.example.smartmeal.data.manager.DateManager.getLastSelectedDate()
                val targetDate = lastSelected ?: today

                var targetIndex = statsList.indexOfFirst { 
                    normalizeDate(it.date).time == normalizeDate(targetDate).time 
                }
                
                if (targetIndex == -1) {
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
