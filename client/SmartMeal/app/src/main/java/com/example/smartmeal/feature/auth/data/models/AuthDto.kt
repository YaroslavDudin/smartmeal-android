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

// --- Responses ---
data class RegisterResponse(
    val user: UserDto,
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
