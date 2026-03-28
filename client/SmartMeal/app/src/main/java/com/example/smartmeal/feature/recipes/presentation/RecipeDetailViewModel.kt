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
    val currentServings: Int = 1
)

class RecipeDetailViewModel(
    private val api: RecipeApi,
    private val preferences: SetupPreferences? = null
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeDetailState())
    val state: StateFlow<RecipeDetailState> = _state.asStateFlow()

    private var currentRecipeId: Int = -1
    private var currentMenuItemId: Int? = null

    fun loadRecipe(recipeId: Int, menuItemId: Int?, servings: Int? = null) {
        currentRecipeId = recipeId
        currentMenuItemId = menuItemId
        
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
                    _state.update { it.copy(
                        isLoading = false,
                        recipe = response.body(),
                        currentServings = effectiveServings
                    )}
                } else {
                    _state.update { it.copy(isLoading = false, error = "Ошибка: ${response.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Неизвестная ошибка") }
            }
        }
    }

    fun changeServings(servings: Int) {
        // Сохраняем индивидуальную настройку для конкретного блюда
        currentMenuItemId?.let { preferences?.setMenuItemServings(it, servings) }
        loadRecipe(currentRecipeId, currentMenuItemId, servings)
    }
}
