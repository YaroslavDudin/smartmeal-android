package com.example.smartmeal.feature.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.smartmeal.ui.components.buttons.SmartMealButton
import com.example.smartmeal.ui.components.buttons.SmartMealButtonVariant

@Composable
fun LoginRegisterForm(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsState()

    // Наблюдатель за успешной авторизацией
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onAuthSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLoginMode) "Вход" else "Регистрация",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(visible = !isLoginMode) {
            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it 
                    validationError = null
                },
                label = { Text("Имя пользователя") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        if (!isLoginMode) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it 
                validationError = null
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it 
                validationError = null
            },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (authState is AuthState.Loading) {
            CircularProgressIndicator()
        } else {
            SmartMealButton(
                text = if (isLoginMode) "Войти" else "Зарегистрироваться",
                onClick = {
                    validationError = null
                    
                    if (email.isBlank()) {
                        validationError = "Вы не заполнили поле email"
                        return@SmartMealButton
                    }
                    if (password.isBlank()) {
                        validationError = "Вы не заполнили поле пароль"
                        return@SmartMealButton
                    }
                    
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        validationError = "Некорректный email"
                        return@SmartMealButton
                    }
                    if (password.length < 8) {
                        validationError = "Пароль должен быть не менее 8 символов"
                        return@SmartMealButton
                    }
                    if (!isLoginMode && username.isBlank()) {
                        validationError = "Введите имя пользователя"
                        return@SmartMealButton
                    }

                    if (isLoginMode) {
                        viewModel.login(email, password)
                    } else {
                        viewModel.register(username, email, password)
                    }
                }
            )
        }

        if (validationError != null || authState is AuthState.Error) {
            val errorText = validationError ?: (authState as AuthState.Error).message
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { 
            isLoginMode = !isLoginMode 
            validationError = null
        }) {
            Text(
                text = if (isLoginMode) "Нет аккаунта? Создать" else "Уже есть аккаунт? Войти",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
