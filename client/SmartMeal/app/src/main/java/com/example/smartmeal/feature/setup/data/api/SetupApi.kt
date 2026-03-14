package com.example.smartmeal.feature.setup.data.api

import com.example.smartmeal.feature.setup.data.models.AllergyDto
import com.example.smartmeal.feature.setup.data.models.DietTypeDto
import com.example.smartmeal.feature.setup.data.models.UpdateProfileRequest
import com.example.smartmeal.feature.setup.data.models.UserProfileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface SetupApi {

    @GET("api/accounts/me/")
    suspend fun getCurrentUser(): Response<UserProfileDto>

    @PATCH("api/accounts/me/")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserProfileDto>

    @GET("api/accounts/diet-types/")
    suspend fun getDietTypes(): Response<List<DietTypeDto>>

    @GET("api/accounts/allergies/")
    suspend fun getAllergies(): Response<List<AllergyDto>>
}
