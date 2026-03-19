package com.example.smartmeal.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Данные для настройки тени, аналогичные тем, что используются в дизайне.
 */
data class ShadowData(
    val radius: Dp = 8.dp,
    val spread: Dp = 0.dp,
    val color: Color = Color.Black.copy(alpha = 0.15f),
    val offset: DpOffset = DpOffset(0.dp, 2.dp)
)

/**
 * Профессиональная реализация dropShadow с поддержкой spread.
 */
fun Modifier.dropShadow(
    shape: Shape,
    shadow: ShadowData = ShadowData()
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        
        // Размытие (Radius)
        if (shadow.radius > 0.dp) {
            frameworkPaint.maskFilter = android.graphics.BlurMaskFilter(
                shadow.radius.toPx(),
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
        frameworkPaint.color = shadow.color.toArgb()
        
        // Учет Spread (расширение тени)
        val spreadRadius = shadow.spread.toPx()
        val shadowSize = size.copy(
            width = size.width + spreadRadius * 2,
            height = size.height + spreadRadius * 2
        )
        
        val outline = shape.createOutline(shadowSize, layoutDirection, this)
        
        canvas.save()
        // Смещение (Offset) + компенсация Spread для центрирования
        canvas.translate(
            shadow.offset.x.toPx() - spreadRadius,
            shadow.offset.y.toPx() - spreadRadius
        )
        canvas.drawOutline(outline, paint)
        canvas.restore()
    }
}

/**
 * Упрощенная версия для обратной совместимости.
 */
fun Modifier.softBottomShadow(
    shape: Shape,
    color: Color = Color.Black.copy(alpha = 0.12f),
    blurRadius: Dp = 3.dp,
    offsetY: Dp = 1.5.dp
): Modifier = this.dropShadow(
    shape = shape,
    shadow = ShadowData(
        radius = blurRadius,
        color = color,
        offset = DpOffset(0.dp, offsetY)
    )
)
