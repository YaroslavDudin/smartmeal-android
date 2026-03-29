package com.example.smartmeal.feature.home.data.api

import com.example.smartmeal.feature.home.data.menu.CartItemDto
import com.example.smartmeal.feature.home.data.menu.MenuDto
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import com.example.smartmeal.feature.home.data.menu.RecipeDetailDto
import com.example.smartmeal.feature.home.data.menu.RecipeShortDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MenuApi {

    // ─── Меню ───────────────────────────────────────────────────────────────

    /** Список всех меню текущего пользователя */
    @GET("api/menus/")
    suspend fun getMenus(): Response<List<MenuDto>>

    /** Конкретное меню по ID */
    @GET("api/menus/{id}/")
    suspend fun getMenu(@Path("id") id: Int): Response<MenuDto>

    /** Удалить меню */
    @DELETE("api/menus/{id}/")
    suspend fun deleteMenu(@Path("id") id: Int): Response<Unit>

    // ─── Элементы меню ──────────────────────────────────────────────────────

    /** Все элементы меню текущего пользователя (все дни, все приёмы пищи) */
    @GET("api/menus/items/")
    suspend fun getMenuItems(): Response<List<MenuItemDto>>

    /** Удалить блюдо из меню */
    @DELETE("api/menus/items/{id}/")
    suspend fun deleteMenuItem(@Path("id") id: Int): Response<Unit>

    /** Заменить блюдо в меню на другое подходящее */
    @POST("api/menus/items/{id}/replace/")
    suspend fun replaceMenuItem(@Path("id") id: Int): Response<MenuItemDto>

    // ─── Рецепты ────────────────────────────────────────────────────────────

    /**
     * Список рецептов.
     * @param search необязательный поиск по названию: `?search=борщ`
     */
    @GET("api/recipes/")
    suspend fun getRecipes(@Query("search") search: String? = null): Response<List<RecipeShortDto>>

    /** Полный рецепт с ингредиентами и шагами */
    @GET("api/recipes/{id}/")
    suspend fun getRecipe(
        @Path("id") id: Int,
        @Query("servings") servings: Int? = null
    ): Response<RecipeDetailDto>

    // ─── Корзина ────────────────────────────────────────────────────────────

    /** Список покупок текущего пользователя */
    @GET("api/cart/")
    suspend fun getCart(): Response<List<CartItemDto>>

    /**
     * Отметить позицию купленной / снять отметку.
     * @param request тело: `{ "is_checked": true }` или `{ "amount": "500.00" }`
     */
    @PATCH("api/cart/{id}/")
    suspend fun updateCartItem(
        @Path("id") id: Int,
        @Body request: UpdateCartItemRequest
    ): Response<CartItemDto>

    /** Удалить позицию из корзины */
    @DELETE("api/cart/{id}/")
    suspend fun deleteCartItem(@Path("id") id: Int): Response<Unit>

    // ─── Избранное ──────────────────────────────────────────────────────────

    /** Список избранных рецептов текущего пользователя */
    @GET("api/accounts/favorites/")
    suspend fun getFavorites(): Response<List<UserFavoriteDto>>

    /** Переключить статус избранного для рецепта */
    @POST("api/accounts/favorites/toggle/")
    suspend fun toggleFavorite(@Body request: ToggleFavoriteRequest): Response<ToggleFavoriteResponse>
}

// ─── Request-классы для MenuApi ─────────────────────────────────────────────

data class UpdateCartItemRequest(
    val is_checked: Boolean? = null,
    val amount: String? = null
)

data class ToggleFavoriteRequest(
    val recipe: Int
)

data class ToggleFavoriteResponse(
    val is_favorite: Boolean
)

data class UserFavoriteDto(
    val id: Int,
    val recipe: Int,
    val recipe_title: String,
    val recipe_image_url: String?,
    val recipe_cook_time: Int
)
