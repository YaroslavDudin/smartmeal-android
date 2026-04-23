package com.example.smartmeal.data.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Менеджер для отслеживания активного слота (приема пищи) в меню.
 * Используется для корректного добавления блюд из избранного в нужный слот.
 */
object MealSlotManager {
    // По умолчанию завтрак
    private val _activeMealType = MutableStateFlow<String?>("breakfast")
    val activeMealType = _activeMealType.asStateFlow()

    fun setActiveMealType(type: String?) {
        _activeMealType.value = type?.lowercase()
    }

    fun getActiveMealType(): String? = _activeMealType.value

    fun clear() {
        _activeMealType.value = "breakfast"
    }
}
