package com.example.smartmeal.feature.menu_generator.data.api

import com.example.smartmeal.feature.home.data.menu.CartItemDto
import com.example.smartmeal.feature.home.data.menu.MenuDto
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import com.example.smartmeal.feature.home.data.menu.RecipeShortDto
import com.example.smartmeal.feature.menu_generator.data.models.AddToCartRequest
import com.example.smartmeal.feature.menu_generator.data.models.AutoGenerateRequest
import com.example.smartmeal.feature.menu_generator.data.models.CreateMenuItemRequest
import com.example.smartmeal.feature.menu_generator.data.models.CreateMenuRequest
import com.example.smartmeal.feature.menu_generator.data.models.GeneratedMenuDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * API для генерации меню.
 *
 * ─── Быстрый флоу (автогенерация) ───────────────────────────────────────────
 * 1. [autoGenerate] — одним запросом создать меню и заполнить рецептами
 *
 * ─── Ручной флоу (сборка по шагам) ──────────────────────────────────────────
 * 1. [createMenu]   — создать пустое меню
 * 2. [getRecipes]   — выбрать рецепты из каталога
 * 3. [addMenuItem]  × N — добавить рецепты по дням и типам приёма пищи
 * 4. [addToCart]    × M — наполнить корзину ингредиентами
 */
interface GeneratorApi {

    // ─── Автоматическая генерация (рекомендуемый путь) ───────────────────────

    /**
     * POST /api/menus/generate/
     *
     * Создаёт меню и автоматически заполняет его рецептами (breakfast/lunch/dinner × days).
     * Бэкенд фильтрует рецепты по [AutoGenerateRequest.diet_type] (fallback → профиль юзера)
     * и [AutoGenerateRequest.max_cook_time] (null = без ограничения).
     *
     * Ответ 201: [GeneratedMenuDto] с полным списком items.
     * Ответ 400: если нет рецептов для заданных параметров.
     *
     * Пример:
     * ```
     * autoGenerate(AutoGenerateRequest(period = "week", start_date = "2026-03-10"))
     * // → GeneratedMenuDto(id=42, period="week", items=[...21 items...])
     * ```
     */
    @POST("api/menus/generate/")
    suspend fun autoGenerate(@Body request: AutoGenerateRequest): Response<GeneratedMenuDto>

    // ─── Ручной флоу: шаг 1 — создать меню ──────────────────────────────────

    /**
     * POST /api/menus/
     * Создаёт пустое меню. Возвращает `MenuDto` с `id` — он нужен для шагов ниже.
     */
    @POST("api/menus/")
    suspend fun createMenu(@Body request: CreateMenuRequest): Response<MenuDto>

    // ─── Ручной флоу: шаг 2 — выбрать рецепты ───────────────────────────────

    /**
     * GET /api/recipes/
     * Список рецептов для выбора при составлении меню.
     * @param search поиск по названию (опционально): `?search=борщ`
     */
    @GET("api/recipes/")
    suspend fun getRecipes(@Query("search") search: String? = null): Response<List<RecipeShortDto>>

    // ─── Ручной флоу: шаг 3 — наполнить меню рецептами ──────────────────────

    /**
     * POST /api/menus/items/
     * Добавляет рецепт в конкретный слот меню.
     *
     * Ошибка 400 если слот (menu + day_offset + meal_type) уже занят.
     *
     * Пример — добавить завтрак в первый день:
     * ```
     * addMenuItem(CreateMenuItemRequest(menu=42, day_offset=0, meal_type="breakfast", recipe=5))
     * ```
     *
     * Все типы приёма пищи: "breakfast", "lunch", "dinner", "snack", "drink"
     */
    @POST("api/menus/items/")
    suspend fun addMenuItem(@Body request: CreateMenuItemRequest): Response<MenuItemDto>

    // ─── Ручной флоу: шаг 4 — добавить ингредиенты в корзину ────────────────

    /**
     * POST /api/cart/
     * Добавляет ингредиент в список покупок.
     *
     * ID ингредиентов и единиц измерения берутся из RecipeDetailDto.ingredients
     * (GET /api/recipes/{id}/ — доступен через MenuApi).
     */
    @POST("api/cart/")
    suspend fun addToCart(@Body request: AddToCartRequest): Response<CartItemDto>
}
