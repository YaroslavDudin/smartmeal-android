package com.example.smartmeal.feature.home.data

import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.data.api.UpdateCartItemRequest
import com.example.smartmeal.feature.home.data.menu.CartCategoryDto
import com.example.smartmeal.feature.home.data.menu.MenuDto
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import com.example.smartmeal.feature.home.data.menu.RecipeDetailDto
import kotlinx.coroutines.sync.withLock

class MenuRepository(private val api: MenuApi) {

    /** Получает список покупок, сгруппированный по категориям. */
    suspend fun getCart(): List<CartCategoryDto> {
        val response = api.getCart()
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            throw Exception("Ошибка загрузки корзины: ${response.code()} ${errorBody ?: ""}".trim())
        }
        
        /** Получаем словарь от API: Map<Категория, Список_продуктов> */
        val cartMap = response.body() ?: emptyMap()
        
        /** Преобразуем словарь в удобный список DTO */
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
        private const val CACHE_DURATION = 15000L // 15 секунд кэша
        
        fun clearCache() {
            menuItemsCache = null
            latestMenuCache = null
            menuByIdCache.clear()
            lastCacheTime = 0
        }
    }

    /** Получает все элементы меню пользователя. */
    suspend fun getMenuItems(): List<MenuItemDto> {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            if (menuItemsCache != null && now - lastCacheTime < CACHE_DURATION) {
                return@withLock menuItemsCache!!
            }

            val response = api.getMenuItems()
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                throw Exception("Ошибка загрузки меню: ${response.code()} ${errorBody ?: ""}".trim())
            }
            val items = response.body() ?: emptyList()
            menuItemsCache = items
            lastCacheTime = now
            items
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
            val now = System.currentTimeMillis()
            if (latestMenuCache != null && now - lastCacheTime < CACHE_DURATION) {
                return@withLock latestMenuCache
            }

            val response = api.getMenus()
            if (response.isSuccessful) {
                val menus = response.body()
                if (!menus.isNullOrEmpty()) {
                    val lastId = menus.maxOf { it.id }
                    val menu = getMenuByIdInternal(lastId)
                    latestMenuCache = menu
                    lastCacheTime = now
                    return@withLock menu
                }
            }
            null
        }
    }

    /** Внутренний метод получения меню без блокировки (вызывается внутри withLock) */
    private suspend fun getMenuByIdInternal(id: Int): MenuDto? {
        if (menuByIdCache.containsKey(id)) return menuByIdCache[id]
        
        val response = api.getMenu(id)
        return if (response.isSuccessful) {
            val menu = response.body()
            if (menu != null) menuByIdCache[id] = menu
            menu
        } else null
    }

    /** Получает детали конкретного меню со всеми блюдами. */
    suspend fun getMenuById(id: Int): MenuDto? {
        return mutex.withLock {
            getMenuByIdInternal(id)
        }
    }

    /** Заменяет блюдо в меню на другое подходящее. */
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
        if (response.isSuccessful) {
            return response.body()
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