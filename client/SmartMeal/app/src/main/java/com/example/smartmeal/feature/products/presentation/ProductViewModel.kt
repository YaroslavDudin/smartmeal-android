package com.example.smartmeal.feature.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.feature.home.data.MenuRepository
import com.example.smartmeal.feature.home.data.menu.CartCategoryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductUiState(
    val isLoading: Boolean = false,
    val categories: List<CartCategoryDto> = emptyList(),
    val error: String? = null
)

class ProductViewModel(private val repository: MenuRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val cart = repository.getCart()
                _uiState.update { it.copy(isLoading = false, categories = cart) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun toggleProductChecked(itemId: Int, isChecked: Boolean) {
        viewModelScope.launch {
            try {
                val success = repository.updateCartItem(itemId, isChecked = isChecked)
                if (success) {
                    // Локальное обновление для мгновенного отклика
                    _uiState.update { state ->
                        val updatedCategories = state.categories.map { category ->
                            category.copy(items = category.items.map { item ->
                                if (item.id == itemId) item.copy(is_checked = isChecked) else item
                            })
                        }
                        state.copy(categories = updatedCategories)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }
}
