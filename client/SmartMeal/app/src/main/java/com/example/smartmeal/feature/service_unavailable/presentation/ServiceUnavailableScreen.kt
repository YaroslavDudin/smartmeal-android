package com.example.smartmeal.feature.service_unavailable.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText
import kotlinx.coroutines.launch

private val ServiceBackground = Color(0xFFFAFAFA)
private val ServiceTitle = Color(0xFF222222)
private val ServiceBody = Color(0xFF4F4F4F)
private val ServiceYellow = Color(0xFFF3E35D)
private val ServiceRed = Color(0xFFE96D6D)
private val ServiceAction = Color(0xFFE8A24D)

@Composable
fun ServiceUnavailableScreen(
    onRefresh: suspend () -> Boolean,
    onRecovered: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    BackHandler(enabled = true) {}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ServiceBackground)
            .statusBarsPadding()
            .padding(horizontal = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ServiceUnavailableIllustration(
                modifier = Modifier.size(116.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            SmartMealText(
                text = "Нестабильная работа сервисов",
                color = ServiceTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            SmartMealText(
                text = "Приносим извинения, наши сервисы временно недоступны. Мы уже исправляем проблему. Попробуйте обновить или повторите попытку позже.",
                color = ServiceBody,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 25.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(26.dp))

            TextButton(
                enabled = !isChecking,
                onClick = {
                    scope.launch {
                        isChecking = true
                        val recovered = onRefresh()
                        isChecking = false
                        if (recovered) {
                            onRecovered()
                        }
                    }
                }
            ) {
                AnimatedVisibility(
                    visible = isChecking,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(18.dp),
                        color = ServiceAction,
                        strokeWidth = 2.dp
                    )
                }
                SmartMealText(
                    text = "Обновить",
                    color = ServiceAction,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ServiceUnavailableIllustration(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val yellowStroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val redStroke = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val cx = w / 2f

        val bodyTop = h * 0.43f
        val bodyBottom = h * 0.92f
        val bodyLeft = w * 0.31f
        val bodyRight = w * 0.69f

        val hatPath = Path().apply {
            moveTo(bodyLeft, bodyTop)
            lineTo(bodyLeft, h * 0.32f)
            cubicTo(w * 0.18f, h * 0.30f, w * 0.17f, h * 0.11f, w * 0.34f, h * 0.12f)
            cubicTo(w * 0.39f, h * 0.02f, w * 0.56f, h * 0.02f, w * 0.62f, h * 0.12f)
            cubicTo(w * 0.81f, h * 0.11f, w * 0.86f, h * 0.31f, bodyRight, h * 0.33f)
            lineTo(bodyRight, bodyTop)
            close()
        }
        drawPath(hatPath, color = ServiceYellow, style = yellowStroke)

        drawLine(
            color = ServiceYellow,
            start = Offset(bodyLeft, bodyTop),
            end = Offset(bodyRight, bodyTop),
            strokeWidth = yellowStroke.width,
            cap = StrokeCap.Round
        )

        drawRoundRect(
            color = ServiceYellow,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx()),
            style = yellowStroke
        )

        drawLine(
            color = ServiceYellow,
            start = Offset(bodyLeft + 4.dp.toPx(), h * 0.84f),
            end = Offset(bodyRight - 4.dp.toPx(), h * 0.84f),
            strokeWidth = yellowStroke.width,
            cap = StrokeCap.Round
        )

        drawArc(
            color = ServiceYellow,
            startAngle = 205f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(cx - w * 0.11f, h * 0.61f),
            size = Size(w * 0.22f, h * 0.15f),
            style = yellowStroke
        )
        drawCircle(ServiceYellow, radius = 1.9.dp.toPx(), center = Offset(w * 0.40f, h * 0.58f))
        drawCircle(ServiceYellow, radius = 1.9.dp.toPx(), center = Offset(w * 0.60f, h * 0.58f))
        drawLine(ServiceYellow, Offset(w * 0.41f, h * 0.52f), Offset(w * 0.45f, h * 0.50f), yellowStroke.width, StrokeCap.Round)
        drawLine(ServiceYellow, Offset(w * 0.59f, h * 0.50f), Offset(w * 0.63f, h * 0.52f), yellowStroke.width, StrokeCap.Round)

        val monitorLeft = w * 0.39f
        val monitorTop = h * 0.22f
        val monitorSize = Size(w * 0.25f, h * 0.15f)
        drawRoundRect(
            color = ServiceRed,
            topLeft = Offset(monitorLeft, monitorTop),
            size = monitorSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            style = redStroke
        )
        drawLine(
            color = ServiceRed,
            start = Offset(cx, monitorTop + monitorSize.height),
            end = Offset(cx, monitorTop + monitorSize.height + h * 0.055f),
            strokeWidth = redStroke.width,
            cap = StrokeCap.Round
        )
        drawLine(
            color = ServiceRed,
            start = Offset(cx - w * 0.08f, monitorTop + monitorSize.height + h * 0.055f),
            end = Offset(cx + w * 0.08f, monitorTop + monitorSize.height + h * 0.055f),
            strokeWidth = redStroke.width,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = ServiceBackground,
            radius = w * 0.08f,
            center = Offset(w * 0.66f, h * 0.22f)
        )
        drawCircle(
            color = ServiceRed,
            radius = w * 0.08f,
            center = Offset(w * 0.66f, h * 0.22f),
            style = redStroke
        )
        drawLine(
            color = ServiceRed,
            start = Offset(w * 0.66f, h * 0.17f),
            end = Offset(w * 0.66f, h * 0.22f),
            strokeWidth = redStroke.width,
            cap = StrokeCap.Round
        )
        drawCircle(ServiceRed, radius = 1.8.dp.toPx(), center = Offset(w * 0.66f, h * 0.27f))
    }
}
