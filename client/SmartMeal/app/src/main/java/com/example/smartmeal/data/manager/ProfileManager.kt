package com.example.smartmeal.data.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Synchronizes profile updates across screens.
 * Global portion size, diet and allergies should refresh dependent UI.
 */
object ProfileManager {
    data class ProfileUpdate(
        val portionSize: Int,
        val dietTypeId: Int?,
        val allergyIds: Set<Int>,
    )

    private val _profileUpdates = MutableSharedFlow<ProfileUpdate>(extraBufferCapacity = 1)
    val profileUpdates = _profileUpdates.asSharedFlow()

    fun notifyProfileChanged(
        portionSize: Int,
        dietTypeId: Int?,
        allergyIds: Set<Int>,
    ) {
        _profileUpdates.tryEmit(
            ProfileUpdate(
                portionSize = portionSize,
                dietTypeId = dietTypeId,
                allergyIds = allergyIds,
            )
        )
    }
}
