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
    val selectedIndex: Int = 0,
    val isCaloriesEnabled: Boolean = false,
    val targetCalories: Double = 2000.0,
    val targetProteins: Double = 100.0,
    val targetFats: Double = 67.0,
    val targetCarbs: Double = 250.0
)

internal fun sortMealsForStatistics(items: List<MenuItemDto>): List<MenuItemDto> {
    fun mealOrder(mealType: String): Int = when (mealType.lowercase(Locale.US)) {
        "breakfast", "завтрак" -> 0
        "lunch", "обед" -> 1
        "dinner", "ужин" -> 2
        else -> 3
    }

    return items.sortedWith(
        compareBy<MenuItemDto> { mealOrder(it.meal_type) }
            .thenBy { it.meal_type }
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
        viewModelScope.launch {
            com.example.smartmeal.data.manager.MenuUpdateManager.menuUpdates.collect {
                loadStatistics()
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
                // 1. Получаем все элементы меню
                val allItems = menuRepository.getMenuItems()
                
                // 2. Определяем план как в HomeScreen
                val planType = preferences.getPlanType()
                val planRange = preferences.getCustomPlanRange()
                val selectedPlanDateMillis = preferences.getSelectedPlanDate()
                
                val customPlan = when (planType) {
                    SetupPreferences.PLAN_TYPE_CUSTOM -> {
                        planRange?.let { (start, end) -> com.example.smartmeal.feature.home.presentation.CustomPlan(Date(start), Date(end)) }
                    }
                    SetupPreferences.PLAN_TYPE_WEEKLY -> {
                        selectedPlanDateMillis?.let { start ->
                            val end = Calendar.getInstance().apply {
                                time = Date(start)
                                add(Calendar.DATE, 6)
                            }.timeInMillis
                            com.example.smartmeal.feature.home.presentation.CustomPlan(Date(start), Date(end))
                        }
                    }
                    SetupPreferences.PLAN_TYPE_DAILY -> {
                        selectedPlanDateMillis?.let { start ->
                            com.example.smartmeal.feature.home.presentation.CustomPlan(Date(start), Date(start))
                        }
                    }
                    else -> null
                }

                // 3. Используем ту же логику buildAvailableDates что и в HomeScreen
                val availableDates = buildAvailableDatesInternal(allItems, customPlan)
                
                if (availableDates.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, dailyStats = emptyList()) }
                    return@launch
                }

                val itemsByDate = allItems.groupBy { it.actual_date }
                val statsList = mutableListOf<DailyStats>()

                for (currentDate in availableDates) {
                    val dateKey = apiDateFormatter.format(currentDate)
                    val itemsForDay = itemsByDate[dateKey] ?: emptyList()
                    
                    // ДЕДУПЛИКАЦИЯ: оставляем только самое свежее блюдо (с макс. ID) для каждого типа приема пищи
                    val uniqueItemsForDay = itemsForDay.sortedByDescending { it.id }
                        .distinctBy { it.meal_type.lowercase(Locale.US) }
                    
                    var dayCalories = 0.0
                    var dayProteins = 0.0
                    var dayFats = 0.0
                    var dayCarbs = 0.0

                    for (item in uniqueItemsForDay) {
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
                }

                val today = normalizeDate(Date())
                val lastSelected = com.example.smartmeal.data.manager.DateManager.getLastSelectedDate()
                val targetDate = lastSelected ?: today

                var targetIndex = statsList.indexOfFirst { 
                    normalizeDate(it.date).time == normalizeDate(targetDate).time 
                }
                
                if (targetIndex == -1) {
                    targetIndex = statsList.indexOfFirst { normalizeDate(it.date).time == today.time }
                    if (targetIndex == -1) targetIndex = 0
                }

                // Читаем настройку включенности калорий
                val isCaloriesEnabled = preferences.isCaloriesEnabled()
                val totalCals = if (isCaloriesEnabled) preferences.getTotalCalories().toDouble() else 0.0
                val gender = preferences.getGender() ?: "male"

                // Базовые пропорции (Белки/Жиры/Углеводы в % от калорий)
                val (pPerc, fPerc, cPerc) = if (gender == "female") {
                    Triple(0.20, 0.30, 0.50)
                } else {
                    Triple(0.25, 0.25, 0.50)
                }

                val targetProteins = (totalCals * pPerc) / 4.0
                val targetFats = (totalCals * fPerc) / 9.0
                val targetCarbs = (totalCals * cPerc) / 4.0

                _uiState.update { it.copy(
                    isLoading = false,
                    dailyStats = statsList,
                    selectedIndex = targetIndex.coerceIn(0, maxOf(0, statsList.size - 1)),
                    isCaloriesEnabled = isCaloriesEnabled,
                    targetCalories = totalCals,
                    targetProteins = targetProteins,
                    targetFats = targetFats,
                    targetCarbs = targetCarbs
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Ошибка: ${e.localizedMessage}") }
            }
        }
    }

    // Дублируем внутреннюю логику из HomeScreen для консистентности
    private fun buildAvailableDatesInternal(
        menuItems: List<MenuItemDto>,
        customPlan: com.example.smartmeal.feature.home.presentation.CustomPlan?,
        today: Date = Date()
    ): List<Date> {
        val normalizedToday = normalizeDate(today)
        if (customPlan != null) {
            val dates = mutableListOf<Date>()
            val cal = Calendar.getInstance().apply { 
                time = if (normalizeDate(customPlan.startDate).before(normalizedToday)) normalizedToday else normalizeDate(customPlan.startDate)
            }
            val end = normalizeDate(customPlan.endDate)

            if (cal.time.after(end)) return emptyList()

            while (!cal.time.after(end)) {
                dates.add(cal.time.clone() as Date)
                cal.add(Calendar.DATE, 1)
            }
            return dates
        }

        return menuItems
            .mapNotNull { item -> try { apiDateFormatter.parse(item.actual_date) } catch(e: Exception) { null } }
            .map { normalizeDate(it) }
            .filter { !it.before(normalizedToday) }
            .distinct()
            .sorted()
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
