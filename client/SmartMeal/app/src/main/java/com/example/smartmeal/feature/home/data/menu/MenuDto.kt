package com.example.smartmeal.feature.home.data.menu

data class MenuDto(
    val id: Int,
    val period: String,
    val start_date: String,
    val created_at: String,
    val items: List<MenuItemDto>? = null
)

data class MenuItemDto(
    val id: Int,
    val menu: Int? = null,
    val day_offset: Int,
    val meal_type: String,
    val recipe: Int,
    val recipe_title: String,
    val actual_date: String,
    val cook_time: Int = 0,
    val image_url: String? = null,
)

data class RecipeShortDto(
    val id: Int,
    val title: String,
    val cook_time: Int,
    val servings: Int,
    val total_calories: Double,
    val total_proteins: Double,
    val total_fats: Double,
    val total_carbs: Double
)

data class RecipeDetailDto(
    val id: Int,
    val title: String,
    val image_url: String? = null,
    val cook_time: Int,
    val servings: Int,
    val total_calories: Double,
    val total_proteins: Double,
    val total_fats: Double,
    val total_carbs: Double,
    val per_serving_calories: Double,
    val per_serving_proteins: Double,
    val per_serving_fats: Double,
    val per_serving_carbs: Double,
    val ingredients: List<RecipeIngredientDto>,
    val steps: List<RecipeStepDto>
)

data class RecipeIngredientDto(
    val ingredient_name: String,
    val amount: Double,
    val unit_name: String,
    val category_name: String? = null
)

data class RecipeStepDto(
    val step_number: Int,
    val description: String,
    val image_url: String? = null
)

data class CartItemDto(
    val id: Int,
    val ingredient: Int,
    val ingredient_name: String,
    val unit: Int,
    val unit_name: String,
    val amount: String,
    val is_checked: Boolean
)

data class CartCategoryDto(
    val name: String,
    val items: List<CartItemDto>
)
