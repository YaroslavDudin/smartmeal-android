package com.example.smartmeal.feature.recipes.data.api

import com.example.smartmeal.feature.home.data.menu.RecipeDetailDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RecipeApi {
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
