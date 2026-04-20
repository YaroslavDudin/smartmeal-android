package com.example.smartmeal.feature.recipes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.feature.recipes.data.api.RecipeApi
import com.example.smartmeal.feature.recipes.data.api.RecipeShortDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeListState(
    val recipes: List<RecipeShortDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val minCalories: Int? = null,
    val maxCalories: Int? = null,
    val selectedDietTypeId: Int? = null
)

class RecipeListViewModel(
    private val api: RecipeApi
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeListState())
    val state: StateFlow<RecipeListState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadRecipes()
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            loadRecipes()
        }
    }

    fun onCaloriesChange(min: Int?, max: Int?) {
        _state.update { it.copy(minCalories = min, maxCalories = max) }
        loadRecipes()
    }

    fun onDietTypeChange(dietTypeId: Int?) {
        _state.update { it.copy(selectedDietTypeId = dietTypeId) }
        loadRecipes()
    }

    fun loadRecipes() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val s = _state.value
                val response = api.getRecipes(
                    search = s.searchQuery.takeIf { it.isNotBlank() },
                    dietType = s.selectedDietTypeId,
                    minCalories = s.minCalories,
                    maxCalories = s.maxCalories
                )
                if (response.isSuccessful) {
                    _state.update { it.copy(
                        isLoading = false,
                        recipes = response.body()?.results ?: emptyList()
                    )}
                } else {
                    _state.update { it.copy(isLoading = false, error = "Ошибка: ${response.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }
}
