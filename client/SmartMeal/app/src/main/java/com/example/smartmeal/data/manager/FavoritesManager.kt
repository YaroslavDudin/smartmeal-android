package com.example.smartmeal.data.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Синхронизирует состояние избранного между различными экранами.
 */
object FavoritesManager {
    data class FavoriteUpdate(val recipeId: Int, val isFavorite: Boolean)

    private val _favoriteUpdates = MutableSharedFlow<FavoriteUpdate>(extraBufferCapacity = 1)
    val favoriteUpdates = _favoriteUpdates.asSharedFlow()

    fun notifyFavoriteChanged(recipeId: Int, isFavorite: Boolean) {
        _favoriteUpdates.tryEmit(FavoriteUpdate(recipeId, isFavorite))
    }
}
