package com.example.smartmeal.feature.home.data

import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.data.api.UpdateCartItemRequest
import com.example.smartmeal.feature.home.data.menu.CartCategoryDto
import com.example.smartmeal.feature.home.data.menu.CartItemDto
import com.example.smartmeal.feature.home.data.menu.MenuDto
import retrofit2.Response

class MenuRepository(private val api: MenuApi) {

    /** Получает список покупок, сгруппированный по категориям (пока одна общая) */
    suspend fun getCart(): List<CartCategoryDto> {
        val response = api.getCart()
        if (response.isSuccessful) {
            val items = response.body() ?: emptyList()
            // Для начала просто кладём всё в одну категорию "Продукты"
            return listOf(CartCategoryDto(name = "Продукты", items = items))
        }
        return emptyList()
    }

    /** Обновляет статус купленного товара */
    suspend fun updateCartItem(id: Int, isChecked: Boolean): Boolean {
        val response = api.updateCartItem(id, UpdateCartItemRequest(is_checked = isChecked))
        return response.isSuccessful
    }

    /** Получает последнее актуальное меню пользователя */
    suspend fun getLatestMenu(): MenuDto? {
        val response = api.getMenus()
        if (response.isSuccessful) {
            val menus = response.body()
            if (!menus.isNullOrEmpty()) {
                // Берем самое новое по ID или дате.
                val lastId = menus.maxOf { it.id }
                return getMenuById(lastId)
            }
        }
        return null
    }

    /** Получает детали конкретного меню со всеми блюдами */
    suspend fun getMenuById(id: Int): MenuDto? {
        val response = api.getMenu(id)
        if (response.isSuccessful) {
            return response.body()
        }
        return null
    }

    /** Заменяет блюдо в меню на другое подходящее */
    suspend fun replaceMenuItem(menuItemId: Int): com.example.smartmeal.feature.home.data.menu.MenuItemDto? {
        val response = api.replaceMenuItem(menuItemId)
        if (response.isSuccessful) {
            return response.body()
        } else {
            // Пытаемся достать "detail" из JSON ошибки: {"detail": "..."}
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
