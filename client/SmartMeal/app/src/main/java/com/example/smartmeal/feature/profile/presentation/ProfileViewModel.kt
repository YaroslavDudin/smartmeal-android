package com.example.smartmeal.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.setup.data.api.SetupApi
import com.example.smartmeal.feature.setup.data.models.AllergyDto
import com.example.smartmeal.feature.setup.data.models.DietTypeDto
import com.example.smartmeal.feature.setup.data.models.UpdateProfileRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedSuccess: Boolean = false,

    // Р вЂќР В°Р Р…Р Р…РЎвЂ№Р Вµ Р С—РЎР‚Р С•РЎвЂћР С‘Р В»РЎРЏ РЎРѓ РЎРѓР ВµРЎР‚Р Р†Р ВµРЎР‚Р В°
    val userName: String = "admin",
    val currentDietTypeId: Int? = null,
    val currentDietTypeName: String? = null,
    val currentAllergyIds: Set<Int> = emptySet(),
    val currentAllergyNames: List<String> = emptyList(),
    val portionSize: Int = 1,
    val preferredCookTime: String? = null,

    // Р РЋР С—РЎР‚Р В°Р Р†Р С•РЎвЂЎР Р…Р С‘Р С”Р С‘
    val allDietTypes: List<DietTypeDto> = emptyList(),
    val allAllergies: List<AllergyDto> = emptyList(),

    // Р вЂ™РЎР‚Р ВµР СР ВµР Р…Р Р…РЎвЂ№Р Вµ Р Р†РЎвЂ№Р В±Р С•РЎР‚РЎвЂ№ (Р Т‘Р С• Р Р…Р В°Р В¶Р В°РЎвЂљР С‘РЎРЏ "Р СџР С•Р Т‘РЎвЂљР Р†Р ВµРЎР‚Р Т‘Р С‘РЎвЂљРЎРЉ")
    val pendingAllergyIds: Set<Int> = emptySet(),
    val pendingDietTypeId: Int? = null,
    val pendingPortionSize: Int = 1,

    // Р ВР В·Р В±РЎР‚Р В°Р Р…Р Р…Р С•Р Вµ
    val favorites: List<com.example.smartmeal.feature.home.data.api.UserFavoriteDto> = emptyList()
)

class ProfileViewModel(
    private val api: SetupApi,
    private val preferences: SetupPreferences,
    // Р С™Р С•Р В»Р В±РЎРЊР С” Р Р†РЎвЂ№Р В·РЎвЂ№Р Р†Р В°Р ВµРЎвЂљРЎРѓРЎРЏ Р С—Р С•РЎРѓР В»Р Вµ Р В»РЎР‹Р В±Р С•Р С–Р С• РЎС“РЎРѓР С—Р ВµРЎв‚¬Р Р…Р С•Р С–Р С• PATCH РІР‚вЂќ HomeViewModel Р С—Р ВµРЎР‚Р ВµР В·Р В°Р С–РЎР‚РЎС“Р В¶Р В°Р ВµРЎвЂљ Р СР ВµР Р…РЎР‹
    private val onProfileUpdated: () -> Unit = {}
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadProfile()
        loadFavorites()
        viewModelScope.launch {
            com.example.smartmeal.data.manager.FavoritesManager.favoriteUpdates.collect {
                loadFavorites()
            }
        }
    }

    // РІвЂќР‚РІвЂќР‚РІвЂќР‚ Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° Р С—РЎР‚Р С•РЎвЂћР С‘Р В»РЎРЏ + РЎРѓР С—РЎР‚Р В°Р Р†Р С•РЎвЂЎР Р…Р С‘Р С”Р С•Р Р† РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val userDeferred = async { api.getCurrentUser() }
                val dietsDeferred = async { api.getDietTypes() }
                val allergiesDeferred = async { api.getAllergies() }

                val userResp = userDeferred.await()
                val dietsResp = dietsDeferred.await()
                val allergiesResp = allergiesDeferred.await()

                val user = userResp.body()
                val diets = if (dietsResp.isSuccessful) dietsResp.body() ?: emptyList() else emptyList()
                val allergies = if (allergiesResp.isSuccessful) allergiesResp.body() ?: emptyList() else emptyList()

                _state.update {
                    it.copy(
                        isLoading = false,
                        userName = user?.username ?: "Admin",
                        currentDietTypeId = user?.diet_type,
                        currentDietTypeName = user?.diet_type_name,
                        currentAllergyIds = user?.allergies?.toSet() ?: emptySet(),
                        currentAllergyNames = user?.allergies_names ?: emptyList(),
                        portionSize = user?.portion_size ?: 1,
                        preferredCookTime = user?.preferred_cook_time,
                        allDietTypes = diets,
                        allAllergies = allergies,
                        // pending Р С‘Р Р…Р С‘РЎвЂ Р С‘Р В°Р В»Р С‘Р В·Р С‘РЎР‚РЎС“Р ВµР С РЎвЂљР ВµР С”РЎС“РЎвЂ°Р С‘Р СР С‘ Р В·Р Р…Р В°РЎвЂЎР ВµР Р…Р С‘РЎРЏР СР С‘
                        pendingAllergyIds = user?.allergies?.toSet() ?: emptySet(),
                        pendingDietTypeId = user?.diet_type,
                        pendingPortionSize = user?.portion_size ?: 1,
                    )
                }
                user?.portion_size?.let { preferences.setPortionSize(it) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Ошибка загрузки: ${e.message}") }
            }
        }
    }

    // РІвЂќР‚РІвЂќР‚РІвЂќР‚ Р ВР В·Р В±РЎР‚Р В°Р Р…Р Р…Р С•Р Вµ РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

    fun loadFavorites() {
        viewModelScope.launch {
            try {
                val resp = api.getFavorites()
                if (resp.isSuccessful) {
                    _state.update { it.copy(favorites = resp.body() ?: emptyList()) }
                }
            } catch (e: Exception) {
                // Р С›РЎв‚¬Р С‘Р В±Р С”РЎС“ Р С‘Р В·Р В±РЎР‚Р В°Р Р…Р Р…Р С•Р С–Р С• Р СР С•Р В¶Р Р…Р С• Р Р…Р Вµ Р С—Р С•Р С”Р В°Р В·РЎвЂ№Р Р†Р В°РЎвЂљРЎРЉ Р С”Р В°Р С” Р С”РЎР‚Р С‘РЎвЂљР С‘РЎвЂЎР ВµРЎРѓР С”РЎС“РЎР‹
            }
        }
    }

    fun toggleFavorite(recipeId: Int) {
        viewModelScope.launch {
            try {
                val response = api.toggleFavorite(com.example.smartmeal.feature.home.data.api.ToggleFavoriteRequest(recipeId))
                if (response.isSuccessful) {
                    val isFavorite = response.body()?.is_favorite ?: false
                    loadFavorites()
                    // Р Р€Р Р†Р ВµР Т‘Р С•Р СР В»РЎРЏР ВµР С Р Т‘РЎР‚РЎС“Р С–Р С‘Р Вµ РЎРЊР С”РЎР‚Р В°Р Р…РЎвЂ№
                    com.example.smartmeal.data.manager.FavoritesManager.notifyFavoriteChanged(recipeId, isFavorite)
                }
            } catch (e: Exception) {
                // Р С›РЎв‚¬Р С‘Р В±Р С”РЎС“ Р С‘Р В·Р В±РЎР‚Р В°Р Р…Р Р…Р С•Р С–Р С• Р СР С•Р В¶Р Р…Р С• Р Р…Р Вµ Р С—Р С•Р С”Р В°Р В·РЎвЂ№Р Р†Р В°РЎвЂљРЎРЉ Р С”Р В°Р С” Р С”РЎР‚Р С‘РЎвЂљР С‘РЎвЂЎР ВµРЎРѓР С”РЎС“РЎР‹
            }
        }
    }

    // РІвЂќР‚РІвЂќР‚РІвЂќР‚ Р С’Р В»Р В»Р ВµРЎР‚Р С–Р С‘Р С‘ РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

    fun togglePendingAllergy(id: Int) {
        _state.update {
            val updated = if (id in it.pendingAllergyIds) it.pendingAllergyIds - id
                          else it.pendingAllergyIds + id
            it.copy(pendingAllergyIds = updated)
        }
    }

    fun saveAllergies() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, savedSuccess = false) }
            try {
                val resp = api.updateProfile(
                    UpdateProfileRequest(
                        diet_type = s.currentDietTypeId,
                        portion_size = s.portionSize,
                        allergies = s.pendingAllergyIds.toList(),
                        preferred_cook_time = s.preferredCookTime
                    )
                )
                if (resp.isSuccessful) {
                    val body = resp.body()
                    _state.update {
                        it.copy(
                            isSaving = false,
                            savedSuccess = true,
                            currentAllergyIds = it.pendingAllergyIds,
                            currentAllergyNames = body?.allergies_names ?: it.currentAllergyNames,
                        )
                    }
                    com.example.smartmeal.data.manager.ProfileManager.notifyProfileChanged(
                        portionSize = s.portionSize,
                        dietTypeId = s.currentDietTypeId,
                        allergyIds = s.pendingAllergyIds
                    )
                    onProfileUpdated() // Reload menu.
                } else {
                    _state.update { it.copy(isSaving = false, error = "Ошибка сервера: ${resp.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    // РІвЂќР‚РІвЂќР‚РІвЂќР‚ Р В Р В°РЎвЂ Р С‘Р С•Р Р… РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

    fun selectPendingDiet(id: Int) {
        _state.update {
            // Р С—Р С•Р Р†РЎвЂљР С•РЎР‚Р Р…РЎвЂ№Р в„– РЎвЂљР В°Р С— РІР‚вЂќ РЎРѓР Р…Р С‘Р СР В°Р ВµР С Р Р†РЎвЂ№Р В±Р С•РЎР‚
            it.copy(pendingDietTypeId = if (it.pendingDietTypeId == id) null else id)
        }
    }

    fun saveDiet() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, savedSuccess = false) }
            try {
                val resp = api.updateProfile(
                    UpdateProfileRequest(
                        diet_type = s.pendingDietTypeId,
                        portion_size = s.portionSize,
                        allergies = s.currentAllergyIds.toList(),
                        preferred_cook_time = s.preferredCookTime
                    )
                )
                if (resp.isSuccessful) {
                    val body = resp.body()
                    _state.update {
                        it.copy(
                            isSaving = false,
                            savedSuccess = true,
                            currentDietTypeId = it.pendingDietTypeId,
                            currentDietTypeName = body?.diet_type_name,
                        )
                    }
                    com.example.smartmeal.data.manager.ProfileManager.notifyProfileChanged(
                        portionSize = s.portionSize,
                        dietTypeId = s.pendingDietTypeId,
                        allergyIds = s.currentAllergyIds
                    )
                    onProfileUpdated() // Reload menu.
                } else {
                    _state.update { it.copy(isSaving = false, error = "Ошибка сервера: ${resp.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    // РІвЂќР‚РІвЂќР‚РІвЂќР‚ Р СџР С•РЎР‚РЎвЂ Р С‘Р С‘ РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

    fun incrementPortion() {
        _state.update { it.copy(pendingPortionSize = (it.pendingPortionSize + 1).coerceAtMost(20)) }
    }

    fun decrementPortion() {
        _state.update { it.copy(pendingPortionSize = (it.pendingPortionSize - 1).coerceAtLeast(1)) }
    }

    fun savePortion() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, savedSuccess = false) }
            try {
                val resp = api.updateProfile(
                    UpdateProfileRequest(
                        diet_type = s.currentDietTypeId,
                        portion_size = s.pendingPortionSize,
                        allergies = s.currentAllergyIds.toList(),
                        preferred_cook_time = s.preferredCookTime
                    )
                )
                if (resp.isSuccessful) {
                    preferences.setPortionSize(s.pendingPortionSize)
                    _state.update {
                        it.copy(isSaving = false, savedSuccess = true, portionSize = it.pendingPortionSize)
                    }
                    com.example.smartmeal.data.manager.ProfileManager.notifyProfileChanged(
                        portionSize = s.pendingPortionSize,
                        dietTypeId = s.currentDietTypeId,
                        allergyIds = s.currentAllergyIds
                    )
                    onProfileUpdated()
                } else {
                    _state.update { it.copy(isSaving = false, error = "Ошибка сервера: ${resp.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearSavedSuccess() {
        _state.update { it.copy(savedSuccess = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

