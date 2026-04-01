package com.example.smartmeal.data.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Date

/**
 * Синхронизирует выбранную дату между различными экранами (Home, Products, Statistics).
 */
object DateManager {
    private val _dateUpdates = MutableSharedFlow<Date>(extraBufferCapacity = 1)
    val dateUpdates = _dateUpdates.asSharedFlow()

    private var lastSelectedDate: Date? = null

    fun notifyDateSelected(date: Date) {
        lastSelectedDate = date
        _dateUpdates.tryEmit(date)
    }

    fun getLastSelectedDate(): Date? = lastSelectedDate
}
