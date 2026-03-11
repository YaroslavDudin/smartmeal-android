package com.example.smartmeal.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.feature.auth.data.api.AuthApi
import com.example.smartmeal.feature.auth.data.models.LoginRequest
import com.example.smartmeal.feature.auth.data.models.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val token: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val authApi: AuthApi) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = authApi.login(LoginRequest(email, pass))
                if (response.isSuccessful) {
                    val token = response.body()?.access
                    _authState.value = AuthState.Success(token)
                } else {
                    _authState.value = AuthState.Error("Ошибка авторизации")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Ошибка сети: ${e.message}")
            }
        }
    }

    fun register(user: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val req = RegisterRequest(user, email, pass, pass) 
                val response = authApi.register(req)
                if (response.isSuccessful) {
                    login(email, pass)
                } else {
                    _authState.value = AuthState.Error("Ошибка регистрации")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Ошибка сети: ${e.message}")
            }
        }
    }
}
