package com.example.smartmeal.feature.home.data

import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.data.menu.MenuDto
import retrofit2.Response

class MenuRepository(private val api: MenuApi) {

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
}
