package com.example.smartmeal.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    secondary = AccentOrange,
    tertiary = PrimaryGreenDark,
    background = Color.White, // Оставляем белый фон для минимализма и чистоты
    onPrimary = Color.White,
    onBackground = TextBlack
)

// ... (DarkColorScheme оставь или настрой по аналогии) ...

@Composable
fun SmartMealTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Лучше отключить на этапе дизайна, чтобы цвета не перекрывались системными (Android 12+)
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LightColorScheme // Пока можно использовать светлую тему для обоих режимов, или добавить DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}