package com.example.smartmeal.data.api

import com.example.smartmeal.data.models.Ingredient
import com.example.smartmeal.data.models.LoginRequest
import com.example.smartmeal.data.models.LoginResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SmartMealApi {
    @POST("api/auth/login/")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/meals/ingredients/")
    suspend fun getIngredients(): List<Ingredient>

    companion object {
        private const val BASE_URL = "http://10.0.2.2:8000/" // Default for Android Emulator to host loopback

        fun create(): SmartMealApi {
            val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SmartMealApi::class.java)
        }
    }
}
