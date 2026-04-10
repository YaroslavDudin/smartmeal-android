package com.example.smartmeal.data.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Глобальный менеджер для уведомления об изменении состава меню (добавление, замена, удаление).
 */
object MenuUpdateManager {
    private val _menuUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val menuUpdates = _menuUpdates.asSharedFlow()

    /**
     * Вызывать при любом изменении состава меню на сервере.
     */
    fun notifyMenuChanged() {
        com.example.smartmeal.feature.home.data.MenuRepository.clearCache()
        _menuUpdates.tryEmit(Unit)
    }
}
