package com.example.smartmeal.feature.setup.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smartmeal.R
import com.example.smartmeal.ui.components.buttons.SmartMealButton
import com.example.smartmeal.ui.components.buttons.SmartMealButtonColor
import com.example.smartmeal.ui.components.buttons.SmartMealButtonVariant

/**
 * Экран-заставка после авторизации.
 * Пока ViewModel проверяет профиль (isCheckingUser=true) — показывает загрузку.
 * Если пользователь уже настроен (isUserAlreadyConfigured=true) — сразу отправляет на Home.
 * Иначе — показывает приветствие и кнопку «Начать» для перехода к шагу 1.
 */
@Composable
fun SetupIntroScreen(
    viewModel: SetupViewModel,
    onStartSetup: () -> Unit,
    onAlreadyConfigured: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Загружаем данные только когда пользователь реально дошёл до экрана (после логина)
    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    // Auto-skip if user is already configured
    LaunchedEffect(state.isCheckingUser, state.isUserAlreadyConfigured) {
        if (!state.isCheckingUser && state.isUserAlreadyConfigured) {
            onAlreadyConfigured()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (state.isCheckingUser) {
            CircularProgressIndicator(color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // App logo / illustration
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "SmartMeal",
                        modifier = Modifier.size(160.dp),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "SmartMeal",
                        style = MaterialTheme.typography.headlineLarge,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Сгенерируйте своё недельное\nменю за пару минут",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }

                SmartMealButton(
                    text = "Начать",
                    onClick = onStartSetup,
                    modifier = Modifier.fillMaxWidth(),
                    variant = SmartMealButtonVariant.PRIMARY,
                    color = SmartMealButtonColor.GREEN,
                )
            }
        }
    }
}
