package com.example.smartmeal.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.data.local.TokenManager
import com.example.smartmeal.feature.auth.data.api.AuthApi
import com.example.smartmeal.feature.auth.data.models.LoginRequest
import com.example.smartmeal.feature.auth.data.models.RefreshRequest
import com.example.smartmeal.feature.auth.data.models.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val token: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
    data class PasswordResetSent(val message: String) : AuthState()
    data class PasswordResetConfirmed(val message: String) : AuthState()
}

class AuthViewModel(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val setupPreferences: com.example.smartmeal.data.local.SetupPreferences
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, pass: String) {
        if (!validateLogin(email, pass)) return

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = authApi.login(LoginRequest(email, pass))
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    val access = loginResponse?.access
                    val refresh = loginResponse?.refresh
                    if (access != null && refresh != null) {
                        tokenManager.saveTokens(access, refresh)
                        setupPreferences.setActiveUserKey(email) // Восстанавливаем профиль по email
                        _authState.value = AuthState.Success(access)
                    } else {
                        _authState.value = AuthState.Error("Пустой ответ от сервера")
                    }
                } else {
                    _authState.value = AuthState.Error(parseError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Ошибка сети: ${e.message}")
            }
        }
    }

    fun register(user: String, email: String, pass: String, passConfirm: String) {
        if (!validateRegister(user, email, pass, passConfirm)) return

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val req = RegisterRequest(user, email, pass, passConfirm)
                val response = authApi.register(req)
                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    val access = registerResponse?.access
                    val refresh = registerResponse?.refresh
                    if (access != null && refresh != null) {
                        tokenManager.saveTokens(access, refresh)
                        setupPreferences.setActiveUserKey(email) // Восстанавливаем профиль по email
                        _authState.value = AuthState.Success(access)
                    } else {
                        _authState.value = AuthState.Error("Ошибка: токены не получены")
                    }
                } else {
                    _authState.value = AuthState.Error(parseError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Ошибка сети: ${e.message}")
            }
        }
    }

    private fun validateLogin(email: String, pass: String): Boolean {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Вы не заполнили поле email")
            return false
        }
        if (pass.isBlank()) {
            _authState.value = AuthState.Error("Вы не заполнили поле пароль")
            return false
        }
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        if (!email.matches(emailRegex)) {
            _authState.value = AuthState.Error("Некорректный email")
            return false
        }
        return true
    }

    private fun validateRegister(user: String, email: String, pass: String, passConfirm: String): Boolean {
        if (!validateLogin(email, pass)) return false
        if (user.isBlank()) {
            _authState.value = AuthState.Error("Введите имя пользователя")
            return false
        }
        if (pass.length < 8) {
            _authState.value = AuthState.Error("Пароль должен быть не менее 8 символов")
            return false
        }
        if (pass != passConfirm) {
            _authState.value = AuthState.Error("Пароли не совпадают")
            return false
        }
        return true
    }

    private fun parseError(errorJson: String?): String {
        if (errorJson.isNullOrBlank()) return "Неизвестная ошибка"
        
        return try {
            // Упрощенный поиск сообщения в JSON без использования org.json
            fun extractValue(key: String): String? {
                val regex = "\"$key\":\\s*\"([^\"]+)\"".toRegex()
                return regex.find(errorJson)?.groupValues?.get(1) ?: run {
                    val arrayRegex = "\"$key\":\\s*\\[\\s*\"([^\"]+)\"".toRegex()
                    arrayRegex.find(errorJson)?.groupValues?.get(1)
                }
            }

            fun translate(text: String, fieldName: String = ""): String {
                val t = text.lowercase()
                return when {
                    t.contains("email_not_found") -> "Аккаунт с таким email не зарегистрирован"
                    t.contains("wrong_password") -> "Пароль неверный"
                    t.contains("already exists") -> if (fieldName == "email") {
                        "Этот email уже зарегистрирован"
                    } else {
                        "Это имя уже занято"
                    }
                    t.contains("required") || t.contains("blank") -> "Это поле обязательно для заполнения"
                    t.contains("valid email") -> "Введите корректный адрес электронной почты"
                    t.contains("too short") || t.contains("at least") -> "Слишком короткий текст (минимум 8 символов)"
                    t.contains("too common") -> "Пароль слишком простой"
                    t.contains("numeric") -> "Пароль не может состоять только из цифр"
                    t.contains("don't match") -> "Пароли не совпадают"
                    t.contains("no active account found") -> "Аккаунт с такими данными не найден"
                    t.contains("token is invalid") -> "Сессия истекла, войдите снова"
                    else -> text
                }
            }

            val detail = extractValue("detail")
            val nonField = extractValue("non_field_errors")
            val emailErr = extractValue("email")
            val userErr = extractValue("username")
            val passErr = extractValue("password")
            val confirmErr = extractValue("password_confirm")

            when {
                detail != null -> translate(detail)
                nonField != null -> translate(nonField)
                emailErr != null -> "Email: " + translate(emailErr, "email")
                userErr != null -> "Имя пользователя: " + translate(userErr, "username")
                passErr != null -> "Пароль: " + translate(passErr, "password")
                confirmErr != null -> "Подтверждение пароля: " + translate(confirmErr)
                else -> "Ошибка сервера: $errorJson"
            }
        } catch (e: Exception) {
            "Неизвестная ошибка: $errorJson"
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Введите email")
            return
        }
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        if (!email.matches(emailRegex)) {
            _authState.value = AuthState.Error("Некорректный email")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = authApi.passwordReset(com.example.smartmeal.feature.auth.data.models.PasswordResetRequest(email))
                if (response.isSuccessful) {
                    _authState.value = AuthState.PasswordResetSent(response.body()?.get("detail") ?: "Инструкции отправлены на почту")
                } else {
                    _authState.value = AuthState.Error(parseError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Ошибка сети: ${e.message}")
            }
        }
    }

    fun resetPasswordConfirm(uid: String, token: String, pass: String, passConfirm: String) {
        if (pass.length < 8) {
            _authState.value = AuthState.Error("Пароль должен быть не менее 8 символов")
            return
        }
        if (pass != passConfirm) {
            _authState.value = AuthState.Error("Пароли не совпадают")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val req = com.example.smartmeal.feature.auth.data.models.PasswordResetConfirmRequest(uid, token, pass, passConfirm)
                val response = authApi.passwordResetConfirm(req)
                if (response.isSuccessful) {
                    _authState.value = AuthState.PasswordResetConfirmed(response.body()?.get("detail") ?: "Пароль успешно изменен")
                } else {
                    _authState.value = AuthState.Error(parseError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Ошибка сети: ${e.message}")
            }
        }
    }

    fun logout() {
        _authState.value = AuthState.Idle
        val refreshToken = tokenManager.getRefreshToken()
        
        // Очищаем только сессию текущего пользователя, НЕ удаляя базу данных настроек
        setupPreferences.clearActiveUserKey() 
        com.example.smartmeal.feature.home.data.MenuRepository.clearCache()
        com.example.smartmeal.data.manager.DateManager.clear()
        com.example.smartmeal.data.manager.MenuSyncManager.clear()
        com.example.smartmeal.data.manager.FavoritesManager.clear()
        com.example.smartmeal.data.manager.MealSlotManager.clear()
        
        if (refreshToken != null) {
            viewModelScope.launch {
                try {
                    authApi.logout(RefreshRequest(refreshToken))
                } catch (e: Exception) {
                    // Игнорируем ошибки при выходе (например, отсутствие сети)
                } finally {
                    tokenManager.clearTokens()
                    _authState.value = AuthState.Idle
                }
            }
        } else {
            tokenManager.clearTokens()
            _authState.value = AuthState.Idle
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}
