package com.example.smartmeal.feature.setup.presentation

import com.example.smartmeal.feature.setup.data.api.SetupApi
import com.example.smartmeal.feature.setup.data.models.AllergyDto
import com.example.smartmeal.feature.setup.data.models.DietTypeDto
import com.example.smartmeal.feature.setup.data.models.UpdateProfileRequest
import com.example.smartmeal.feature.setup.data.models.UserProfileDto
import com.example.smartmeal.feature.home.data.api.ToggleFavoriteRequest
import com.example.smartmeal.feature.home.data.api.ToggleFavoriteResponse
import com.example.smartmeal.feature.home.data.api.UserFavoriteDto
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.Response

class SetupViewModelTest {

    @Test
    fun customPlanRange_canSpanAcrossMonths() {
        val viewModel = SetupViewModel(FakeSetupApi())
        viewModel.selectPeriodType(PeriodType.CUSTOM)

        val initialState = viewModel.state.value
        val lastDayOfMonth = Calendar.getInstance().apply {
            set(initialState.calendarYear, initialState.calendarMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

        viewModel.selectDay(initialState.calendarYear, initialState.calendarMonth, lastDayOfMonth)
        viewModel.navigateCalendarMonth(1)

        val nextMonthState = viewModel.state.value
        viewModel.selectDay(nextMonthState.calendarYear, nextMonthState.calendarMonth, 5)

        val finalState = viewModel.state.value
        assertNotNull(finalState.selectedStartDateMillis)
        assertNotNull(finalState.selectedEndDateMillis)

        val startCalendar = Calendar.getInstance().apply { timeInMillis = finalState.selectedStartDateMillis!! }
        val endCalendar = Calendar.getInstance().apply { timeInMillis = finalState.selectedEndDateMillis!! }

        assertEquals(lastDayOfMonth, startCalendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(5, endCalendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(nextMonthState.calendarMonth, endCalendar.get(Calendar.MONTH))
    }
}

private class FakeSetupApi : SetupApi {
    override suspend fun getCurrentUser(): Response<UserProfileDto> {
        throw UnsupportedOperationException()
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): Response<UserProfileDto> {
        throw UnsupportedOperationException()
    }

    override suspend fun getDietTypes(): Response<List<DietTypeDto>> {
        throw UnsupportedOperationException()
    }

    override suspend fun getAllergies(): Response<List<AllergyDto>> {
        throw UnsupportedOperationException()
    }

    override suspend fun getFavorites(): Response<List<UserFavoriteDto>> {
        throw UnsupportedOperationException()
    }

    override suspend fun toggleFavorite(request: ToggleFavoriteRequest): Response<ToggleFavoriteResponse> {
        throw UnsupportedOperationException()
    }
}
