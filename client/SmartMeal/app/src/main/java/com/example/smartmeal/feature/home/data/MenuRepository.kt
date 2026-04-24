package com.example.smartmeal.feature.home.data

import android.content.Context
import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.data.api.UpdateCartItemRequest
import com.example.smartmeal.feature.home.data.menu.CartCategoryDto
import com.example.smartmeal.feature.home.data.menu.MenuDto
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import com.example.smartmeal.feature.home.data.menu.RecipeDetailDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.withLock

class MenuRepository(private val api: MenuApi, private val context: Context? = null) {
    private val prefs = context?.getSharedPreferences("smart_meal_menu_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        // Мгновенно восстанавливаем кэш из Postgres (локальную копию) при запуске
        if (latestMenuCache == null && prefs != null) {
            try {
                val latestMenuJson = prefs?.getString("latest_menu", null)
                if (latestMenuJson != null) {
                    latestMenuCache = gson.fromJson(latestMenuJson, MenuDto::class.java)
                }

                val menuItemsJson = prefs?.getString("menu_items", null)
                if (menuItemsJson != null) {
                    val type = object : TypeToken<List<MenuItemDto>>() {}.type
                    menuItemsCache = gson.fromJson(menuItemsJson, type)
                }
                
                lastCacheTime = prefs?.getLong("last_cache_time", 0) ?: 0
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveCacheToDisk() {
        prefs?.edit()?.apply {
            putString("latest_menu", gson.toJson(latestMenuCache))
            putString("menu_items", gson.toJson(menuItemsCache))
            putLong("last_cache_time", lastCacheTime)
            apply()
        }
    }

    /** Получает список покупок, сгруппированный по категориям. */
    suspend fun getCart(): List<CartCategoryDto> {
        val response = api.getCart()
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            throw Exception("Ошибка загрузки корзины: ${response.code()} ${errorBody ?: ""}".trim())
        }
        
        val cartMap = response.body() ?: emptyMap()
        return cartMap.map { (categoryName, items) ->
            CartCategoryDto(name = categoryName, items = items)
        }
    }

    /** Обновляет статус купленного товара. */
    suspend fun updateCartItem(id: Int, isChecked: Boolean): Boolean {
        val response = api.updateCartItem(id, UpdateCartItemRequest(is_checked = isChecked))
        return response.isSuccessful
    }

    companion object {
        private val mutex = kotlinx.coroutines.sync.Mutex()
        private var menuItemsCache: List<MenuItemDto>? = null
        private var latestMenuCache: MenuDto? = null
        private val menuByIdCache = mutableMapOf<Int, MenuDto>()
        private var lastCacheTime: Long = 0
        
        fun clearCache(context: Context? = null) {
            menuItemsCache = null
            latestMenuCache = null
            menuByIdCache.clear()
            lastCacheTime = 0
            context?.getSharedPreferences("smart_meal_menu_cache", Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
        }

        fun getLatestMenuCache(): MenuDto? = latestMenuCache
        fun getMenuItemsCache(): List<MenuItemDto>? = menuItemsCache

        fun setMenuItemsCache(items: List<MenuItemDto>) {
            menuItemsCache = items
            lastCacheTime = System.currentTimeMillis()
        }

        fun updateMenuItemInCache(updatedItem: MenuItemDto) {
            menuItemsCache = menuItemsCache?.map { 
                if (it.id == updatedItem.id) updatedItem else it 
            }
            lastCacheTime = System.currentTimeMillis()
        }
    }

    /** Получает все элементы меню пользователя. */
    suspend fun getMenuItems(): List<MenuItemDto> {
        return mutex.withLock {
            val response = api.getMenuItems()
            if (response.isSuccessful) {
                val items = response.body() ?: emptyList()
                menuItemsCache = items
                lastCacheTime = System.currentTimeMillis()
                saveCacheToDisk()
                items
            } else {
                menuItemsCache ?: throw Exception("Сервер недоступен и кэш пуст")
            }
        }
    }

    /** Получает рецепт по id. */
    suspend fun getRecipe(id: Int): RecipeDetailDto? {
        val response = api.getRecipe(id)
        return if (response.isSuccessful) response.body() else null
    }

    /** Получает последнее актуальное меню пользователя. */
    suspend fun getLatestMenu(): MenuDto? {
        return mutex.withLock {
            val response = api.getMenus()
            if (response.isSuccessful) {
                val menus = response.body()
                if (!menus.isNullOrEmpty()) {
                    val lastId = menus.maxOf { it.id }
                    val menu = getMenuByIdInternal(lastId)
                    latestMenuCache = menu
                    lastCacheTime = System.currentTimeMillis()
                    saveCacheToDisk()
                    return@withLock menu
                }
            }
            latestMenuCache
        }
    }

    private suspend fun getMenuByIdInternal(id: Int): MenuDto? {
        if (menuByIdCache.containsKey(id)) return menuByIdCache[id]
        val response = api.getMenu(id)
        return if (response.isSuccessful) {
            val menu = response.body()
            if (menu != null) menuByIdCache[id] = menu
            menu
        } else null
    }

    suspend fun getMenuById(id: Int): MenuDto? {
        return mutex.withLock { getMenuByIdInternal(id) }
    }

    suspend fun replaceMenuItem(
        menuItemId: Int, 
        cookTimeRange: String? = null,
        totalCalories: Int? = null,
        mealCalories: Map<String, Int>? = null,
        calorieMargin: Int? = null
    ): MenuItemDto? {
        val mealCalsJson = mealCalories?.let { 
            val entries = it.map { (k, v) -> "\"$k\":$v" }.joinToString(",")
            "{$entries}"
        }
        val response = api.replaceMenuItem(menuItemId, cookTimeRange, totalCalories, mealCalsJson, calorieMargin)
        return if (response.isSuccessful) {
            response.body()
        } else {
            val errorBody = response.errorBody()?.string()
            val message = try {
                val json = org.json.JSONObject(errorBody ?: "{}")
                json.optString("detail", "Ошибка замены")
            } catch (e: Exception) {
                "Ошибка сервера: ${response.code()}"
            }
            throw Exception(message)
        }
    }
}
