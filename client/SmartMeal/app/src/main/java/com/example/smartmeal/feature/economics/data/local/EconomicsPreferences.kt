package com.example.smartmeal.feature.economics.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Локальное хранилище настроек экономики.
 * TODO: В будущем заменить на синхронизацию с сервером или Яндекс API.
 */
class EconomicsPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDailyBudget(): Float = prefs.getFloat(KEY_DAILY_BUDGET, DEFAULT_BUDGET)

    fun setDailyBudget(amount: Float) {
        prefs.edit().putFloat(KEY_DAILY_BUDGET, amount).apply()
    }

    companion object {
        private const val PREFS_NAME = "smartmeal_economics"
        private const val KEY_DAILY_BUDGET = "daily_budget"
        private const val DEFAULT_BUDGET = 500f
    }
}
