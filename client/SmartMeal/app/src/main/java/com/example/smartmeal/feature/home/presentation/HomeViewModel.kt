package com.example.smartmeal.feature.home.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.data.api.RetrofitClient
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.home.data.MenuRepository
import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.data.menu.MenuDto
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import com.example.smartmeal.feature.menu_generator.data.api.GeneratorApi
import com.example.smartmeal.feature.menu_generator.data.models.AutoGenerateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MealSection(val id: String, val title: String, val meal: MenuItemDto)

data class HomeUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false, // Новый флаг для твоей красивой загрузки
    val hasMenu: Boolean = false,
    val error: String? = null,
    val selectedDay: String = "",
    val selectedDate: Date? = null,
    val selectedDateFromPlan: Boolean = false,
    val mealSections: List<MealSection> = emptyList(),
    val currentMenu: MenuDto? = null,
    val allMenuItems: List<MenuItemDto> = emptyList(),
    val customPlan: CustomPlan? = null
)

class HomeViewModel(
    application: Application,
    private val preferences: SetupPreferences
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val menuRepository = MenuRepository(
        RetrofitClient.createService(MenuApi::class.java), 
        application.applicationContext
    )
    private val generatorApi = RetrofitClient.createService(GeneratorApi::class.java)
    private val menuApi: MenuApi = RetrofitClient.createService(MenuApi::class.java)
    private val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        loadData()
        
        viewModelScope.launch {
            com.example.smartmeal.data.manager.FavoritesManager.favoriteUpdates.collect { update ->
                updateFavoriteInState(update.recipeId, update.isFavorite)
            }
        }
        viewModelScope.launch {
            com.example.smartmeal.data.manager.DateManager.dateUpdates.collect { date ->
                if (_uiState.value.selectedDate?.time != date.time) {
                    val customPlan = _uiState.value.customPlan
                    selectDate(date, customPlan, notifyManager = false)
                }
            }
        }
        viewModelScope.launch {
            com.example.smartmeal.data.manager.MealSlotManager.activeMealType.collect {
                updateMealSections()
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            // 1. МГНОВЕННО: Пробуем показать то, что есть в кэше
            val cachedMenu = MenuRepository.getLatestMenuCache()
            val cachedItems = MenuRepository.getMenuItemsCache()
            
            if (cachedMenu != null && cachedItems != null) {
                // Если есть кэш, сразу его отрисовываем
                processMenuUpdate(cachedMenu, cachedItems)
                _uiState.update { it.copy(isSyncing = true) } // Показываем оверлей "Сверяем данные"
            } else {
                _uiState.update { it.copy(isLoading = true) }
            }

            // 2. ФОНОВОЕ ОБНОВЛЕНИЕ: Сверка с Postgres
            try {
                val menu = menuRepository.getLatestMenu()
                if (menu != null) {
                    val allItems = menuRepository.getMenuItems()
                    processMenuUpdate(menu, allItems)
                }
            } catch (e: Exception) {
                if (!_uiState.value.hasMenu) {
                    _uiState.update { it.copy(error = e.localizedMessage) }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false, isSyncing = false) }
            }
        }
    }

    private fun processMenuUpdate(menu: MenuDto, allItems: List<MenuItemDto>) {
        val planType = preferences.getPlanType()
        val planRange = preferences.getCustomPlanRange()
        val selectedPlanDateMillis = preferences.getSelectedPlanDate()

        val (planStart, planEnd) = when (planType) {
            SetupPreferences.PLAN_TYPE_CUSTOM -> {
                if (planRange != null) Date(planRange.first) to Date(planRange.second)
                else null to null
            }
            SetupPreferences.PLAN_TYPE_WEEKLY -> {
                if (selectedPlanDateMillis != null) {
                    val start = Date(selectedPlanDateMillis)
                    val end = Calendar.getInstance().apply {
                        time = start
                        add(Calendar.DATE, 6)
                    }.time
                    start to end
                } else null to null
            }
            SetupPreferences.PLAN_TYPE_DAILY -> {
                if (selectedPlanDateMillis != null) Date(selectedPlanDateMillis) to Date(selectedPlanDateMillis)
                else null to null
            }
            else -> null to null
        }

        val menuStart = try { apiDateFormatter.parse(menu.start_date) } catch(e: Exception) { null } ?: Date()
        val maxOffset = menu.items?.maxOfOrNull { it.day_offset } ?: 0
        val menuEnd = Calendar.getInstance().apply {
            time = menuStart
            add(Calendar.DATE, maxOffset)
        }.time

        _uiState.update {
            val today = normalizeDateStatic(Date())
            val availableDates = buildAvailableDates(menu.items ?: emptyList(), CustomPlan(menuStart, menuEnd))
            val lastSelected = com.example.smartmeal.data.manager.DateManager.getLastSelectedDate()
            val resolvedSelectedDate = if (lastSelected != null && availableDates.any { it.time == normalizeDateStatic(lastSelected).time }) {
                normalizeDateStatic(lastSelected)
            } else if (availableDates.any { it.time == today.time }) {
                today
            } else {
                availableDates.firstOrNull()
            }

            it.copy(
                hasMenu = true,
                currentMenu = menu,
                customPlan = CustomPlan(menuStart, menuEnd),
                allMenuItems = allItems,
                selectedDate = resolvedSelectedDate,
                selectedDay = resolvedSelectedDate?.let { d -> resolveDayNameForDate(d) }.orEmpty(),
                selectedDateFromPlan = false
            )
        }
        updateMealSections()
    }

    private fun updateFavoriteInState(recipeId: Int, isFavorite: Boolean) {
        _uiState.update { currentState ->
            val updatedCurrentMenu = currentState.currentMenu?.copy(
                items = currentState.currentMenu.items?.map {
                    if (it.recipe == recipeId) it.copy(is_favorite = isFavorite) else it
                }
            )
            val updatedAllMenuItems = currentState.allMenuItems.map {
                if (it.recipe == recipeId) it.copy(is_favorite = isFavorite) else it
            }
            currentState.copy(
                currentMenu = updatedCurrentMenu,
                allMenuItems = updatedAllMenuItems
            )
        }
        updateMealSections()
    }

    fun reloadMenu() {
        viewModelScope.launch {
            MenuRepository.clearCache(getApplication())
            loadData()
            com.example.smartmeal.data.manager.MenuUpdateManager.notifyMenuChanged()
        }
    }

    fun setActiveSlot(mealType: String) {
        com.example.smartmeal.data.manager.MealSlotManager.setActiveMealType(mealType)
    }

    fun regenerateMenuForCurrentPlan() {
        val request = resolveStoredPlanGeneration(
            planType = preferences.getPlanType(),
            selectedPlanDateMillis = preferences.getSelectedPlanDate(),
            customRange = preferences.getCustomPlanRange()
        )

        if (request.startDate == null) {
            reloadMenu()
            return
        }

        generateMenu(
            planType = request.planType,
            selectedPlanDate = request.startDate,
            customDays = request.customDays
        )
    }

    fun generateMenu(
        planType: String?,
        selectedPlanDate: Date?,
        customDays: Int? = null,
        totalCalories: Int? = null,
        mealCalories: Map<String, Int>? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val type = planType ?: SetupPreferences.PLAN_TYPE_WEEKLY
                val periodStr = when (type) {
                    SetupPreferences.PLAN_TYPE_CUSTOM -> "custom"
                    SetupPreferences.PLAN_TYPE_DAILY -> "day"
                    else -> "week"
                }
                val startDateStr = resolveGenerationStartDateString(
                    formatter = apiDateFormatter, selectedPlanDate = selectedPlanDate
                )

                val breakfastTime = preferences.getMealCookTime("Завтрак")
                val lunchTime = preferences.getMealCookTime("Обед")
                val dinnerTime = preferences.getMealCookTime("Ужин")

                val cookTimesMap = mutableMapOf<String, String>()
                if (breakfastTime != null && breakfastTime != "any") cookTimesMap["Завтрак"] = breakfastTime
                if (lunchTime != null && lunchTime != "any") cookTimesMap["Обед"] = lunchTime
                if (dinnerTime != null && dinnerTime != "any") cookTimesMap["Ужин"] = dinnerTime

                val dietType = preferences.getDietType()
                val allergies = preferences.getAllergies()
                val caloriesEnabled = preferences.isCaloriesEnabled()

                val response = generatorApi.autoGenerate(
                    AutoGenerateRequest(
                        period = periodStr,
                        start_date = startDateStr,
                        days = customDays,
                        diet_type = dietType,
                        exclude_allergies = allergies,
                        cook_times = if (cookTimesMap.isEmpty()) null else cookTimesMap,
                        total_calories = if (caloriesEnabled) totalCalories ?: preferences.getTotalCalories() else null,
                        calorie_margin = if (caloriesEnabled) preferences.getCalorieMargin() else null,
                        meal_calories = if (caloriesEnabled) mealCalories ?: preferences.getAllMealCalories() else null
                    )
                )
                if (response.isSuccessful) {
                    val newMenuId = response.body()?.id
                    if (newMenuId != null) {
                        try {
                            menuApi.recalculateCart(com.example.smartmeal.feature.home.data.api.RecalculateCartRequest(menu_id = newMenuId))
                        } catch (e: Exception) {}
                    }
                    MenuRepository.clearCache(getApplication())
                    loadData()
                    com.example.smartmeal.data.manager.MenuUpdateManager.notifyMenuChanged()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = try {
                        val json = org.json.JSONObject(errorBody ?: "{}")
                        json.optString("detail", "Ошибка генерации")
                    } catch (e: Exception) { "Ошибка сервера: ${response.code()}" }
                    _uiState.update { it.copy(isLoading = false, error = message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun updateMealSections() {
        val state = _uiState.value
        val menu = state.currentMenu ?: return
        try {
            val resolvedDate = state.selectedDate ?: buildAvailableDates(menu.items ?: emptyList(), state.customPlan).firstOrNull()

            if (state.selectedDate == null && resolvedDate != null) {
                com.example.smartmeal.data.manager.DateManager.notifyDateSelected(resolvedDate)
            }

            val itemsForDay = if (resolvedDate != null) {
                val selectedDateStr = apiDateFormatter.format(resolvedDate)
                val currentMenuItems = menu.items?.filter { it.actual_date == selectedDateStr } ?: emptyList()

                val sourceItems = if (currentMenuItems.isNotEmpty()) {
                    currentMenuItems
                } else if (state.selectedDateFromPlan) {
                    state.allMenuItems.filter { it.actual_date == selectedDateStr }
                } else {
                    emptyList()
                }

                val uniqueItems = sourceItems.sortedWith(
                    compareByDescending<MenuItemDto> { it.menu ?: 0 }
                        .thenByDescending { it.id }
                ).distinctBy { it.meal_type.lowercase(Locale.US) }

                com.example.smartmeal.data.manager.MenuSyncManager.updateMenuForDate(
                    selectedDateStr,
                    uniqueItems.map { com.example.smartmeal.data.manager.RecipeInMenu(it.recipe, it.meal_type) }.toSet()
                )
                uniqueItems
            } else emptyList()

            val activeMealType = com.example.smartmeal.data.manager.MealSlotManager.getActiveMealType()

            val mealSections = itemsForDay.map { item ->
                val title = when (item.meal_type.lowercase(Locale.US)) {
                    "breakfast", "завтрак" -> "Завтрак"
                    "lunch", "обед" -> "Обед"
                    "dinner", "ужин" -> "Ужин"
                    else -> item.meal_type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
                val updatedItem = item.copy(is_active = item.meal_type.lowercase(Locale.US) == activeMealType?.lowercase(Locale.US))
                MealSection(id = item.meal_type, title = title, meal = updatedItem)
            }.sortedBy { section ->
                when(section.id.lowercase(Locale.US)) {
                    "breakfast", "завтрак" -> 1
                    "lunch", "обед" -> 2
                    "dinner", "ужин" -> 3
                    else -> 4
                }
            }

            _uiState.update { it.copy(
                mealSections = mealSections,
                selectedDate = resolvedDate,
                selectedDay = resolvedDate?.let { d -> resolveDayNameForDate(d) }.orEmpty()
            ) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Ошибка обработки даты: ${e.localizedMessage}") }
        }
    }

    fun toggleFavorite(mealId: Int) {
        val state = _uiState.value
        val menuItem = state.currentMenu?.items?.find { it.id == mealId }
            ?: state.allMenuItems.find { it.id == mealId }
            ?: return

        val recipeId = menuItem.recipe
        val mealType = menuItem.meal_type

        viewModelScope.launch {
            try {
                val response = menuApi.toggleFavorite(com.example.smartmeal.feature.home.data.api.ToggleFavoriteRequest(
                    recipe = recipeId,
                    meal_type = mealType
                ))
                if (response.isSuccessful) {
                    val isFavorite = response.body()?.is_favorite ?: false
                    updateFavoriteInState(recipeId, isFavorite)
                    com.example.smartmeal.data.manager.FavoritesManager.notifyFavoriteChanged(recipeId, isFavorite)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка при изменении избранного: ${e.localizedMessage}") }
            }
        }
    }

    fun selectDate(date: Date, customPlan: CustomPlan? = null, notifyManager: Boolean = true) {
        val normalized = normalizeDateStatic(date)
        if (customPlan != null) {
            val start = normalizeDateStatic(customPlan.startDate)
            val end = normalizeDateStatic(customPlan.endDate)
            if (normalized.before(start) || normalized.after(end)) return
        }
        val calendar = Calendar.getInstance().apply { time = normalized }
        val dayIndex = when(calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6; else -> 0
        }
        if (com.example.smartmeal.data.manager.MealSlotManager.getActiveMealType() == null) {
            com.example.smartmeal.data.manager.MealSlotManager.setActiveMealType("breakfast")
        }
        _uiState.update { it.copy(selectedDay = dayNames[dayIndex], selectedDate = normalized, selectedDateFromPlan = customPlan != null) }
        updateMealSections()

        if (notifyManager) {
            com.example.smartmeal.data.manager.DateManager.notifyDateSelected(normalized)
        }
    }

    fun replaceMeal(mealId: Int) {
        val state = _uiState.value
        val menuItem = state.allMenuItems.find { it.id == mealId } ?: state.currentMenu?.items?.find { it.id == mealId } ?: return
        val oldRecipeId = menuItem.recipe
        val mealType = menuItem.meal_type
        val dateStr = menuItem.actual_date
        
        com.example.smartmeal.data.manager.MealSlotManager.setActiveMealType(mealType)

        viewModelScope.launch {
            try {
                val russianMealName = when (mealType.lowercase(Locale.US)) {
                    "breakfast", "завтрак" -> "Завтрак"
                    "lunch", "обед" -> "Обед"
                    "dinner", "ужин" -> "Ужин"
                    else -> mealType
                }
                val cookTimeRange = preferences.getMealCookTime(russianMealName).takeIf { it != "any" }
                val caloriesEnabled = preferences.isCaloriesEnabled()
                val totalCalories = if (caloriesEnabled) preferences.getTotalCalories() else null
                val mealCalories = if (caloriesEnabled) preferences.getAllMealCalories() else null
                val calorieMargin = if (caloriesEnabled) preferences.getCalorieMargin() else null

                val updatedItem = menuRepository.replaceMenuItem(
                    menuItemId = mealId,
                    cookTimeRange = cookTimeRange,
                    totalCalories = totalCalories,
                    mealCalories = mealCalories,
                    calorieMargin = calorieMargin
                )
                if (updatedItem != null) {
                    preferences.clearMenuItemServings(updatedItem.id)
                    com.example.smartmeal.data.manager.MenuSyncManager.replaceRecipeInState(dateStr, oldRecipeId, updatedItem.recipe)
                    
                    MenuRepository.updateMenuItemInCache(updatedItem)
                    
                    // ОБНОВЛЯЕМ СОСТОЯНИЕ ВЕЗДЕ
                    _uiState.update { currentState ->
                        val updatedAll = currentState.allMenuItems.map { if (it.id == updatedItem.id) updatedItem else it }
                        val updatedCurrentMenu = currentState.currentMenu?.copy(
                            items = currentState.currentMenu.items?.map { if (it.id == updatedItem.id) updatedItem else it }
                        )
                        currentState.copy(
                            allMenuItems = updatedAll,
                            currentMenu = updatedCurrentMenu
                        )
                    }
                    
                    updateMealSections()
                    com.example.smartmeal.data.manager.MenuUpdateManager.notifyMenuChanged()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}

private fun normalizeDateStatic(date: Date): Date {
    val cal = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return cal.time
}

private fun resolveDayNameForDate(date: Date): String {
    val calendar = Calendar.getInstance().apply { time = date }
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Пн"; Calendar.TUESDAY -> "Вт"; Calendar.WEDNESDAY -> "Ср"
        Calendar.THURSDAY -> "Чт"; Calendar.FRIDAY -> "Пт"; Calendar.SATURDAY -> "Сб"
        Calendar.SUNDAY -> "Вс"; else -> ""
    }
}

internal data class StoredPlanGeneration(
    val planType: String?,
    val startDate: Date?,
    val customDays: Int?
)

internal fun resolveStoredPlanGeneration(
    planType: String?,
    selectedPlanDateMillis: Long?,
    customRange: Pair<Long, Long>?
): StoredPlanGeneration {
    return when (planType) {
        SetupPreferences.PLAN_TYPE_CUSTOM -> StoredPlanGeneration(
            planType = planType,
            startDate = customRange?.first?.let(::Date),
            customDays = resolveCustomDays(customRange)
        )
        SetupPreferences.PLAN_TYPE_WEEKLY,
        SetupPreferences.PLAN_TYPE_DAILY -> StoredPlanGeneration(
            planType = planType,
            startDate = selectedPlanDateMillis?.let(::Date),
            customDays = null
        )
        else -> StoredPlanGeneration(
            planType = planType,
            startDate = selectedPlanDateMillis?.let(::Date),
            customDays = null
        )
    }
}

internal fun resolveGenerationStartDateString(
    formatter: SimpleDateFormat,
    selectedPlanDate: Date?,
    fallbackDate: Date = Date()
): String = formatter.format(selectedPlanDate ?: fallbackDate)
