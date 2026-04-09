package com.example.smartmeal.feature.auth.data.models

// --- Requests ---
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val password_confirm: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshRequest(
    val refresh: String
)

data class PasswordResetRequest(
    val email: String
)

data class PasswordResetConfirmRequest(
    val uid: String,
    val token: String,
    val new_password: String,
    val new_password_confirm: String
)

// --- Responses ---
data class RegisterResponse(
    val user: UserDto,
    val access: String,
    val refresh: String,
    val message: String
)

data class UserDto(
    val username: String,
    val email: String
)

data class LoginResponse(
    val access: String,
    val refresh: String
)

data class RefreshResponse(
    val access: String,
    val refresh: String? = null
)
