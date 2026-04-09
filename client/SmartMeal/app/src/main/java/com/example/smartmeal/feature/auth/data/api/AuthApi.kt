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

    @POST("api/accounts/token/refresh/")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<RefreshResponse>

    @POST("api/accounts/logout/")
    suspend fun logout(@Body request: RefreshRequest): Response<Unit>

    @POST("api/accounts/password-reset/")
    suspend fun passwordReset(@Body request: PasswordResetRequest): Response<Map<String, String>>

    @POST("api/accounts/password-reset-confirm/")
    suspend fun passwordResetConfirm(@Body request: PasswordResetConfirmRequest): Response<Map<String, String>>
}
