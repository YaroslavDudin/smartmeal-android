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
    val error: String? = null
)

class RecipeDetailViewModel(private val api: RecipeApi) : ViewModel() {

    private val _state = MutableStateFlow(RecipeDetailState())
    val state: StateFlow<RecipeDetailState> = _state.asStateFlow()

    fun loadRecipe(recipeId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.getRecipeDetail(recipeId)
                if (response.isSuccessful) {
                    _state.update { it.copy(isLoading = false, recipe = response.body()) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Ошибка: ${response.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Неизвестная ошибка") }
            }
        }
    }
}
