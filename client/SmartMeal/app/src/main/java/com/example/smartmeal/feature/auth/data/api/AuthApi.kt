package com.example.smartmeal.feature.auth.data.api

import com.example.smartmeal.feature.auth.data.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/accounts/register/")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/accounts/token/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
