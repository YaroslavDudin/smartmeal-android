package com.example.smartmeal.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.feature.auth.data.api.AuthApi
import com.example.smartmeal.feature.auth.data.models.LoginRequest
import com.example.smartmeal.feature.auth.data.models.RegisterRequest
import com.example.smartmeal.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val token: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
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
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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
        return try {
            val json = JSONObject(errorJson ?: "{}")

            fun getMsg(key: String): String {
                val obj = json.opt(key)
                return when (obj) {
                    is org.json.JSONArray -> obj.optString(0)
                    null -> ""
                    else -> obj.toString()
                }
            }

            fun translate(text: String, fieldName: String = ""): String {
                val t = text.lowercase()
                return when {
                    t.contains("email_not_found") -> "Аккаунт с таким email не зарегистрирован"
                    t.contains("wrong_password") -> "Пароль не верный"
                    t.contains("already exists") -> if (fieldName == "email") "Этот email уже зарегистрирован" else "Это имя уже занято"
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

            when {
                json.has("detail") -> translate(getMsg("detail"))
                json.has("non_field_errors") -> translate(getMsg("non_field_errors"))
                json.has("email") -> "Email: " + translate(getMsg("email"), "email")
                json.has("username") -> "Имя пользователя: " + translate(getMsg("username"), "username")
                json.has("password") -> "Пароль: " + translate(getMsg("password"), "password")
                json.has("password_confirm") -> "Подтверждение пароля: " + translate(getMsg("password_confirm"))
                else -> {
                    val firstKey = json.keys().asSequence().firstOrNull()
                    if (firstKey != null) {
                        translate(getMsg(firstKey), firstKey)
                    } else {
                        "Ошибка сервера: $errorJson"
                    }
                }
            }
        } catch (e: Exception) {
            "Неизвестная ошибка: $errorJson"
        }
    }

    fun logout() {
        _authState.value = AuthState.Idle
        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken != null) {
            viewModelScope.launch {
                try {
                    authApi.logout(com.example.smartmeal.feature.auth.data.models.RefreshRequest(refreshToken))
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
