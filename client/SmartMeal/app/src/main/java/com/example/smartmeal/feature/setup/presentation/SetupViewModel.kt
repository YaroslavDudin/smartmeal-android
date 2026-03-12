package com.example.smartmeal.feature.setup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.util.Calendar

enum class PeriodType(val apiValue: String, val label: String) {
    DAILY("day", "Дневной план"),
    WEEKLY("week", "Недельный план"),
    CUSTOM("day", "Свой план"),
}

data class SetupState(
    // --- Step 1 ---
    val dietTypes: List<DietTypeDto> = emptyList(),
    val selectedDietTypeId: Int? = null,
    val portionSize: Int = 1,

    // --- Step 2 ---
    val periodType: PeriodType = PeriodType.WEEKLY,
    val calendarYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val calendarMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedDay: Int? = null,      // для CUSTOM — начало диапазона
    val selectedEndDay: Int? = null,   // для CUSTOM — конец диапазона

    // --- Step 3 ---
    val allergies: List<AllergyDto> = emptyList(),
    val selectedAllergyIds: Set<Int> = emptySet(),
    val eatAll: Boolean = false,
    val cookTimePreference: String? = null,  // "under30", "30to60", "over60"

    // --- Common ---
    val isLoading: Boolean = false,
    val isCheckingUser: Boolean = true,  // true при первом запросе /me/
    val isUserAlreadyConfigured: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false,
)

class SetupViewModel(private val api: SetupApi) : ViewModel() {

    private val _state = MutableStateFlow(SetupState())
    val state: StateFlow<SetupState> = _state.asStateFlow()

    // Вызывается явно из SetupIntroScreen — только после того как пользователь залогинился
    fun loadInitialData() {
        // Не перезагружаем если данные уже успешно загружены (например, пользователь вернулся назад)
        val s = _state.value
        if (!s.isCheckingUser && s.dietTypes.isNotEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isCheckingUser = true) }
            try {
                val userDeferred = async { api.getCurrentUser() }
                val dietTypesDeferred = async { api.getDietTypes() }
                val allergiesDeferred = async { api.getAllergies() }

                val userResponse = userDeferred.await()
                val dietTypesResponse = dietTypesDeferred.await()
                val allergiesResponse = allergiesDeferred.await()

                val user = userResponse.body()
                val dietTypes = if (dietTypesResponse.isSuccessful) dietTypesResponse.body() ?: emptyList() else emptyList()
                val allergies = if (allergiesResponse.isSuccessful) allergiesResponse.body() ?: emptyList() else emptyList()

                _state.update {
                    it.copy(
                        isCheckingUser = false,
                        isUserAlreadyConfigured = user?.diet_type != null,
                        dietTypes = dietTypes,
                        allergies = allergies,
                        // Prefill from existing profile if user comes back
                        selectedDietTypeId = user?.diet_type,
                        portionSize = user?.portion_size ?: 1,
                        selectedAllergyIds = user?.allergies?.toSet() ?: emptySet(),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isCheckingUser = false, error = "Ошибка загрузки: ${e.message}") }
            }
        }
    }

    // --- Step 1 ---

    fun selectDietType(id: Int) {
        _state.update { it.copy(selectedDietTypeId = if (it.selectedDietTypeId == id) null else id) }
    }

    fun incrementPortion() {
        _state.update { it.copy(portionSize = (it.portionSize + 1).coerceAtMost(20)) }
    }

    fun decrementPortion() {
        _state.update { it.copy(portionSize = (it.portionSize - 1).coerceAtLeast(1)) }
    }

    // --- Step 2 ---

    fun selectPeriodType(type: PeriodType) {
        _state.update { it.copy(periodType = type, selectedDay = null, selectedEndDay = null) }
    }

    fun selectDay(day: Int) {
        val s = _state.value
        if (s.periodType == PeriodType.CUSTOM) {
            when {
                // Нет начала → устанавливаем начало
                s.selectedDay == null -> {
                    _state.update { it.copy(selectedDay = day, selectedEndDay = null) }
                }
                // Начало есть, конца нет → устанавливаем конец
                s.selectedEndDay == null -> {
                    if (day > s.selectedDay) {
                        _state.update { it.copy(selectedEndDay = day) }
                    } else if (day < s.selectedDay) {
                        // Тап раньше начала — меняем местами
                        _state.update { it.copy(selectedDay = day, selectedEndDay = s.selectedDay) }
                    } else {
                        // Тап на тот же день — сбрасываем
                        _state.update { it.copy(selectedDay = null, selectedEndDay = null) }
                    }
                }
                // Оба заданы → начинаем выбор заново
                else -> {
                    _state.update { it.copy(selectedDay = day, selectedEndDay = null) }
                }
            }
        } else {
            _state.update { it.copy(selectedDay = day) }
        }
    }

    fun navigateCalendarMonth(offset: Int) {
        _state.update {
            val cal = Calendar.getInstance()
            cal.set(it.calendarYear, it.calendarMonth, 1)
            cal.add(Calendar.MONTH, offset)
            it.copy(
                calendarYear = cal.get(Calendar.YEAR),
                calendarMonth = cal.get(Calendar.MONTH),
                selectedDay = null,
                selectedEndDay = null,
            )
        }
    }

    // --- Step 3 ---

    fun toggleAllergy(id: Int) {
        _state.update {
            val updated = if (id in it.selectedAllergyIds) {
                it.selectedAllergyIds - id
            } else {
                it.selectedAllergyIds + id
            }
            it.copy(selectedAllergyIds = updated, eatAll = false)
        }
    }

    fun setEatAll(value: Boolean) {
        _state.update {
            it.copy(
                eatAll = value,
                selectedAllergyIds = if (value) emptySet() else it.selectedAllergyIds,
            )
        }
    }

    fun selectCookTime(preference: String) {
        _state.update {
            it.copy(cookTimePreference = if (it.cookTimePreference == preference) null else preference)
        }
    }

    // --- Submit ---

    fun submitSetup() {
        val s = _state.value
        if (s.selectedDietTypeId == null) {
            _state.update { it.copy(error = "Выберите тип питания") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.updateProfile(
                    UpdateProfileRequest(
                        diet_type = s.selectedDietTypeId,
                        portion_size = s.portionSize,
                        allergies = s.selectedAllergyIds.toList(),
                    )
                )
                if (response.isSuccessful) {
                    _state.update { it.copy(isLoading = false, isComplete = true) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Ошибка сохранения: ${response.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Ошибка сети: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // Сброс стейта при выходе из аккаунта.
    // Нужен потому что ViewModel живёт на уровне NavGraph и переживает смену пользователя.
    fun reset() {
        _state.value = SetupState()
    }
}
