package com.example.smartmeal.feature.recipes.data.api

import com.example.smartmeal.feature.home.data.menu.RecipeDetailDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface RecipeApi {
    @GET("api/recipes/{id}/")
    suspend fun getRecipeDetail(@Path("id") id: Int): Response<RecipeDetailDto>
}
