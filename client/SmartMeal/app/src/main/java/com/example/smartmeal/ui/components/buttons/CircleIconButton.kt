package com.example.smartmeal.ui.components.buttons

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.SmartMealTheme

enum class CircleIconType {
    FAVORITE,
    REPLACE,
    BACK
}

@Composable
fun CircleIconButton(
    iconType: CircleIconType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    backgroundColor: Color = Color.White,
    contentColor: Color? = null,
    size: Int = 48
) {
    // Состояние для анимации нажатия
    var isPressed by remember { mutableStateOf(false) }

    // Анимируем размер кнопки при нажатии
    val animatedSize by animateDpAsState(
        targetValue = if (isPressed) (size - 4).dp else size.dp,
        animationSpec = tween(durationMillis = 100),
        label = "button_size_animation"
    )

    // Выбираем иконку
    val icon = when (iconType) {
        CircleIconType.FAVORITE -> {
            if (isSelected) Icons.Filled.Star
            else Icons.Outlined.StarBorder
        }
        CircleIconType.REPLACE -> Icons.Filled.Refresh
        CircleIconType.BACK -> Icons.AutoMirrored.Filled.ArrowBack
    }

    // Определяем цвет иконки
    val resolvedContentColor = contentColor ?: when (iconType) {
        CircleIconType.FAVORITE -> if (isSelected) Color(0xFFFFD700) else Color.Black
        else -> PrimaryGreen
    }

    // Описание для accessibility
    val contentDescription = when (iconType) {
        CircleIconType.FAVORITE -> if (isSelected) "Убрать из избранного" else "Добавить в избранное"
        CircleIconType.REPLACE -> "Заменить"
        CircleIconType.BACK -> "Назад"
    }

    // Создаем источник взаимодействий для отслеживания нажатий
    val interactionSource = remember { MutableInteractionSource() }

    // Следим за нажатиями
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release -> isPressed = false
                is PressInteraction.Cancel -> isPressed = false
            }
        }
    }

    Button(
        onClick = onClick,
        modifier = modifier.size(animatedSize),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = resolvedContentColor
        ),
        interactionSource = interactionSource ,
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CircleIconButtonPreview() {
    SmartMealTheme {
        Box(modifier = Modifier.size(100.dp)) {
            CircleIconButton(
                iconType = CircleIconType.BACK,
                onClick = {},
                isSelected = true
            )
        }
    }
}

//CircleIconButton(
//    iconType = CircleIconType.BACK,
//    onClick = { navController.popBackStack() }
//)
