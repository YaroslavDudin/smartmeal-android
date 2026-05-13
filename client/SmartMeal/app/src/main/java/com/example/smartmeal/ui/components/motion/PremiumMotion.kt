package com.example.smartmeal.ui.components.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

private fun premiumEnterTransition(offsetY: Int): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
    ) + slideInVertically(
        initialOffsetY = { offsetY },
        animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing)
    ) + scaleIn(
        initialScale = 0.985f,
        animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing)
    )
}

private fun premiumExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = 180)
    ) + scaleOut(
        targetScale = 1.01f,
        animationSpec = tween(durationMillis = 180)
    )
}

@Composable
fun PremiumReveal(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    offsetY: Int = 36,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (delayMillis > 0) {
            delay(delayMillis.toLong())
        }
        visible = true
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = visible,
            enter = premiumEnterTransition(offsetY = offsetY),
            exit = premiumExitTransition()
        ) {
            content()
        }
    }
}

@Composable
fun PremiumScreen(
    modifier: Modifier = Modifier,
    delayMillis: Int = 40,
    content: @Composable () -> Unit
) {
    PremiumReveal(
        modifier = modifier,
        delayMillis = delayMillis,
        offsetY = 56,
        content = content
    )
}
