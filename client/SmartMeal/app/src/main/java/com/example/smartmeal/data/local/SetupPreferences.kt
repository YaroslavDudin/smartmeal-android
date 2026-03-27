package com.example.smartmeal.data.local

import android.content.Context
import java.util.Locale

class SetupPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setActiveUserKey(rawKey: String?) {
        if (rawKey.isNullOrBlank()) {
            prefs.edit().remove(KEY_ACTIVE_USER).apply()
            return
        }
        val normalized = normalizeUserKey(rawKey)
        val current = getActiveUserKey()
        val editor = prefs.edit()
        if (!current.isNullOrBlank()) {
            editor.putString(KEY_PREVIOUS_USER, current)
        }
        editor.putString(KEY_ACTIVE_USER, normalized).apply()

        val scopedPlanKey = scopedKeyFor(KEY_PLAN_TYPE, normalized)
        val scopedStartKey = scopedKeyFor(KEY_CUSTOM_START, normalized)
        val scopedEndKey = scopedKeyFor(KEY_CUSTOM_END, normalized)
        val scopedSelectedDateKey = scopedKeyFor(KEY_SELECTED_PLAN_DATE, normalized)
        val previous = getPreviousUserKey()
        val shouldMigrateLegacy = (previous.isNullOrBlank() || previous == normalized) &&
            !prefs.contains(scopedPlanKey) &&
            !prefs.contains(scopedStartKey) &&
            !prefs.contains(scopedEndKey) &&
            !prefs.contains(scopedSelectedDateKey)

        if (shouldMigrateLegacy) {
            val legacyPlan = prefs.getString(KEY_PLAN_TYPE, null)
            val hasLegacyRange = prefs.contains(KEY_CUSTOM_START) && prefs.contains(KEY_CUSTOM_END)
            val legacyStart = prefs.getLong(KEY_CUSTOM_START, 0L)
            val legacyEnd = prefs.getLong(KEY_CUSTOM_END, 0L)
            val migrateEditor = prefs.edit()
            if (legacyPlan != null) {
                migrateEditor.putString(scopedPlanKey, legacyPlan)
            }
            if (hasLegacyRange && legacyStart != 0L && legacyEnd != 0L) {
                migrateEditor.putLong(scopedStartKey, legacyStart)
                migrateEditor.putLong(scopedEndKey, legacyEnd)
            }
            migrateEditor.apply()
        }
    }

    fun getActiveUserKey(): String? = prefs.getString(KEY_ACTIVE_USER, null)

    fun clearActiveUserKey() {
        prefs.edit().remove(KEY_ACTIVE_USER).apply()
    }

    private fun getPreviousUserKey(): String? = prefs.getString(KEY_PREVIOUS_USER, null)

    fun setPlanType(type: String) {
        prefs.edit().putString(scopedKey(KEY_PLAN_TYPE), type).apply()
    }

    fun getPlanType(): String? {
        val active = getActiveUserKey()
        if (active.isNullOrBlank()) return null
        return prefs.getString(scopedKey(KEY_PLAN_TYPE), null)
    }

    fun setCustomPlanRange(startMillis: Long, endMillis: Long) {
        prefs.edit()
            .putLong(scopedKey(KEY_CUSTOM_START), startMillis)
            .putLong(scopedKey(KEY_CUSTOM_END), endMillis)
            .apply()
    }

    fun setSelectedPlanDate(dateMillis: Long) {
        prefs.edit().putLong(scopedKey(KEY_SELECTED_PLAN_DATE), dateMillis).apply()
    }

    fun getSelectedPlanDate(): Long? {
        val active = getActiveUserKey()
        if (active.isNullOrBlank()) return null
        val key = scopedKey(KEY_SELECTED_PLAN_DATE)
        if (!prefs.contains(key)) return null
        val value = prefs.getLong(key, 0L)
        return value.takeIf { it != 0L }
    }

    fun getCustomPlanRange(): Pair<Long, Long>? {
        val active = getActiveUserKey()
        if (active.isNullOrBlank()) return null
        val startKey = scopedKey(KEY_CUSTOM_START)
        val endKey = scopedKey(KEY_CUSTOM_END)
        if (!prefs.contains(startKey) || !prefs.contains(endKey)) return null
        val start = prefs.getLong(startKey, 0L)
        val end = prefs.getLong(endKey, 0L)
        if (start == 0L || end == 0L) return null
        return start to end
    }

    fun clearCustomPlanRange() {
        prefs.edit().remove(scopedKey(KEY_CUSTOM_START)).remove(scopedKey(KEY_CUSTOM_END)).apply()
    }

    fun clearSelectedPlanDate() {
        prefs.edit().remove(scopedKey(KEY_SELECTED_PLAN_DATE)).apply()
    }

    fun clearPlanSelection() {
        prefs.edit()
            .remove(scopedKey(KEY_PLAN_TYPE))
            .remove(scopedKey(KEY_CUSTOM_START))
            .remove(scopedKey(KEY_CUSTOM_END))
            .remove(scopedKey(KEY_SELECTED_PLAN_DATE))
            .apply()
    }

    private fun scopedKey(base: String): String {
        val active = getActiveUserKey()
        return if (active.isNullOrBlank()) base else "${base}_$active"
    }

    private fun scopedKeyFor(base: String, userKey: String): String {
        return "${base}_$userKey"
    }

    private fun normalizeUserKey(raw: String): String {
        val cleaned = raw.trim().lowercase(Locale.US)
        val sb = StringBuilder(cleaned.length)
        for (ch in cleaned) {
            sb.append(if (ch.isLetterOrDigit()) ch else '_')
        }
        return sb.toString().take(64).ifBlank { "user" }
    }

    fun setMenuItemServings(menuItemId: Int, servings: Int) {
        prefs.edit().putInt("menu_item_servings_$menuItemId", servings).apply()
    }

    fun getMenuItemServings(menuItemId: Int): Int {
        return prefs.getInt("menu_item_servings_$menuItemId", 0) // 0 означает отсутствие переопределения
    }

    fun setPortionSize(size: Int) {
        prefs.edit().putInt(scopedKey(KEY_PORTION_SIZE), size).apply()
    }

    fun getPortionSize(): Int {
        val active = getActiveUserKey()
        if (active.isNullOrBlank()) return 1
        return prefs.getInt(scopedKey(KEY_PORTION_SIZE), 1)
    }

    companion object {
        const val PLAN_TYPE_DAILY = "daily"
        const val PLAN_TYPE_WEEKLY = "weekly"
        const val PLAN_TYPE_CUSTOM = "custom"

        private const val PREFS_NAME = "setup_prefs"
        private const val KEY_ACTIVE_USER = "active_user_key"
        private const val KEY_PREVIOUS_USER = "previous_user_key"
        private const val KEY_PLAN_TYPE = "plan_type"
        private const val KEY_CUSTOM_START = "custom_plan_start"
        private const val KEY_CUSTOM_END = "custom_plan_end"
        private const val KEY_SELECTED_PLAN_DATE = "selected_plan_date"
        private const val KEY_PORTION_SIZE = "portion_size"
    }
}
