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
    val avatar: String?, // URL аватара
    val portion_size: Int,
    val diet_type: Int?,
    val diet_type_name: String?,
    val preferred_cook_time: String?,
    val preferred_cook_time_display: String?,
    val allergies: List<Int>,
    val allergies_names: List<String>,
    val birth_date: String? = null,
    val gender: String? = null,
)

data class UpdateProfileRequest(
    val username: String? = null,
    val diet_type: Int?,
    val portion_size: Int,
    val allergies: List<Int>,
    val preferred_cook_time: String? = null,
    val birth_date: String? = null,
    val gender: String? = null,
    // Аватар обычно передается через Multipart, поэтому в JSON-запросе он может не понадобиться, 
    // но добавим для полноты модели, если будем использовать PATCH с URL (хотя это редко)
    val avatar: String? = null, 
)
