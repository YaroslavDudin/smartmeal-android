package com.example.smartmeal.feature.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.R
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.buttons.SmartMealButton
import com.example.smartmeal.ui.components.buttons.SmartMealButtonColor
import com.example.smartmeal.ui.components.buttons.SmartMealButtonVariant
import com.example.smartmeal.ui.theme.BgLightGray
import com.example.smartmeal.ui.theme.BorderGray
import com.example.smartmeal.ui.theme.HintGray
import com.example.smartmeal.ui.theme.IconSize
import com.example.smartmeal.ui.theme.Padding
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.SurfaceGray

@Composable
fun LoginRegisterForm(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onNavigateToSandbox: () -> Unit = {}
) {
    val authState by viewModel.authState.collectAsState()

    LoginRegisterFormContent(
        authState = authState,
        onAuthSuccess = onAuthSuccess,
        onLogin = { email, pass -> viewModel.login(email, pass) },
        onRegister = { user, email, pass, confirm -> viewModel.register(user, email, pass, confirm) },
        onNavigateToSandbox = onNavigateToSandbox
    )
}

@Composable
fun LoginRegisterFormContent(
    authState: AuthState,
    onAuthSuccess: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onNavigateToSandbox: () -> Unit = {},
    initialIsLoginMode: Boolean = true,
) {
    var isLoginMode by remember { mutableStateOf(initialIsLoginMode) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Наблюдатель за успешной авторизацией
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLightGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Padding.SCREEN),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 1. Иллюстрация сверху
            Image(
                painter = painterResource(id = R.drawable.food),
                contentDescription = "Food logo",
                modifier = Modifier
                    .size(IconSize.LOGO)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Заголовок
            SmartMealText(
                text = if (isLoginMode) "Вход" else "Регистрация",
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Кастомный переключатель (Tabs)
            AuthToggleSwitch(
                isLoginMode = isLoginMode,
                onToggle = { isLoginMode = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Поля ввода
            if (!isLoginMode) {
                CustomTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Имя",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_username")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            CustomTextField(
                value = email,
                onValueChange = { email = it },
                label = "Электронная почта",
                keyboardType = KeyboardType.Email,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email")
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomTextField(
                value = password,
                onValueChange = { password = it },
                label = "Пароль",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordVisibilityChange = { isPasswordVisible = !isPasswordVisible },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password")
            )

            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Подтвердите пароль",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    isPasswordVisible = isConfirmPasswordVisible,
                    onPasswordVisibilityChange = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_confirm_password")
                )
            }

            // 5. Забыли пароль? (только для входа)
            if (isLoginMode) {
                Spacer(modifier = Modifier.height(12.dp))
                SmartMealText(
                    text = "Забыли пароль?",
                    color = PrimaryGreen,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .testTag("auth_forgot")
                        .clickable { /* TODO: Forgot Password */ }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 6. Ошибки
            if (authState is AuthState.Error) {
                SmartMealText(
                    text = authState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // 7. Кнопка действия
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = PrimaryGreen)
            } else {
                SmartMealButton(
                    text = if (isLoginMode) "Войти" else "Создать аккаунт",
                    onClick = {
                        if (isLoginMode) {
                            onLogin(email, password)
                        } else {
                            onRegister(username, email, password, confirmPassword)
                        }
                    },
                    variant = SmartMealButtonVariant.PRIMARY,
                    color = SmartMealButtonColor.GREEN,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_submit")
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

/*
            TextButton(
                onClick = onNavigateToSandbox,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                SmartMealText("Перейти в Sandbox", color = PrimaryGreen)
            }
*/
        }
    }
}

@Composable
fun AuthToggleSwitch(
    isLoginMode: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(SurfaceGray, RoundedCornerShape(25.dp))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Кнопка "Вход"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (isLoginMode) Color.White else Color.Transparent)
                    .shadow(if (isLoginMode) 1.dp else 0.dp, RoundedCornerShape(25.dp))
                    .testTag("auth_toggle_login")
                    .clickable { onToggle(true) },
                contentAlignment = Alignment.Center
            ) {
                SmartMealText(
                    text = "Вход",
                    color = if (isLoginMode) Color.Black else HintGray,
                    fontWeight = if (isLoginMode) FontWeight.Medium else FontWeight.Normal
                )
            }
            // Кнопка "Регистрация"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (!isLoginMode) Color.White else Color.Transparent)
                    .shadow(if (!isLoginMode) 1.dp else 0.dp, RoundedCornerShape(25.dp))
                    .testTag("auth_toggle_register")
                    .clickable { onToggle(false) },
                contentAlignment = Alignment.Center
            ) {
                SmartMealText(
                    text = "Регистрация",
                    color = if (!isLoginMode) Color.Black else HintGray,
                    fontWeight = if (!isLoginMode) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityChange: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            SmartMealText(
                text = label,
                color = HintGray,
                fontSize = 16.sp
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = BorderGray,
            focusedBorderColor = PrimaryGreen,
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = onPasswordVisibilityChange) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = HintGray,
                        modifier = Modifier.size(IconSize.MEDIUM)
                    )
                }
            }
        }
    )
}
