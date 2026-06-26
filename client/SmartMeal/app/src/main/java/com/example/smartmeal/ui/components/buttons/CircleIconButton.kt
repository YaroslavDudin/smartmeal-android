package com.example.smartmeal.ui.components.buttons

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartmeal.R
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.SmartMealTheme

import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape

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
    size: Int = 48,
    shape: Shape = CircleShape
) {
    // Состояние для анимации нажатия
    var isPressed by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "button_press_scale"
    )

    // Определяем цвет иконки (для стандартных иконок)
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

    // Создаем источник взаимодействий
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release -> isPressed = false
                is PressInteraction.Cancel -> isPressed = false
            }
        }
    }

    // Используем Box вместо Button, чтобы убрать ripple (эффект нажатия)
    Box(
        modifier = modifier
            .size(size.dp)
            .graphicsLayer(
                scaleX = pressScale,
                scaleY = pressScale
            )
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                // Отключаем индикацию (ripple) только для избранного
                indication = if (iconType == CircleIconType.FAVORITE) null else LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (iconType == CircleIconType.FAVORITE) {
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1f,
                animationSpec = tween(durationMillis = 300),
                label = "star_scale"
            )

            val rotation by animateFloatAsState(
                targetValue = if (isSelected) 360f else 0f,
                animationSpec = tween(durationMillis = 400),
                label = "star_rotation"
            )

            Crossfade(
                targetState = isSelected,
                animationSpec = tween(durationMillis = 300),
                label = "star_fade"
            ) { selected ->
                Icon(
                    painter = painterResource(id = if (selected) R.drawable.star else R.drawable.badstar),
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            rotationZ = rotation
                        ),
                    tint = Color.Unspecified
                )
            }
        } else {
            val icon = when (iconType) {
                CircleIconType.REPLACE -> Icons.Default.Refresh
                CircleIconType.BACK -> Icons.AutoMirrored.Filled.ArrowBack
                else -> Icons.Default.Star
            }
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
                tint = resolvedContentColor
            )
        }
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
