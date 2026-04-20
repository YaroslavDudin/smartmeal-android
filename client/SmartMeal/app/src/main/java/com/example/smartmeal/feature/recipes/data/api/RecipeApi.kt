package com.example.smartmeal.feature.recipes.data.api

import com.example.smartmeal.feature.home.data.menu.RecipeDetailDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RecipeApi {
    @GET("api/recipes/")
    suspend fun getRecipes(
        @Query("search") search: String? = null,
        @Query("diet_type") dietType: Int? = null,
        @Query("min_calories") minCalories: Int? = null,
        @Query("max_calories") maxCalories: Int? = null,
        @Query("page") page: Int? = null
    ): Response<RecipeListResponse>

    @GET("api/recipes/{id}/")
    suspend fun getRecipeDetail(
        @Path("id") id: Int,
        @Query("servings") servings: Int? = null // Для запроса нужного количества порций
    ): Response<RecipeDetailDto>

    @POST("api/accounts/favorites/toggle/")
    suspend fun toggleFavorite(@Body request: ToggleFavoriteRequest): Response<ToggleFavoriteResponse>
}

data class ToggleFavoriteRequest(
    val recipe: Int
)

data class ToggleFavoriteResponse(
    val is_favorite: Boolean
)

data class RecipeListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<RecipeShortDto>
)

data class RecipeShortDto(
    val id: Int,
    val title: String,
    val image_url: String?,
    val cook_time: Int,
    val servings: Int,
    val per_serving_calories: Double,
    val per_serving_proteins: Double,
    val per_serving_fats: Double,
    val per_serving_carbs: Double,
    val is_favorite: Boolean = false
)
