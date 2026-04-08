package com.example.smartmeal.data.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Сеньор-решение для синхронизации состава меню между всеми экранами.
 * Хранит карту: Дата (yyyy-MM-dd) -> Набор ID рецептов, которые в этот день в рационе.
 */
object MenuSyncManager {
    private val _menuState = MutableStateFlow<Map<String, Set<Int>>>(emptyMap())
    val menuState = _menuState.asStateFlow()

    /**
     * Обновить список рецептов для конкретной даты.
     */
    fun updateMenuForDate(date: String, recipeIds: Set<Int>) {
        _menuState.update { current ->
            current + (date to recipeIds)
        }
    }

    /**
     * Мгновенно (оптимистично) заменить один рецепт на другой в локальном стейте.
     */
    fun replaceRecipeInState(date: String, oldRecipeId: Int?, newRecipeId: Int) {
        _menuState.update { current ->
            val currentSet = current[date] ?: emptySet()
            val nextSet = (currentSet - (oldRecipeId ?: -1)) + newRecipeId
            current + (date to nextSet)
        }
    }

    /**
     * Проверить, есть ли рецепт в меню на указанную дату.
     */
    fun isRecipeInMenu(date: String, recipeId: Int): Boolean {
        return _menuState.value[date]?.contains(recipeId) ?: false
    }
    
    fun getRecipeIdsForDate(date: String): Set<Int> {
        return _menuState.value[date] ?: emptySet()
    }
}
