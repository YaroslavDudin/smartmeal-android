package com.example.smartmeal.data.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Информационная структура о рецепте в меню.
 */
data class RecipeInMenu(
    val recipeId: Int,
    val mealType: String
)

/**
 * Сеньор-решение для синхронизации состава меню между всеми экранами.
 * Хранит карту: Дата (yyyy-MM-dd) -> Набор рецептов (ID + Тип приема пищи).
 */
object MenuSyncManager {
    private val _menuState = MutableStateFlow<Map<String, Set<RecipeInMenu>>>(emptyMap())
    val menuState = _menuState.asStateFlow()

    /**
     * Обновить список рецептов для конкретной даты.
     */
    fun updateMenuForDate(date: String, recipes: Set<RecipeInMenu>) {
        _menuState.update { current ->
            current + (date to recipes)
        }
    }

    /**
     * Мгновенно (оптимистично) заменить один рецепт на другой в локальном стейте.
     */
    fun replaceRecipeInState(date: String, oldRecipeId: Int?, newRecipeId: Int, mealType: String? = null) {
        _menuState.update { current ->
            val currentSet = current[date] ?: emptySet()
            
            // Если mealType не передан, пытаемся найти его в существующем сете по oldRecipeId
            val resolvedMealType = mealType ?: currentSet.find { it.recipeId == oldRecipeId }?.mealType ?: ""
            
            val nextSet = currentSet.filter { it.recipeId != oldRecipeId }.toMutableSet()
            nextSet.add(RecipeInMenu(newRecipeId, resolvedMealType))
            
            current + (date to nextSet)
        }
    }

    /**
     * Проверить, есть ли рецепт в меню на указанную дату (в любом слоте).
     */
    fun isRecipeInMenu(date: String, recipeId: Int): Boolean {
        return _menuState.value[date]?.any { it.recipeId == recipeId } ?: false
    }

    /**
     * Проверить, есть ли рецепт в меню на указанную дату в КОНКРЕТНОМ слоте.
     */
    fun isRecipeInMenuSlot(date: String, recipeId: Int, mealType: String): Boolean {
        val normalizedType = mealType.lowercase(java.util.Locale.US)
        return _menuState.value[date]?.any { 
            it.recipeId == recipeId && (
                it.mealType.lowercase(java.util.Locale.US).contains(normalizedType) ||
                normalizedType.contains(it.mealType.lowercase(java.util.Locale.US)) ||
                (it.mealType.lowercase(java.util.Locale.US) == "lunch" && (normalizedType == "обед" || normalizedType == "lunch")) ||
                (it.mealType.lowercase(java.util.Locale.US) == "breakfast" && (normalizedType == "завтрак" || normalizedType == "breakfast")) ||
                (it.mealType.lowercase(java.util.Locale.US) == "dinner" && (normalizedType == "ужин" || normalizedType == "dinner"))
            )
        } ?: false
    }
    
    fun getRecipesForDate(date: String): Set<RecipeInMenu> {
        return _menuState.value[date] ?: emptySet()
    }
}
