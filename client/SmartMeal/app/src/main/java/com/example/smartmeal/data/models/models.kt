package com.example.smartmeal.data.models

import com.google.gson.annotations.SerializedName

data class Ingredient(
    val id: Int,
    val name: String,
    val category: String,
    @SerializedName("base_unit") val baseUnit: String
)

data class LoginRequest(
    val email: String,
    val username: String = email, // DRF ObtainAuthToken expects username by default, but we use email
    val password: String
)

data class LoginResponse(
    val token: String,
    @SerializedName("user_id") val userId: Int,
    val email: String
)
