package com.example.smartmeal.feature.setup.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import com.example.smartmeal.R
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.buttons.SmartMealButton
import com.example.smartmeal.ui.components.buttons.SmartMealButtonColor
import com.example.smartmeal.ui.components.buttons.SmartMealButtonVariant
import com.example.smartmeal.ui.theme.*

import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.smartmeal.ui.components.feedback.ExitConfirmDialog

/**
 * Экран-заставка после авторизации.
 */
@Composable
fun SetupIntroScreen(
    viewModel: SetupViewModel,
    onStartSetup: () -> Unit,
    onAlreadyConfigured: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    // Перехват кнопки "Назад" для подтверждения выхода
    BackHandler(enabled = true) {
        showExitDialog = true
    }

    if (showExitDialog) {
        ExitConfirmDialog(
            onConfirm = {
                (context as? android.app.Activity)?.finish()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    // Загружаем данные только когда пользователь реально дошёл до экрана
    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    // Auto-skip if user is already configured
    LaunchedEffect(state.isCheckingUser, state.isUserAlreadyConfigured) {
        if (!state.isCheckingUser && state.isUserAlreadyConfigured) {
            onAlreadyConfigured()
        }
    }

    SetupIntroContent(
        state = state,
        onStartSetup = onStartSetup
    )
}

@Composable
fun SetupIntroContent(
    state: SetupState,
    onStartSetup: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        if (state.isCheckingUser) {
            CircularProgressIndicator(
                color = PrimaryGreen,
                modifier = Modifier.testTag("setup_intro_loading")
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Padding.SCREEN),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.5f))

                // App logo / illustration
                Image(
                    painter = painterResource(id = R.drawable.food),
                    contentDescription = "Food illustration",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .widthIn(max = 300.dp) // Ограничиваем макс ширину для планшетов
                        .fillMaxWidth()
                        .weight(3f)
                        .testTag("setup_intro_image")
                )

                Spacer(modifier = Modifier.weight(0.5f))

                // Заголовок
                SmartMealText(
                    text = "Гибкий Рацион",
                    fontSize = TextSize.HERO,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    lineHeight = TextSize.HERO_LINE_HEIGHT,
                    modifier = Modifier.testTag("setup_intro_title")
                )

                Spacer(modifier = Modifier.height(Padding.MEDIUM))

                // Описание
                SmartMealText(
                    text = "Сгенерируйте своё недельное\nменю за пару минут",
                    fontSize = TextSize.BODY,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("setup_intro_subtitle")
                )

                Spacer(modifier = Modifier.weight(1f))

                // Кнопка
                SmartMealButton(
                    text = "Начать",
                    onClick = onStartSetup,
                    variant = SmartMealButtonVariant.PRIMARY,
                    color = SmartMealButtonColor.GREEN,
                    modifier = Modifier.testTag("setup_intro_start")
                )

                Spacer(modifier = Modifier.height(Padding.BOTTOM_SPACE))
            }
        }
    }
}
