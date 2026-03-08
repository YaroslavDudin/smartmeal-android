package com.example.smartmeal.ui.components.feedback

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartmeal.ui.theme.SmartMealTheme

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.3f)
        ),
        start = Offset.Zero,
        end = Offset(x = shimmerTranslate.value, y = shimmerTranslate.value)
    )

    this.background(brush = brush)
}

@Composable
fun ShimmerRectangle(
    modifier: Modifier = Modifier,
    width: Int? = null,
    height: Int = 20
) {
    val finalModifier = if (width != null) {
        modifier.width(width.dp).height(height.dp)
    } else {
        modifier.fillMaxWidth().height(height.dp)
    }

    Box(
        modifier = finalModifier
            .clip(RoundedCornerShape(8.dp))
            .shimmerEffect()
    )
}

@Composable
fun ShimmerCircle(
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .shimmerEffect()
    )
}

@Composable
fun RecipeCardShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Изображение
        ShimmerRectangle(height = 200)

        Spacer(modifier = Modifier.height(8.dp))

        // Заголовок
        ShimmerRectangle(width = 200, height = 24)

        Spacer(modifier = Modifier.height(4.dp))

        // Описание
        ShimmerRectangle(height = 16)
        ShimmerRectangle(width = 150, height = 16)
    }
}

@Preview(showBackground = true)
@Composable
fun ShimmerPreview() {
    SmartMealTheme {
        Column {
            RecipeCardShimmer()
            ShimmerCircle(size = 64)
        }
    }
}

//if (isLoading) {
//    RecipeCardShimmer()
//} else {
//    // Показываем реальные данные
//}