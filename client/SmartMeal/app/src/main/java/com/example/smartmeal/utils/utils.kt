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
import androidx.compose.ui.unit.dp

/**
 * Реализация тени согласно документации Android:
 * Использует drawBehind, Paint и BlurMaskFilter для создания мягкой тени строго снизу.
 */
fun Modifier.softBottomShadow(
    shape: Shape,
    color: Color = Color.Black.copy(alpha = 0.15f),
    blurRadius: Dp = 3.dp,
    offsetY: Dp = 1.5.dp
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()

        if (blurRadius > 0.dp) {
            frameworkPaint.maskFilter = android.graphics.BlurMaskFilter(
                blurRadius.toPx(),
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
        frameworkPaint.color = color.toArgb()

        val outline = shape.createOutline(size, layoutDirection, this)

        canvas.save()
        canvas.translate(0f, offsetY.toPx())
        canvas.drawOutline(outline, paint)
        canvas.restore()
    }
}
