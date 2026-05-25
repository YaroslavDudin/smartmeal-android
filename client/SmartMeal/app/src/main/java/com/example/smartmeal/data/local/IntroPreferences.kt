package com.example.smartmeal.data.local

import android.content.Context

class IntroPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isIntroShown(): Boolean = prefs.getBoolean(KEY_INTRO_SHOWN, false)

    fun markIntroShown() {
        prefs.edit().putBoolean(KEY_INTRO_SHOWN, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "intro_prefs"
        private const val KEY_INTRO_SHOWN = "intro_shown"
    }
}
