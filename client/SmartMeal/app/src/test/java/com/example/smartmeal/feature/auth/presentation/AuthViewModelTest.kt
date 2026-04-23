package com.example.smartmeal.feature.auth.presentation

import com.example.smartmeal.data.local.TokenManager
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.auth.data.api.AuthApi
import com.example.smartmeal.feature.auth.data.models.PasswordResetConfirmRequest
import com.example.smartmeal.feature.auth.data.models.PasswordResetRequest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val authApi = mockk<AuthApi>()
    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val setupPreferences = mockk<SetupPreferences>(relaxed = true)
    private lateinit var viewModel: AuthViewModel
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(authApi, tokenManager, setupPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `forgotPassword success sets PasswordResetSent state`() = runTest {
        val email = "test@example.com"
        val responseMap = mapOf("detail" to "Instructions sent")
        coEvery { authApi.passwordReset(PasswordResetRequest(email)) } returns Response.success(responseMap)

        viewModel.forgotPassword(email)
        advanceUntilIdle()

        assertTrue(viewModel.authState.value is AuthState.PasswordResetSent)
        assertEquals("Instructions sent", (viewModel.authState.value as AuthState.PasswordResetSent).message)
    }

    @Test
    fun `forgotPassword failure sets Error state`() = runTest {
        val email = "test@example.com"
        // AuthViewModel expects a JSON with "detail" or other keys
        val errorJson = "{\"detail\": \"email_not_found\"}"
        val contentType = "application/json".toMediaType()
        coEvery { authApi.passwordReset(any()) } returns Response.error(404, okhttp3.ResponseBody.create(contentType, errorJson))

        viewModel.forgotPassword(email)
        advanceUntilIdle()

        assertTrue(viewModel.authState.value is AuthState.Error)
        assertEquals("Аккаунт с таким email не зарегистрирован", (viewModel.authState.value as AuthState.Error).message)
    }

    @Test
    fun `resetPasswordConfirm success sets PasswordResetConfirmed state`() = runTest {
        val uid = "uid"
        val token = "token"
        val pass = "newpassword123"
        val responseMap = mapOf("detail" to "Password changed")
        
        coEvery { 
            authApi.passwordResetConfirm(PasswordResetConfirmRequest(uid, token, pass, pass)) 
        } returns Response.success(responseMap)

        viewModel.resetPasswordConfirm(uid, token, pass, pass)
        advanceUntilIdle()

        assertTrue(viewModel.authState.value is AuthState.PasswordResetConfirmed)
        assertEquals("Password changed", (viewModel.authState.value as AuthState.PasswordResetConfirmed).message)
    }

    @Test
    fun `logout clears all data`() = runTest {
        every { tokenManager.getRefreshToken() } returns "fake_refresh_token"
        coEvery { authApi.logout(any()) } returns Response.success(Unit)

        viewModel.logout()
        advanceUntilIdle()

        verify { setupPreferences.clearAll() }
        verify { tokenManager.clearTokens() }
    }
}
