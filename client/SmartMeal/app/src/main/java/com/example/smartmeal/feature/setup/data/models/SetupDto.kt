package com.example.smartmeal.feature.setup.data.models

data class DietTypeDto(
    val id: Int,
    val name: String,
)

data class AllergyDto(
    val id: Int,
    val name: String,
)

data class UserProfileDto(
    val id: Int,
    val username: String,
    val email: String,
    val portion_size: Int,
    val diet_type: Int?,
    val diet_type_name: String?,
    val allergies: List<Int>,
    val allergies_names: List<String>,
)

data class UpdateProfileRequest(
    val diet_type: Int?,
    val portion_size: Int,
    val allergies: List<Int>,
)
