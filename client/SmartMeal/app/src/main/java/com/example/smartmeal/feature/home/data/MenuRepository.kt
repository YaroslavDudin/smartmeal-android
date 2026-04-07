package com.example.smartmeal.feature.home.data

import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.data.api.UpdateCartItemRequest
import com.example.smartmeal.feature.home.data.menu.CartCategoryDto
import com.example.smartmeal.feature.home.data.menu.MenuDto
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import com.example.smartmeal.feature.home.data.menu.RecipeDetailDto

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

    /** Получает все элементы меню пользователя. */
    suspend fun getMenuItems(): List<MenuItemDto> {
        val response = api.getMenuItems()
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            throw Exception("Ошибка загрузки меню: ${response.code()} ${errorBody ?: ""}".trim())
        }
        return response.body() ?: emptyList()
    }

    /** Получает рецепт по id. */
    suspend fun getRecipe(id: Int): RecipeDetailDto? {
        val response = api.getRecipe(id)
        return if (response.isSuccessful) response.body() else null
    }

    /** Получает последнее актуальное меню пользователя. */
    suspend fun getLatestMenu(): MenuDto? {
        val response = api.getMenus()
        if (response.isSuccessful) {
            val menus = response.body()
            if (!menus.isNullOrEmpty()) {
                val lastId = menus.maxOf { it.id }
                return getMenuById(lastId)
            }
        }
        return null
    }

    /** Получает детали конкретного меню со всеми блюдами. */
    suspend fun getMenuById(id: Int): MenuDto? {
        val response = api.getMenu(id)
        if (response.isSuccessful) {
            return response.body()
        }
        return null
    }

    /** Заменяет блюдо в меню на другое подходящее. */
    suspend fun replaceMenuItem(menuItemId: Int): MenuItemDto? {
        val response = api.replaceMenuItem(menuItemId)
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