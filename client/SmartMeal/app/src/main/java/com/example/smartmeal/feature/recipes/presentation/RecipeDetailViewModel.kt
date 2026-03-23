package com.example.smartmeal.feature.recipes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class RecipeDetailViewModel(private val api: RecipeApi) : ViewModel() {

    private val _state = MutableStateFlow(RecipeDetailState())
    val state: StateFlow<RecipeDetailState> = _state.asStateFlow()

    private var currentRecipeId: Int = -1 // объявление переменной id текущего рецепта

    fun loadRecipe(recipeId: Int, servings: Int? = null) {
        currentRecipeId = recipeId // сохранение id текущего рецепта
        val isInitial = _state.value.recipe == null  // первичная загрузка если рецепта ещё нет
        viewModelScope.launch {
            if (isInitial) {
                _state.update { it.copy(isLoading = true, error = null) }
            } else {
                _state.update { it.copy(isLoading = false, error = null) }
            }
            try {
                val response = api.getRecipeDetail(recipeId, servings)
                if (response.isSuccessful) {
                    _state.update { it.copy(
                        isLoading = false,
                        recipe = response.body(),
                        currentServings = servings ?: it.currentServings // текущее количество порций в состоянии
                    )}
                } else {
                    _state.update { it.copy(isLoading = false, error = "Ошибка: ${response.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Неизвестная ошибка") }
            }
        }
    }

    fun changeServings(servings: Int) { // изменить количество порций
        loadRecipe(currentRecipeId, servings)
    }
}
