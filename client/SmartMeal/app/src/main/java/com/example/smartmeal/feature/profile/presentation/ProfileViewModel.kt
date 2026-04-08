package com.example.smartmeal.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.setup.data.api.SetupApi
import com.example.smartmeal.feature.setup.data.models.AllergyDto
import com.example.smartmeal.feature.setup.data.models.DietTypeDto
import com.example.smartmeal.feature.setup.data.models.UpdateProfileRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isRegenerating: Boolean = false,
    val error: String? = null,
    val savedSuccess: Boolean = false,

    // Данные профиля с сервера
    val userName: String = "admin",
    val userEmail: String = "",
    val birthDate: String = "",
    val gender: String? = null,
    val currentDietTypeId: Int? = null,
    val currentDietTypeName: String? = null,
    val currentAllergyIds: Set<Int> = emptySet(),
    val currentAllergyNames: List<String> = emptyList(),
    val portionSize: Int = 1,
    val preferredCookTime: String? = null,
    val mealCookTimes: Map<String, String> = emptyMap(),

    // Справочники
    val allDietTypes: List<DietTypeDto> = emptyList(),
    val allAllergies: List<AllergyDto> = emptyList(),

    // Временные выборы (до нажатия "Подтвердить")
    val pendingAllergyIds: Set<Int> = emptySet(),
    val pendingDietTypeId: Int? = null,
    val pendingPortionSize: Int = 1,
    val pendingUserName: String = "",
    val pendingBirthDate: String? = null,
    val pendingGender: String? = null,
    val usernameError: String? = null,

    // Избранное
    val favorites: List<com.example.smartmeal.feature.home.data.api.UserFavoriteDto> = emptyList(),
    val recipeIdsInMenuOnSelectedDay: Set<Int> = emptySet(),
    val menuItemsOnSelectedDay: List<com.example.smartmeal.feature.home.data.menu.MenuItemDto> = emptyList()
)

fun ProfileState.getGroupedFavorites(): Map<String, List<com.example.smartmeal.feature.home.data.api.UserFavoriteDto>> {
    val order = listOf("Завтрак", "Обед", "Ужин", "Перекус", "Напитки")
    val grouped = mutableMapOf<String, MutableList<com.example.smartmeal.feature.home.data.api.UserFavoriteDto>>()
    
    favorites.forEach { fav ->
        // Проверяем русские названия из бэкенда
        val type = order.find { type -> 
            fav.meal_types.any { it.equals(type, ignoreCase = true) || it.contains(type, ignoreCase = true) } 
        } ?: "Другое"
        
        grouped.getOrPut(type) { mutableListOf() }.add(fav)
    }
    
    val sortedLabels = order + "Другое"
    return sortedLabels.mapNotNull { label ->
        grouped[label]?.let { label to it }
    }.toMap()
}

class ProfileViewModel(
    private val api: SetupApi,
    private val preferences: SetupPreferences,
    // Колбэк для полной перегенерации (диета, аллергии)
    private val onProfileSettingsChanged: () -> Unit = {},
    // Колбэк для легкого обновления данных (плюсик)
    private val onMenuManualChanged: () -> Unit = {}
) : ViewModel() {

    private val menuApi = com.example.smartmeal.data.api.RetrofitClient.createService(com.example.smartmeal.feature.home.data.api.MenuApi::class.java)
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadProfile()
        loadFavorites()
        viewModelScope.launch {
            com.example.smartmeal.data.manager.FavoritesManager.favoriteUpdates.collect {
                loadFavorites()
            }
        }
        
        // СИНХРОНИЗАЦИЯ: Следим за изменением выбранной даты
        viewModelScope.launch {
            com.example.smartmeal.data.manager.DateManager.dateUpdates.collect { date ->
                updateMenuStateForDate(date)
            }
        }

        // СИНХРОНИЗАЦИЯ: Следим за изменениями в глобальном менеджере меню
        viewModelScope.launch {
            com.example.smartmeal.data.manager.MenuSyncManager.menuState.collect { _ ->
                val currentDate = com.example.smartmeal.data.manager.DateManager.getLastSelectedDate() ?: 
                                 preferences.getSelectedPlanDate()?.let { java.util.Date(it) }
                currentDate?.let { updateMenuStateForDate(it) }
            }
        }
    }

    private fun updateMenuStateForDate(date: java.util.Date) {
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(date)
        
        // 1. Обновляем набор ID для быстрой отрисовки иконок
        val recipeIds = com.example.smartmeal.data.manager.MenuSyncManager.getRecipeIdsForDate(dateStr)
        _state.update { it.copy(recipeIdsInMenuOnSelectedDay = recipeIds) }
        
        // 2. Фоново подгружаем актуальные объекты MenuItem, чтобы знать их ID и типы (для будущих нажатий на +)
        viewModelScope.launch {
            try {
                val response = menuApi.getMenuItems()
                if (response.isSuccessful) {
                    val items = response.body()?.filter { it.actual_date == dateStr } ?: emptyList()
                    _state.update { it.copy(menuItemsOnSelectedDay = items) }
                }
            } catch (e: Exception) {}
        }
    }

    private fun checkRecipesInMenu() {
        // Метод оставлен для совместимости, но теперь мы используем MenuSyncManager
        val currentDate = com.example.smartmeal.data.manager.DateManager.getLastSelectedDate() ?: 
                         preferences.getSelectedPlanDate()?.let { java.util.Date(it) }
        currentDate?.let { updateMenuStateForDate(it) }
    }

    fun addToMenu(recipeId: Int) {
        val selectedDate = com.example.smartmeal.data.manager.DateManager.getLastSelectedDate() ?: return
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(selectedDate)

        viewModelScope.launch {
            try {
                // 1. Загружаем свежие пункты меню
                val itemsRes = menuApi.getMenuItems()
                if (!itemsRes.isSuccessful) {
                    _state.update { it.copy(error = "Ошибка загрузки меню: ${itemsRes.code()}") }
                    return@launch
                }
                
                // 2. Фильтруем слоты СТРОГО по выбранной дате (как в HomeScreen)
                val allItemsOnDate = itemsRes.body()?.filter { it.actual_date == dateStr } ?: emptyList()
                
                if (allItemsOnDate.isEmpty()) {
                    _state.update { it.copy(error = "На $dateStr нет приемов пищи в плане") }
                    return@launch
                }

                val favorite = _state.value.favorites.find { it.recipe == recipeId } ?: return@launch
                val recipeMealTypes = favorite.meal_types.map { it.lowercase(java.util.Locale.US) }
                
                // 3. Ищем подходящий слот по типу (Обед к Обеду) среди слотов этой даты
                // Сортируем по ID убыванию, чтобы взять самый актуальный слот (если их несколько)
                var targetSlot = allItemsOnDate.sortedByDescending { it.id }.find { item ->
                    val slotType = item.meal_type.lowercase(java.util.Locale.US)
                    recipeMealTypes.any { rt -> 
                        slotType.contains(rt) || rt.contains(slotType) ||
                        (slotType == "lunch" && rt.contains("обед")) ||
                        (slotType == "breakfast" && rt.contains("завтрак")) ||
                        (slotType == "dinner" && rt.contains("ужин"))
                    }
                }
                
                // Fallback: берем любой первый слот этой даты
                if (targetSlot == null) {
                    targetSlot = allItemsOnDate.maxByOrNull { it.id }
                }
                
                if (targetSlot != null) {
                    val oldRecipeId = targetSlot.recipe
                    
                    // СИНХРОНИЗАЦИЯ: Оптимистичное обновление UI
                    com.example.smartmeal.data.manager.MenuSyncManager.replaceRecipeInState(
                        dateStr, oldRecipeId, recipeId
                    )

                    val replaceRes = menuApi.setRecipeToMenuItem(
                        targetSlot.id, 
                        com.example.smartmeal.feature.home.data.api.SetRecipeRequest(recipeId)
                    )
                    
                    if (replaceRes.isSuccessful) {
                        preferences.clearMenuItemServings(targetSlot.id)
                        
                        onMenuManualChanged() 
                        updateMenuStateForDate(selectedDate)
                        
                        try {
                            menuApi.recalculateCart(com.example.smartmeal.feature.home.data.api.RecalculateCartRequest())
                        } catch (e: Exception) {}
                    } else {
                        // ОТКАТ ПРИ ОШИБКЕ
                        com.example.smartmeal.data.manager.MenuSyncManager.replaceRecipeInState(
                            dateStr, recipeId, oldRecipeId
                        )
                        _state.update { it.copy(error = "Ошибка сервера: ${replaceRes.code()}") }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Ошибка связи: ${e.localizedMessage}") }
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val userDeferred = async { api.getCurrentUser() }
                val dietsDeferred = async { api.getDietTypes() }
                val allergiesDeferred = async { api.getAllergies() }

                val userResp = userDeferred.await()
                val dietsResp = dietsDeferred.await()
                val allergiesResp = allergiesDeferred.await()

                val user = userResp.body()
                val diets = if (dietsResp.isSuccessful) dietsResp.body() ?: emptyList() else emptyList()
                val allergies = if (allergiesResp.isSuccessful) allergiesResp.body() ?: emptyList() else emptyList()

                _state.update {
                    it.copy(
                        isLoading = false,
                        userName = user?.username ?: "Admin",
                        userEmail = user?.email ?: "",
                        birthDate = user?.birth_date ?: "",
                        gender = user?.gender,
                        currentDietTypeId = user?.diet_type,
                        currentDietTypeName = user?.diet_type_name,
                        currentAllergyIds = user?.allergies?.toSet() ?: emptySet(),
                        currentAllergyNames = user?.allergies_names ?: emptyList(),
                        portionSize = user?.portion_size ?: 1,
                        preferredCookTime = user?.preferred_cook_time,
                        mealCookTimes = preferences.getAllMealCookTimes(),
                        allDietTypes = diets,
                        allAllergies = allergies,
                        // pending
                        pendingAllergyIds = user?.allergies?.toSet() ?: emptySet(),
                        pendingDietTypeId = user?.diet_type,
                        pendingPortionSize = user?.portion_size ?: 1,
                        pendingUserName = user?.username ?: "",
                        pendingBirthDate = user?.birth_date,
                        pendingGender = user?.gender
                    )
                }
                user?.portion_size?.let { preferences.setPortionSize(it) }
                preferences.setDietType(user?.diet_type)
                preferences.setAllergies(user?.allergies ?: emptyList())
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Ошибка загрузки: ${e.message}") }
            }
        }
    }

    fun updatePendingUserName(name: String) {
        _state.update { it.copy(pendingUserName = name, usernameError = null) }
    }

    fun updatePendingBirthDate(date: String?) {
        _state.update { it.copy(pendingBirthDate = date) }
    }

    fun updatePendingGender(gender: String?) {
        _state.update { it.copy(pendingGender = gender) }
    }

    fun savePersonalData() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, savedSuccess = false) }
            try {
                val resp = api.updateProfile(
                    UpdateProfileRequest(
                        username = s.pendingUserName,
                        diet_type = s.currentDietTypeId,
                        portion_size = s.portionSize,
                        allergies = s.currentAllergyIds.toList(),
                        preferred_cook_time = s.preferredCookTime,
                        birth_date = s.pendingBirthDate,
                        gender = s.pendingGender
                    )
                )
                if (resp.isSuccessful) {
                    val body = resp.body()
                    _state.update {
                        it.copy(
                            isSaving = false,
                            savedSuccess = true,
                            userName = it.pendingUserName,
                            birthDate = it.pendingBirthDate ?: "",
                            gender = it.pendingGender,
                            usernameError = null
                        )
                    }
                } else {
                    val errorBody = resp.errorBody()?.string()
                    if (errorBody?.contains("username") == true) {
                        _state.update { it.copy(isSaving = false, usernameError = "Это имя уже занято, попробуйте другое") }
                    } else {
                        _state.update { it.copy(isSaving = false, error = "Ошибка сервера: ${resp.code()}") }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun setBirthDate(date: String) {
        if (date.length <= 10) {
            _state.update { it.copy(birthDate = date) }
            preferences.setBirthDate(date)
        }
    }

    fun saveMealCookTimes(mealTimes: Map<String, String>) {
        viewModelScope.launch {
            _state.update { it.copy(mealCookTimes = mealTimes) }
            mealTimes.forEach { (meal, time) ->
                preferences.setMealCookTime(meal, time)
            }
            // Справочник поддерживает три типа приема пищи
            listOf("Завтрак", "Обед", "Ужин").forEach { meal ->
                if (!mealTimes.containsKey(meal)) {
                    preferences.setMealCookTime(meal, "any")
                }
            }
        }
    }

    fun confirmCookTimes() {
        onProfileSettingsChanged()
    }


    // РІвЂќР‚РІвЂќР‚РІвЂќР‚ Р ВР В·Р В±РЎР‚Р В°Р Р…Р Р…Р С•Р Вµ РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

    fun loadFavorites() {
        viewModelScope.launch {
            try {
                val resp = api.getFavorites()
                if (resp.isSuccessful) {
                    _state.update { it.copy(favorites = resp.body() ?: emptyList()) }
                }
            } catch (e: Exception) {
                // Р С›РЎв‚¬Р С‘Р В±Р С”РЎС“ Р С‘Р В·Р В±РЎР‚Р В°Р Р…Р Р…Р С•Р С–Р С• Р СР С•Р В¶Р Р…Р С• Р Р…Р Вµ Р С—Р С•Р С”Р В°Р В·РЎвЂ№Р Р†Р В°РЎвЂљРЎРЉ Р С”Р В°Р С” Р С”РЎР‚Р С‘РЎвЂћР СљР С‘РЎвЂЎР ВµРЎРѓР С”РЎС“РЎР‹
            }
        }
    }

    fun toggleFavorite(recipeId: Int) {
        viewModelScope.launch {
            try {
                val response = api.toggleFavorite(com.example.smartmeal.feature.home.data.api.ToggleFavoriteRequest(recipeId))
                if (response.isSuccessful) {
                    val isFavorite = response.body()?.is_favorite ?: false
                    loadFavorites()
                    // Р Р€Р Р†Р ВµР Т‘Р С•Р СР В»РЎРЏР ВµР С Р Т‘РЎР‚РЎС“Р С–Р С‘Р В© РЎРЊР С”РЎР‚Р В°Р Р…РЎвЂ№
                    com.example.smartmeal.data.manager.FavoritesManager.notifyFavoriteChanged(recipeId, isFavorite)
                }
            } catch (e: Exception) {
                // Р С›РЎв‚¬Р С‘Р В±Р С”РЎС“ Р С‘Р В·Р В±РЎР‚Р В°Р Р…Р Р…Р С•Р С–Р С• Р СР С•Р В¶Р Р…Р С• Р Р…Р Вµ Р С—Р С•Р С”Р В°Р В·РЎвЂ№Р Р†Р В°РЎвЂљРЎРЉ Р С”Р В°Р С” Р С”РЎР‚Р С‘РЎвЂћР СљР С‘РЎвЂЎР ВµРЎРѓР С”РЎС“РЎР‹
            }
        }
    }

    // РІвЂќР‚РІвЂќР‚РІвЂќР‚ Р С’Р В»Р В»Р ВµРЎР‚Р С–Р С‘Р С‘ РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

    fun togglePendingAllergy(id: Int) {
        _state.update {
            val updated = if (id in it.pendingAllergyIds) it.pendingAllergyIds - id
                          else it.pendingAllergyIds + id
            it.copy(pendingAllergyIds = updated)
        }
    }

    fun saveAllergies() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, savedSuccess = false) }
            try {
                val resp = api.updateProfile(
                    UpdateProfileRequest(
                        diet_type = s.currentDietTypeId,
                        portion_size = s.portionSize,
                        allergies = s.pendingAllergyIds.toList(),
                        preferred_cook_time = s.preferredCookTime
                    )
                )
                if (resp.isSuccessful) {
                    val body = resp.body()
                    preferences.setAllergies(s.pendingAllergyIds.toList())
                    _state.update {
                        it.copy(
                            isSaving = false,
                            savedSuccess = true,
                            currentAllergyIds = it.pendingAllergyIds,
                            currentAllergyNames = body?.allergies_names ?: it.currentAllergyNames,
                        )
                    }
                    com.example.smartmeal.data.manager.ProfileManager.notifyProfileChanged(
                        portionSize = s.portionSize,
                        dietTypeId = s.currentDietTypeId,
                        allergyIds = s.pendingAllergyIds
                    )
                    onProfileSettingsChanged() // Р СџР С•Р В»Р Р…Р В°РЎРЏ Р С—Р ВµРЎР‚Р ВµР С–Р ВµР Р…Р ВµРЎР‚Р В°РЎвЂ Р С‘РЎРЏ.
                } else {
                    _state.update { it.copy(isSaving = false, error = "Ошибка сервера: ${resp.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    // РІвЂќР‚РІвЂќР‚РІвЂќР‚ Р В Р В°РЎвЂ Р С‘Р С•Р Р… РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќРРІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

    fun selectPendingDiet(id: Int) {
        _state.update {
            // Р С—Р С•Р Р†РЎвЂљР С•РЎР‚Р Р…РЎвЂ№Р в„– РЎвЂљР В°Р С— РІР‚вЂќ РЎРѓР Р…Р С‘Р СР В°Р ВµР С Р Р†РЎвЂ№Р В±Р С•РЎР‚
            it.copy(pendingDietTypeId = if (it.pendingDietTypeId == id) null else id)
        }
    }

    fun saveDiet() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, savedSuccess = false) }
            try {
                val resp = api.updateProfile(
                    UpdateProfileRequest(
                        diet_type = s.pendingDietTypeId,
                        portion_size = s.portionSize,
                        allergies = s.currentAllergyIds.toList(),
                        preferred_cook_time = s.preferredCookTime
                    )
                )
                if (resp.isSuccessful) {
                    val body = resp.body()
                    preferences.setDietType(s.pendingDietTypeId)
                    _state.update {
                        it.copy(
                            isSaving = false,
                            savedSuccess = true,
                            currentDietTypeId = it.pendingDietTypeId,
                            currentDietTypeName = body?.diet_type_name,
                        )
                    }
                    com.example.smartmeal.data.manager.ProfileManager.notifyProfileChanged(
                        portionSize = s.portionSize,
                        dietTypeId = s.pendingDietTypeId,
                        allergyIds = s.currentAllergyIds
                    )
                    onProfileSettingsChanged() // Р СџР С•Р В»Р Р…Р В°РЎРЏ Р С—Р ВµРЎР‚Р ВµР С–Р ВµР Р…Р ВµРЎР‚Р В°РЎвЂ Р С‘РЎРЏ.
                } else {
                    _state.update { it.copy(isSaving = false, error = "Ошибка сервера: ${resp.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    // РІвЂќР‚РІвЂќР‚РІвЂќР‚ Р СџР С•РЎР‚РЎвЂ Р С‘Р С•Р Р… РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

    fun incrementPortion() {
        _state.update { it.copy(pendingPortionSize = (it.pendingPortionSize + 1).coerceAtMost(20)) }
    }

    fun decrementPortion() {
        _state.update { it.copy(pendingPortionSize = (it.pendingPortionSize - 1).coerceAtLeast(1)) }
    }

    fun savePortion() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, savedSuccess = false) }
            try {
                val resp = api.updateProfile(
                    UpdateProfileRequest(
                        diet_type = s.currentDietTypeId,
                        portion_size = s.pendingPortionSize,
                        allergies = s.currentAllergyIds.toList(),
                        preferred_cook_time = s.preferredCookTime
                    )
                )
                if (resp.isSuccessful) {
                    preferences.setPortionSize(s.pendingPortionSize)
                    _state.update {
                        it.copy(isSaving = false, savedSuccess = true, portionSize = it.pendingPortionSize)
                    }
                    com.example.smartmeal.data.manager.ProfileManager.notifyProfileChanged(
                        portionSize = s.pendingPortionSize,
                        dietTypeId = s.currentDietTypeId,
                        allergyIds = s.currentAllergyIds
                    )
                    onProfileSettingsChanged()
                } else {
                    _state.update { it.copy(isSaving = false, error = "Ошибка сервера: ${resp.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearSavedSuccess() {
        _state.update { it.copy(savedSuccess = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
