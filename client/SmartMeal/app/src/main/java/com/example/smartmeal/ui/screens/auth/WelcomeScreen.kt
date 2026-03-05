package com.example.smartmeal.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.data.api.SmartMealApi
import com.example.smartmeal.data.models.LoginRequest
import kotlinx.coroutines.launch

class LoginViewModel(private val api: SmartMealApi = SmartMealApi.create()) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                // В DRF ObtainAuthToken ожидает 'username' и 'password'
                // Мы передаем email как username
                val response = api.login(LoginRequest(email = email, username = email, password = password))
                // Сохранение токена (в реальном приложении нужно использовать DataStore/SharedPreferences)
                onSuccess()
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Login failed"
            } finally {
                isLoading = false
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onNavigateNext: () -> Unit,
    viewModel: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Smart Meal Login", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        
        if (viewModel.error != null) {
            Text(text = viewModel.error!!, color = MaterialTheme.colorScheme.error)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { viewModel.login(onNavigateNext) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = "Login")
            }
        }
    }
}
