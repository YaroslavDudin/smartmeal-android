package com.example.smartmeal.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Константы для иконок и кнопок во всем приложении
 * Чтобы размеры были единообразными
 */
object IconConstants {
    // Размеры кнопок
    const val BUTTON_SIZE_SMALL = 40
    const val BUTTON_SIZE_MEDIUM = 48
    const val BUTTON_SIZE_LARGE = 56

    // Размеры иконок внутри кнопок
    const val ICON_SIZE_SMALL = 20
    const val ICON_SIZE_MEDIUM = 24
    const val ICON_SIZE_LARGE = 32

    // DP версии для Compose
    val BUTTON_SIZE_SMALL_DP = 40.dp
    val BUTTON_SIZE_MEDIUM_DP = 48.dp
    val BUTTON_SIZE_LARGE_DP = 56.dp

    val ICON_SIZE_SMALL_DP = 20.dp
    val ICON_SIZE_MEDIUM_DP = 24.dp
    val ICON_SIZE_LARGE_DP = 32.dp

    // Анимация
    const val ANIMATION_DURATION = 100
    const val PRESSED_SCALE_FACTOR = 0.9f
    const val PRESSED_SIZE_OFFSET = 4
}

//// Маленькая (40dp)
//CircleIconButton(
//    iconType = CircleIconType.FAVORITE,
//    onClick = { /* действие */ },
//    size = IconConstants.BUTTON_SIZE_SMALL
//)
//
//// Средняя (48dp) - можно не указывать, это значение по умолчанию
//CircleIconButton(
//    iconType = CircleIconType.REPLACE,
//    onClick = { /* действие */ }
//)
//
//// Большая (56dp)
//CircleIconButton(
//    iconType = CircleIconType.BACK,
//    onClick = { /* действие */ },
//    size = IconConstants.BUTTON_SIZE_LARGE
//)