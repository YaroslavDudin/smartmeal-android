package com.example.smartmeal.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = SmartMealTomato,
    secondary = SmartMealOrange,
    tertiary = SmartMealGreen,
    background = SmartMealBackground,
    surface = SmartMealSurface,
    surfaceVariant = SmartMealSurfaceSoft,
    outline = SmartMealCardBorder,
    onPrimary = Color.White,
    onSecondary = SmartMealTextColor,
    onBackground = SmartMealTextColor,
    onSurface = SmartMealTextColor,
    onSurfaceVariant = SmartMealTextSecondary
)

@Composable
fun SmartMealTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
