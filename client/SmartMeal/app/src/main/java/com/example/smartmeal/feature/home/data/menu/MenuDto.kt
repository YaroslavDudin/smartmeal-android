package com.example.smartmeal.feature.home.data.menu

// ─── Меню ───────────────────────────────────────────────────────────────────

data class MenuDto(
    val id: Int,
    val period: String,      // "day" — дневное, "week" — недельное
    val start_date: String,  // "2026-03-10"
    val created_at: String,  // "2026-03-10T10:00:00Z"
    val items: List<MenuItemDto>? = null
)

// ─── Элементы меню ──────────────────────────────────────────────────────────

data class MenuItemDto(
    val id: Int,
    val menu: Int? = null,   // ID меню
    val day_offset: Int,     // 0 = первый день, 1 = второй и т.д.
    val meal_type: String,   // "breakfast", "lunch", "dinner", "snack", "drink"
    val recipe: Int,         // ID рецепта
    val recipe_title: String, // Название рецепта (уже есть в API бэкенда)
    val actual_date: String  // "2026-03-10" — реальная дата (start_date + day_offset)
)

// ─── Рецепты ────────────────────────────────────────────────────────────────

/** Краткая карточка рецепта — для списков и превью в меню */
data class RecipeShortDto(
    val id: Int,
    val title: String,
    val cook_time: Int,          // минуты
    val servings: Int,           // кол-во порций в рецепте
    val total_calories: Double,
    val total_proteins: Double,
    val total_fats: Double,
    val total_carbs: Double
)

/** Полный рецепт — для экрана детали рецепта */
data class RecipeDetailDto(
    val id: Int,
    val title: String,
    val cook_time: Int,
    val servings: Int,
    val total_calories: Double,
    val total_proteins: Double,
    val total_fats: Double,
    val total_carbs: Double,
    val per_serving_calories: Double,   // на одну порцию
    val per_serving_proteins: Double,
    val per_serving_fats: Double,
    val per_serving_carbs: Double,
    val ingredients: List<RecipeIngredientDto>,
    val steps: List<RecipeStepDto>
)

data class RecipeIngredientDto(
    val ingredient_name: String,
    val amount: Double,
    val unit_name: String        // "г", "мл", "шт" и т.д.
)

data class RecipeStepDto(
    val step_number: Int,
    val description: String
)

// ─── Корзина покупок ─────────────────────────────────────────────────────────

data class CartItemDto(
    val id: Int,
    val ingredient: Int,         // ID ингредиента
    val ingredient_name: String,
    val unit: Int,               // ID единицы измерения
    val unit_name: String,
    val amount: String,          // Decimal-строка: "300.00"
    val is_checked: Boolean      // куплено или нет
)
