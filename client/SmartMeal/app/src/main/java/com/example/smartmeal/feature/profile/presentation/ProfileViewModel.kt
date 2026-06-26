package com.example.smartmeal.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.setup.data.api.SetupApi
import com.example.smartmeal.feature.setup.data.models.AllergyDto
import com.example.smartmeal.feature.setup.data.models.DietTypeDto
import com.example.smartmeal.feature.setup.data.models.UpdateProfileRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

data class ProfileState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isRegenerating: Boolean = false,
    val error: String? = null,
    val savedSuccess: Boolean = false,

    // Данные профиля с сервера
    val userName: String = "admin",
    val userEmail: String = "",
    val avatarUrl: String? = null,
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
    val recipesInMenuOnSelectedDay: Set<com.example.smartmeal.data.manager.RecipeInMenu> = emptySet(),
    val menuItemsOnSelectedDay: List<com.example.smartmeal.feature.home.data.menu.MenuItemDto> = emptyList(),
    
    // Калорийность
    val totalCalories: Int = 2000,
    val mealCalories: Map<String, Int> = emptyMap(),
    val proteinPercent: Int = 20,
    val fatPercent: Int = 30,
    val carbsPercent: Int = 50
)

fun ProfileState.getGroupedFavorites(): Map<String, List<com.example.smartmeal.feature.home.data.api.UserFavoriteDto>> {
    val order = listOf("Завтрак", "Обед", "Ужин", "Перекус", "Напитки")
    val grouped = mutableMapOf<String, MutableList<com.example.smartmeal.feature.home.data.api.UserFavoriteDto>>()
    
    favorites.forEach { fav ->
        // Сначала пытаемся сгруппировать по конкретному типу из записи избранного
        val specificType = fav.meal_type_name?.let { name ->
            order.find { it.equals(name, ignoreCase = true) || name.contains(it, ignoreCase = true) }
        }
        
        // Если в записи нет конкретного типа, ищем первый подходящий из типов самого рецепта
        val type = specificType ?: order.find { type -> 
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
    private val onCriticalSettingsChanged: () -> Unit = {},
    private val onSimpleSettingsChanged: () -> Unit = {},
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
        
        viewModelScope.launch {
            com.example.smartmeal.data.manager.DateManager.dateUpdates.collect { date ->
                updateMenuStateForDate(date)
            }
        }

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
        val recipes = com.example.smartmeal.data.manager.MenuSyncManager.getRecipesForDate(dateStr)
        _state.update { it.copy(recipesInMenuOnSelectedDay = recipes) }
    }

    fun addToMenu(recipeId: Int, preferredMealType: String? = null) {
        val selectedDate = com.example.smartmeal.data.manager.DateManager.getLastSelectedDate() ?: return
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(selectedDate)

        viewModelScope.launch {
            try {
                val itemsRes = menuApi.getMenuItems()
                if (!itemsRes.isSuccessful) {
                    _state.update { it.copy(error = "Ошибка загрузки меню: ${itemsRes.code()}") }
                    return@launch
                }
                
                val allItems = itemsRes.body() ?: emptyList()
                if (allItems.isEmpty()) {
                    _state.update { it.copy(error = "У вас еще нет блюд в плане") }
                    return@launch
                }

                // Находим ID последнего меню, чтобы обновлять только актуальные слоты
                val latestMenuId = allItems.maxOfOrNull { it.menu ?: 0 } ?: 0
                
                // Фильтруем: только текущая дата И только последнее меню
                val allItemsOnDate = allItems.filter { it.actual_date == dateStr && (it.menu ?: 0) == latestMenuId }
                    .sortedByDescending { it.id }
                    .distinctBy { it.meal_type.lowercase(java.util.Locale.US) }
                
                if (allItemsOnDate.isEmpty()) {
                    // Если в последнем меню нет слотов на эту дату, пробуем просто по дате
                    val fallbackItems = allItems.filter { it.actual_date == dateStr }
                        .sortedByDescending { it.id }
                        .distinctBy { it.meal_type.lowercase(java.util.Locale.US) }
                    
                    if (fallbackItems.isEmpty()) {
                        _state.update { it.copy(error = "На $dateStr нет приемов пищи в плане") }
                        return@launch
                    }
                }

                val favorite = _state.value.favorites.find { it.recipe == recipeId } ?: return@launch
                val recipeMealTypes = favorite.meal_types.map { it.lowercase(java.util.Locale.US) }
                
                // 1. Сначала пытаемся найти слот, соответствующий ПРЕДПОЧТИТЕЛЬНОМУ типу (из раздела избранного)
                var targetSlot = if (preferredMealType != null) {
                    val pref = preferredMealType.lowercase(java.util.Locale.US)
                    allItemsOnDate.find { item ->
                        val slotType = item.meal_type.lowercase(java.util.Locale.US)
                        // Проверяем совпадение слота с предпочтением
                        val matchesPref = slotType == pref || 
                                         (slotType == "lunch" && (pref == "обед" || pref == "lunch")) ||
                                         (slotType == "breakfast" && (pref == "завтрак" || pref == "breakfast")) ||
                                         (slotType == "dinner" && (pref == "ужин" || pref == "dinner"))
                        
                        // Если слот совпадает с предпочтением, берем его (даже если типы рецепта не совсем сходятся)
                        matchesPref
                    }
                } else null

                // 2. Если не нашли по предпочтению, ищем по АКТИВНОМУ слоту
                if (targetSlot == null) {
                    val activeType = com.example.smartmeal.data.manager.MealSlotManager.getActiveMealType()
                    targetSlot = allItemsOnDate.find { item ->
                        val slotType = item.meal_type.lowercase(java.util.Locale.US)
                        slotType == activeType
                    }
                }

                // 3. Если все еще не нашли, ищем любой подходящий по типам блюда
                if (targetSlot == null) {
                    targetSlot = allItemsOnDate.find { item ->
                        val slotType = item.meal_type.lowercase(java.util.Locale.US)
                        recipeMealTypes.any { rt -> 
                            slotType.contains(rt) || rt.contains(slotType) ||
                            (slotType == "lunch" && rt.contains("обед")) ||
                            (slotType == "breakfast" && rt.contains("завтрак")) ||
                            (slotType == "dinner" && rt.contains("ужин"))
                        }
                    }
                }
                
                // 4. Совсем крайний случай - берем первый попавшийся слот на эту дату
                if (targetSlot == null) {
                    targetSlot = allItemsOnDate.firstOrNull()
                }
                
                if (targetSlot != null) {
                    val oldRecipeId = targetSlot.recipe
                    
                    val replaceRes = menuApi.setRecipeToMenuItem(
                        targetSlot.id, 
                        com.example.smartmeal.feature.home.data.api.SetRecipeRequest(recipeId)
                    )
                    
                    if (replaceRes.isSuccessful) {
                        // Сначала обновляем локальный стейт синхронизации
                        com.example.smartmeal.data.manager.MenuSyncManager.replaceRecipeInState(
                            dateStr, oldRecipeId, recipeId
                        )
                        preferences.clearMenuItemServings(targetSlot.id)
                        
                        // Принудительно очищаем кэш репозитория
                        com.example.smartmeal.feature.home.data.MenuRepository.clearCache()
                        
                        // Уведомляем главный экран о необходимости обновления
                        onMenuManualChanged() 
                        
                        // Обновляем состояние "✓" в профиле
                        updateMenuStateForDate(selectedDate)
                    } else {
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
                val (userResult, dietsResult, allergiesResult) = supervisorScope {
                    val userDeferred = async { runCatching { api.getCurrentUser() } }
                    val dietsDeferred = async { runCatching { api.getDietTypes() } }
                    val allergiesDeferred = async { runCatching { api.getAllergies() } }

                    Triple(
                        userDeferred.await(),
                        dietsDeferred.await(),
                        allergiesDeferred.await()
                    )
                }

                val userResp = userResult.getOrThrow()
                val dietsResp = dietsResult.getOrThrow()
                val allergiesResp = allergiesResult.getOrThrow()

                val user = userResp.body()
                val diets = if (dietsResp.isSuccessful) dietsResp.body() ?: emptyList() else emptyList()
                val allergies = if (allergiesResp.isSuccessful) allergiesResp.body() ?: emptyList() else emptyList()

                _state.update {
                    it.copy(
                        isLoading = false,
                        userName = user?.username ?: "Admin",
                        userEmail = user?.email ?: "",
                        avatarUrl = user?.avatar,
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
                        pendingAllergyIds = user?.allergies?.toSet() ?: emptySet(),
                        pendingDietTypeId = user?.diet_type,
                        pendingPortionSize = user?.portion_size ?: 1,
                        pendingUserName = user?.username ?: "",
                        pendingBirthDate = user?.birth_date,
                        pendingGender = user?.gender,
                        totalCalories = user?.target_calories ?: preferences.getTotalCalories(),
                        mealCalories = preferences.getAllMealCalories(),
                        proteinPercent = user?.protein_percent ?: preferences.getProteinPercent(),
                        fatPercent = user?.fat_percent ?: preferences.getFatPercent(),
                        carbsPercent = user?.carbs_percent ?: preferences.getCarbsPercent()
                    )
                }
                user?.portion_size?.let { preferences.setPortionSize(it) }
                preferences.setDietType(user?.diet_type)
                preferences.setAllergies(user?.allergies ?: emptyList())
                preferences.setGender(user?.gender)
                user?.calories_enabled?.let { preferences.setCaloriesEnabled(it) }
                user?.target_calories?.let { preferences.setTotalCalories(it) }
                user?.calorie_margin?.let { preferences.setCalorieMargin(it) }
                if (user?.protein_percent != null && user.fat_percent != null && user.carbs_percent != null) {
                    preferences.setMacroPercents(user.protein_percent, user.fat_percent, user.carbs_percent)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Ошибка загрузки: ${e.message}") }
            }
        }
    }

    fun updateAvatar(inputStream: InputStream, fileName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val bytes = withContext(Dispatchers.IO) { inputStream.readBytes() }
                val requestFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("avatar", fileName, requestFile)

                val s = _state.value
                val usernamePart = s.userName.toRequestBody("text/plain".toMediaTypeOrNull())
                val dietTypePart = s.currentDietTypeId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val portionSizePart = s.portionSize.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val cookTimePart = s.preferredCookTime?.toRequestBody("text/plain".toMediaTypeOrNull())
                val birthDatePart = s.birthDate.toRequestBody("text/plain".toMediaTypeOrNull())
                val genderPart = s.gender?.toRequestBody("text/plain".toMediaTypeOrNull())
                
                val allergyParts = s.currentAllergyIds.map { id ->
                    MultipartBody.Part.createFormData("allergies", id.toString())
                }

                val resp = api.updateProfileWithAvatar(
                    username = usernamePart,
                    dietType = dietTypePart,
                    portionSize = portionSizePart,
                    preferredCookTime = cookTimePart,
                    birthDate = birthDatePart,
                    gender = genderPart,
                    allergies = allergyParts,
                    avatar = body
                )

                if (resp.isSuccessful) {
                    val user = resp.body()
                    _state.update { 
                        it.copy(
                            isSaving = false, 
                            avatarUrl = user?.avatar,
                            savedSuccess = true 
                        ) 
                    }
                } else {
                    _state.update { it.copy(isSaving = false, error = "Ошибка при загрузке фото: ${resp.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.localizedMessage) }
            } finally {
                withContext(Dispatchers.IO) { try { inputStream.close() } catch (e: Exception) {} }
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

    fun saveMealCookTimes(mealTimes: Map<String, String>) {
        viewModelScope.launch {
            _state.update { it.copy(mealCookTimes = mealTimes) }
            mealTimes.forEach { (meal, time) ->
                preferences.setMealCookTime(meal, time)
            }
            listOf("Завтрак", "Обед", "Ужин").forEach { meal ->
                if (!mealTimes.containsKey(meal)) {
                    preferences.setMealCookTime(meal, "any")
                }
            }
            onCriticalSettingsChanged()
        }
    }

    fun confirmCookTimes() {
        onCriticalSettingsChanged()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            try {
                val resp = api.getFavorites()
                if (resp.isSuccessful) {
                    _state.update { it.copy(favorites = resp.body() ?: emptyList()) }
                }
            } catch (e: Exception) {}
        }
    }

    fun toggleFavorite(recipeId: Int, mealType: String? = null) {
        // Если mealType не передан (например, нажали в общем списке), 
        // пробуем найти уже существующую запись избранного для этого рецепта
        val resolvedMealType = mealType ?: _state.value.favorites.find { it.recipe == recipeId }?.meal_type_name
        
        viewModelScope.launch {
            try {
                val response = api.toggleFavorite(com.example.smartmeal.feature.home.data.api.ToggleFavoriteRequest(
                    recipe = recipeId,
                    meal_type = resolvedMealType
                ))
                if (response.isSuccessful) {
                    val isFavorite = response.body()?.is_favorite ?: false
                    loadFavorites()
                    com.example.smartmeal.data.manager.FavoritesManager.notifyFavoriteChanged(recipeId, isFavorite)
                }
            } catch (e: Exception) {}
        }
    }

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
                    onCriticalSettingsChanged()
                } else {
                    _state.update { it.copy(isSaving = false, error = "Ошибка сервера: ${resp.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun selectPendingDiet(id: Int) {
        _state.update {
            // Если нажали на уже выбранный — ничего не меняем (запрет деселекции)
            if (it.pendingDietTypeId == id) it
            else it.copy(pendingDietTypeId = id)
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
                    onCriticalSettingsChanged()
                } else {
                    _state.update { it.copy(isSaving = false, error = "Ошибка сервера: ${resp.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

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
                    onSimpleSettingsChanged()
                } else {
                    _state.update { it.copy(isSaving = false, error = "Ошибка сервера: ${resp.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun isCaloriesEnabled(): Boolean = preferences.isCaloriesEnabled()
    fun getCalorieMargin(): Int = preferences.getCalorieMargin()

    fun saveCalorieSettings(
        enabled: Boolean,
        total: Int,
        margin: Int,
        meals: Map<String, Int>,
        proteinPercent: Int = preferences.getProteinPercent(),
        fatPercent: Int = preferences.getFatPercent(),
        carbsPercent: Int = preferences.getCarbsPercent()
    ) {
        viewModelScope.launch {
            preferences.setCaloriesEnabled(enabled)
            preferences.setTotalCalories(total)
            preferences.setCalorieMargin(margin)
            preferences.setMacroPercents(proteinPercent, fatPercent, carbsPercent)
            meals.forEach { (type, cals) ->
                preferences.setMealCalories(type, cals)
            }
            _state.update {
                it.copy(
                    totalCalories = total,
                    mealCalories = meals,
                    proteinPercent = proteinPercent,
                    fatPercent = fatPercent,
                    carbsPercent = carbsPercent
                )
            }
            runCatching {
                api.updateProfile(
                    UpdateProfileRequest(
                        username = _state.value.userName,
                        diet_type = _state.value.currentDietTypeId,
                        portion_size = _state.value.portionSize,
                        allergies = _state.value.currentAllergyIds.toList(),
                        preferred_cook_time = _state.value.preferredCookTime,
                        birth_date = _state.value.birthDate.ifBlank { null },
                        gender = _state.value.gender,
                        calories_enabled = enabled,
                        target_calories = total,
                        calorie_margin = margin,
                        protein_percent = proteinPercent,
                        fat_percent = fatPercent,
                        carbs_percent = carbsPercent
                    )
                )
            }.onFailure { error ->
                _state.update { it.copy(error = "Не удалось синхронизировать цель с сервером: ${error.localizedMessage}") }
            }
            onCriticalSettingsChanged()
        }
    }

    fun clearSavedSuccess() {
        _state.update { it.copy(savedSuccess = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
