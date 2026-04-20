package com.example.smartmeal.feature.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.smartmeal.ui.theme.*

@Composable
fun LoginRegisterForm(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onForgotPassword: () -> Unit,
    onNavigateToSandbox: () -> Unit = {}
) {
    val authState by viewModel.authState.collectAsState()

    LoginRegisterFormContent(
        authState = authState,
        onAuthSuccess = onAuthSuccess,
        onLogin = { email, pass -> viewModel.login(email, pass) },
        onRegister = { user, email, pass, confirm -> viewModel.register(user, email, pass, confirm) },
        onNavigateToSandbox = onNavigateToSandbox,
        onForgotPasswordClick = onForgotPassword
    )
}

@Composable
fun LoginRegisterFormContent(
    authState: AuthState,
    onAuthSuccess: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onNavigateToSandbox: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    initialIsLoginMode: Boolean = true,
) {
    var isLoginMode by remember { mutableStateOf(initialIsLoginMode) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val (emailFocus, usernameFocus, passwordFocus, confirmFocus) = remember { FocusRequester.createRefs() }

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

            Image(
                painter = painterResource(id = R.drawable.food),
                contentDescription = "Food logo",
                modifier = Modifier
                    .size(IconSize.LOGO)
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("food_logo"),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            SmartMealText(
                text = if (isLoginMode) "Вход" else "Регистрация",
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            AuthToggleSwitch(
                isLoginMode = isLoginMode,
                onToggle = { 
                    isLoginMode = it 
                    focusManager.clearFocus()
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isLoginMode) {
                CustomTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Имя",
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(usernameFocus)
                        .testTag("auth_username")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            CustomTextField(
                value = email,
                onValueChange = { email = it },
                label = "Электронная почта",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(emailFocus)
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
                imeAction = if (isLoginMode) ImeAction.Done else ImeAction.Next,
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { 
                        focusManager.clearFocus()
                        if (isLoginMode) onLogin(email, password)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocus)
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
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { 
                        focusManager.clearFocus()
                        onRegister(username, email, password, confirmPassword)
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(confirmFocus)
                        .testTag("auth_confirm_password")
                )
            }

            if (isLoginMode) {
                Spacer(modifier = Modifier.height(12.dp))
                SmartMealText(
                    text = "Забыли пароль?",
                    color = PrimaryGreen,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .testTag("auth_forgot")
                        .clickable { onForgotPasswordClick() }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (authState is AuthState.Error) {
                SmartMealText(
                    text = authState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("auth_error"),
                    textAlign = TextAlign.Center
                )
            }

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
                        .fillMaxWidth()
                        .testTag("auth_submit")
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
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
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
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
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
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
