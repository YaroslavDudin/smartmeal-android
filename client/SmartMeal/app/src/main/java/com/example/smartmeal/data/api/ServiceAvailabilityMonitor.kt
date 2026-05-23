package com.example.smartmeal.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServiceAvailabilityMonitor {
    private val _isUnavailable = MutableStateFlow(false)
    val isUnavailable: StateFlow<Boolean> = _isUnavailable.asStateFlow()

    fun reportUnavailable() {
        _isUnavailable.value = true
    }

    fun reportAvailable() {
        _isUnavailable.value = false
    }
}
