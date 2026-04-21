package com.example.smartmeal.feature.recipes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.home.data.menu.RecipeDetailDto
import com.example.smartmeal.feature.recipes.data.api.RecipeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeDetailState(
    val recipe: RecipeDetailDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentServings: Int = 1,
    val isInMenuOnSelectedDay: Boolean = false
)

class RecipeDetailViewModel(
    private val api: RecipeApi,
    private val menuApi: com.example.smartmeal.feature.home.data.api.MenuApi,
    private val preferences: SetupPreferences? = null
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeDetailState())
    val state: StateFlow<RecipeDetailState> = _state.asStateFlow()

    private var currentRecipeId: Int = -1
    private var currentMenuItemId: Int? = null
    private var currentMealType: String? = null

    init {
        viewModelScope.launch {
            com.example.smartmeal.data.manager.FavoritesManager.favoriteUpdates.collect { update ->
                if (update.recipeId == currentRecipeId) {
                    _state.update { it.copy(
                        recipe = it.recipe?.copy(is_favorite = update.isFavorite)
                    )}
                }
            }
        }
        
        // СИНХРОНИЗАЦИЯ: Следим за изменением выбранной даты
        viewModelScope.launch {
            com.example.smartmeal.data.manager.DateManager.dateUpdates.collect { _ ->
                if (currentRecipeId != -1) {
                    checkIfInMenu(currentRecipeId)
                }
            }
        }

        // СИНХРОНИЗАЦИЯ: Следим за изменениями в глобальном менеджере меню
        viewModelScope.launch {
            com.example.smartmeal.data.manager.MenuSyncManager.menuState.collect { _ ->
                if (currentRecipeId != -1) {
                    checkIfInMenu(currentRecipeId)
                }
            }
        }
    }

    fun loadRecipe(recipeId: Int, menuItemId: Int?, servings: Int? = null) {
        currentRecipeId = recipeId
        currentMenuItemId = menuItemId
        
        // Пытаемся найти тип приема пищи, если передан ID элемента меню
        if (menuItemId != null) {
            viewModelScope.launch {
                try {
                    val items = com.example.smartmeal.feature.home.data.MenuRepository.getMenuItemsCache()
                    currentMealType = items.find { it.id == menuItemId }?.meal_type
                } catch (e: Exception) {}
            }
        }

        // ПРИОРИТЕТ: 
        // 1. Сначала проверяем сохраненное переопределение для этого конкретного приема пищи
        // 2. Если его нет, берем переданный servings (глобальный portionSize из навигации)
        // 3. В крайнем случае - текущее состояние
        val savedOverride = menuItemId?.let { preferences?.getMenuItemServings(it) }?.takeIf { it > 0 }
        val effectiveServings = savedOverride ?: servings ?: _state.value.currentServings

        val isInitial = _state.value.recipe == null
        viewModelScope.launch {
            if (isInitial) {
                _state.update { it.copy(isLoading = true, error = null) }
            }
            try {
                val response = api.getRecipeDetail(recipeId, effectiveServings)
                if (response.isSuccessful) {
                    val recipe = response.body()
                    _state.update { it.copy(
                        isLoading = false,
                        recipe = recipe,
                        currentServings = effectiveServings
                    )}
                    checkIfInMenu(recipeId)
                } else {
                    _state.update { it.copy(isLoading = false, error = "Ошибка: ${response.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Неизвестная ошибка") }
            }
        }
    }

    private fun checkIfInMenu(recipeId: Int) {
        val selectedDate = com.example.smartmeal.data.manager.DateManager.getLastSelectedDate() ?: 
                          preferences?.getSelectedPlanDate()?.let { java.util.Date(it) } ?: return
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(selectedDate)
        
        val isInMenu = com.example.smartmeal.data.manager.MenuSyncManager.isRecipeInMenu(dateStr, recipeId)
        _state.update { it.copy(isInMenuOnSelectedDay = isInMenu) }
    }

    fun addToMenu() {
        val recipe = _state.value.recipe ?: return
        val selectedDate = com.example.smartmeal.data.manager.DateManager.getLastSelectedDate() ?: 
                          preferences?.getSelectedPlanDate()?.let { java.util.Date(it) } ?: return
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(selectedDate)
        
        viewModelScope.launch {
            try {
                // 1. Находим все элементы меню пользователя
                val itemsRes = menuApi.getMenuItems()
                if (!itemsRes.isSuccessful) return@launch
                val allItems = itemsRes.body() ?: emptyList()
                
                // 2. Ищем любой доступный слот на выбранную дату.
                val targetSlot = allItems.find { item ->
                    item.actual_date == dateStr
                }
                
                if (targetSlot != null) {
                    val oldRecipeId = targetSlot.recipe
                    
                    // СИНХРОНИЗАЦИЯ: Оптимистичное обновление UI (мгновенно!)
                    com.example.smartmeal.data.manager.MenuSyncManager.replaceRecipeInState(
                        dateStr, oldRecipeId, recipe.id
                    )

                    val replaceRes = menuApi.setRecipeToMenuItem(
                        targetSlot.id, 
                        com.example.smartmeal.feature.home.data.api.SetRecipeRequest(recipe.id)
                    )
                    
                    if (replaceRes.isSuccessful) {
                        preferences?.clearMenuItemServings(targetSlot.id)
                        com.example.smartmeal.data.manager.MenuUpdateManager.notifyMenuChanged()
                        try {
                            menuApi.recalculateCart(com.example.smartmeal.feature.home.data.api.RecalculateCartRequest())
                        } catch (e: Exception) {}
                    } else {
                        // ОТКАТ: Если сервер вернул ошибку
                        com.example.smartmeal.data.manager.MenuSyncManager.replaceRecipeInState(
                            dateStr, recipe.id, oldRecipeId
                        )
                        _state.update { it.copy(error = "Ошибка обновления") }
                    }
                } else {
                    _state.update { it.copy(error = "На дату $dateStr не найдено приемов пищи.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Ошибка связи") }
            }
        }
    }

    fun changeServings(servings: Int) {
        // Сохраняем индивидуальную настройку для конкретного блюда
        currentMenuItemId?.let { preferences?.setMenuItemServings(it, servings) }
        
        // СИНХРОНИЗАЦИЯ: Уведомляем другие экраны (например, Продукты), что настройки изменились
        if (preferences != null) {
            com.example.smartmeal.data.manager.ProfileManager.notifyProfileChanged(
                portionSize = preferences.getPortionSize(),
                dietTypeId = preferences.getDietType(),
                allergyIds = preferences.getAllergies().toSet()
            )
        }
        
        loadRecipe(currentRecipeId, currentMenuItemId, servings)
    }

    fun toggleFavorite() {
        val recipe = _state.value.recipe ?: return
        viewModelScope.launch {
            try {
                val response = api.toggleFavorite(com.example.smartmeal.feature.recipes.data.api.ToggleFavoriteRequest(
                    recipe = recipe.id,
                    meal_type = currentMealType
                ))
                if (response.isSuccessful) {
                    val isFavorite = response.body()?.is_favorite ?: false
                    _state.update { it.copy(
                        recipe = it.recipe?.copy(is_favorite = isFavorite)
                    )}
                    // Уведомляем другие экраны
                    com.example.smartmeal.data.manager.FavoritesManager.notifyFavoriteChanged(recipe.id, isFavorite)
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Ошибка: ${e.localizedMessage}") }
            }
        }
    }
}
