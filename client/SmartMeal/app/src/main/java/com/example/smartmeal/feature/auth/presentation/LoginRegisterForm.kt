package com.example.smartmeal.feature.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.R
import com.example.smartmeal.ui.components.buttons.SmartMealButton
import com.example.smartmeal.ui.components.buttons.SmartMealButtonColor
import com.example.smartmeal.ui.components.buttons.SmartMealButtonVariant
import com.example.smartmeal.ui.theme.*

@Composable
fun LoginRegisterForm(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    LoginRegisterFormContent(
        authState = authState,
        onAuthSuccess = onAuthSuccess,
        onLogin = { email, pass -> viewModel.login(email, pass) },
        onRegister = { user, email, pass, passConfirm ->
            viewModel.register(user, email, pass, passConfirm)
        }
    )
}

@Composable
fun LoginRegisterFormContent(
    authState: AuthState,
    onAuthSuccess: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
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

    val configuration = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 600
    val isCompactWidth = configuration.screenWidthDp < 360

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLightGray)
    ) {
        val horizontalPadding = if (isCompactHeight || isCompactWidth) 16.dp else Padding.SCREEN
        val topSpacing = if (isCompactHeight) 12.dp else 40.dp
        val imageSize = if (isCompactHeight) 72.dp else IconSize.LOGO
        val titleSize = if (isCompactHeight) 22.sp else 28.sp
        val toggleHeight = if (isCompactHeight) 44.dp else 50.dp
        val sectionSpacing = if (isCompactHeight) 16.dp else 24.dp
        val fieldSpacing = if (isCompactHeight) 8.dp else 16.dp
        val fieldHeight = if (isCompactHeight) 48.dp else 60.dp
        val buttonTopSpacing = if (isCompactHeight) 20.dp else 40.dp
        val bottomSpacing = if (isCompactHeight) 24.dp else 40.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(topSpacing))

            // 1. Иллюстрация сверху
            Image(
                painter = painterResource(id = R.drawable.food),
                contentDescription = "Food logo",
                modifier = Modifier
                    .size(imageSize)
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("auth_logo"),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            // 2. Заголовок
            Text(
                text = if (isLoginMode) "Вход" else "Регистрация",
                fontSize = titleSize,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("auth_title")
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            // 3. Кастомный переключатель (Tabs)
            AuthToggleSwitch(
                isLoginMode = isLoginMode,
                onToggle = { isLoginMode = it },
                loginTag = "auth_toggle_login",
                registerTag = "auth_toggle_register",
                height = toggleHeight
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            // 4. Поля ввода
            if (!isLoginMode) {
                CustomTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Имя",
                    height = fieldHeight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_username")
                )
                Spacer(modifier = Modifier.height(fieldSpacing))
            }

            CustomTextField(
                value = email,
                onValueChange = { email = it },
                label = "Электронная почта",
                keyboardType = KeyboardType.Email,
                height = fieldHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email")
            )

            Spacer(modifier = Modifier.height(fieldSpacing))

            CustomTextField(
                value = password,
                onValueChange = { password = it },
                label = "Пароль",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordVisibilityChange = { isPasswordVisible = !isPasswordVisible },
                height = fieldHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password")
            )

            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(fieldSpacing))
                CustomTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Подтвердите пароль",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    isPasswordVisible = isConfirmPasswordVisible,
                    onPasswordVisibilityChange = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                    height = fieldHeight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_confirm_password")
                )
            }

            // 5. Забыли пароль? (Только для входа)
            if (isLoginMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Забыли пароль?",
                    color = PrimaryGreen,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .clickable { /* TODO: Forgot Password */ }
                        .testTag("auth_forgot")
                )
            }

            Spacer(modifier = Modifier.height(buttonTopSpacing))

            // 6. Ошибки
            if (authState is AuthState.Error) {
                Text(
                    text = authState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("auth_error"),
                    textAlign = TextAlign.Center
                )
            }

            // 7. Кнопка действия
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    color = PrimaryGreen,
                    modifier = Modifier.testTag("auth_loading")
                )
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
                        .shadow(4.dp, RoundedCornerShape(24.dp))
                        .testTag("auth_submit")
                )
            }

            Spacer(modifier = Modifier.height(bottomSpacing))
        }
    }
}

@Composable
fun AuthToggleSwitch(
    isLoginMode: Boolean,
    onToggle: (Boolean) -> Unit,
    loginTag: String,
    registerTag: String,
    height: Dp = 50.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
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
                    .shadow(if (isLoginMode) 2.dp else 0.dp, RoundedCornerShape(25.dp))
                    .clickable { onToggle(true) }
                    .testTag(loginTag),
                contentAlignment = Alignment.Center
            ) {
                Text(
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
                    .shadow(if (!isLoginMode) 2.dp else 0.dp, RoundedCornerShape(25.dp))
                    .clickable { onToggle(false) }
                    .testTag(registerTag),
                contentAlignment = Alignment.Center
            ) {
                Text(
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
    onPasswordVisibilityChange: () -> Unit = {},
    height: Dp = 60.dp
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = label,
                color = HintGray,
                fontSize = 16.sp
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .height(height),
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
