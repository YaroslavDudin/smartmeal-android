package com.example.smartmeal.feature.service_unavailable.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.theme.SmartMealBackground
import com.example.smartmeal.ui.theme.SmartMealTextColor
import com.example.smartmeal.ui.theme.SmartMealTextSecondary
import com.example.smartmeal.ui.theme.SmartMealTomato
import kotlinx.coroutines.launch

private val ServiceBackground = SmartMealBackground
private val ServiceTitle = SmartMealTextColor
private val ServiceBody = SmartMealTextSecondary
private val ServiceLine = Color(0xFF9E9A98)
private val ServiceSoftLine = Color(0xFFE7DDD7)
private val ServiceRed = Color(0xFFFF5738)
private val ServiceAction = SmartMealTomato

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
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
    ) {
        ServiceBackgroundDecor(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomCenter)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ServiceUnavailableIllustration(
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            SmartMealText(
                text = "Сервисы временно\nнедоступны",
                color = ServiceTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 27.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            SmartMealText(
                text = "Приносим извинения, наши сервисы временно недоступны. Мы уже исправляем проблему. Попробуйте обновить или повторите попытку позже.",
                color = ServiceBody,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
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
                },
                modifier = Modifier
                    .width(200.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ServiceAction,
                    contentColor = Color.White,
                    disabledContainerColor = ServiceAction.copy(alpha = 0.45f),
                    disabledContentColor = Color.White
                )
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
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
                SmartMealText(
                    text = "Обновить",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ServiceBackgroundDecor(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bottom = h * 0.88f

        drawRoundRect(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(w * 0.10f, bottom - h * 0.09f),
            size = Size(w * 0.22f, h * 0.10f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx())
        )
        drawLine(ServiceSoftLine.copy(alpha = 0.72f), Offset(w * 0.58f, bottom), Offset(w * 0.86f, bottom), 3.dp.toPx(), StrokeCap.Round)
        drawRoundRect(
            color = ServiceSoftLine.copy(alpha = 0.38f),
            topLeft = Offset(w * 0.62f, bottom - h * 0.09f),
            size = Size(w * 0.18f, h * 0.05f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )

        val plantX = w * 0.18f
        val plantY = bottom - h * 0.05f
        drawLine(ServiceSoftLine, Offset(plantX, plantY), Offset(plantX, plantY - h * 0.10f), 2.dp.toPx(), StrokeCap.Round)
        drawOval(Color(0xFFDDE9D9), Offset(plantX - w * 0.065f, plantY - h * 0.10f), Size(w * 0.08f, h * 0.04f))
        drawOval(Color(0xFFDDE9D9), Offset(plantX + w * 0.005f, plantY - h * 0.12f), Size(w * 0.08f, h * 0.04f))
        drawOval(Color(0xFFDDE9D9), Offset(plantX - w * 0.035f, plantY - h * 0.17f), Size(w * 0.08f, h * 0.04f))
    }
}

@Composable
private fun ServiceUnavailableIllustration(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "senior_illustration")
    
    // Float and Sway animations (Senior level easing)
    val floatY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    val swayRotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    // Exclamation Mark Animation (Defined here, used inside Canvas)
    val excScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "exc_pulse"
    )

    Canvas(modifier = modifier.graphicsLayer { 
        translationY = floatY.dp.toPx()
        rotationZ = swayRotation - 4f // Fixed tilt + dynamic sway
    }) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        
        // --- 1. PRO SHADOW (Soft and deep) ---
        val shadowPath = Path().apply {
            moveTo(w * 0.28f, h * 0.38f)
            cubicTo(w * 0.15f, h * 0.38f, w * 0.12f, h * 0.20f, w * 0.30f, h * 0.20f)
            cubicTo(w * 0.32f, h * 0.05f, w * 0.55f, h * 0.04f, w * 0.60f, h * 0.18f)
            cubicTo(w * 0.78f, h * 0.17f, w * 0.86f, h * 0.35f, w * 0.75f, h * 0.40f)
            lineTo(w * 0.30f, h * 0.40f)
            close()
        }
        
        drawPath(
            path = shadowPath,
            color = Color(0xFFDED6D1).copy(alpha = 0.2f),
        )
        // Secondary blurrier layer for depth
        drawPath(
            path = shadowPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFDED6D1).copy(alpha = 0.35f), Color.Transparent),
                center = Offset(cx, h * 0.25f),
                radius = w * 0.4f
            )
        )

        // --- 2. THE PHONE (Senior minimalism) ---
        val phoneLeft = w * 0.32f
        val phoneTop = h * 0.42f
        val phoneSize = Size(w * 0.36f, h * 0.45f)
        
        // Phone Glass/Screen Effect
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.9f),
                1f to Color(0xFFF1F5F9).copy(alpha = 0.8f)
            ),
            topLeft = Offset(phoneLeft, phoneTop),
            size = phoneSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx(), 18.dp.toPx())
        )
        
        // Phone Outline (Elegant & thin)
        drawRoundRect(
            color = ServiceLine.copy(alpha = 0.7f),
            topLeft = Offset(phoneLeft, phoneTop),
            size = phoneSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx(), 18.dp.toPx()),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Subtle Face
        val faceY = h * 0.62f
        drawArc(
            color = ServiceLine,
            startAngle = 210f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(cx - w * 0.07f, faceY),
            size = Size(w * 0.14f, h * 0.08f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(ServiceLine, radius = 2.5.dp.toPx(), center = Offset(w * 0.44f, faceY - h * 0.02f))
        drawCircle(ServiceLine, radius = 2.5.dp.toPx(), center = Offset(w * 0.56f, faceY - h * 0.02f))

        // --- 3. THE CLOUD (Senior Glassmorphism & Depth) ---
        val mainCloudPath = Path().apply {
            moveTo(w * 0.22f, h * 0.35f)
            // Left puff
            cubicTo(w * 0.08f, h * 0.35f, w * 0.08f, h * 0.15f, w * 0.25f, h * 0.15f)
            // Top puff
            cubicTo(w * 0.28f, h * -0.02f, w * 0.55f, h * -0.02f, w * 0.62f, h * 0.15f)
            // Right puff
            cubicTo(w * 0.82f, h * 0.15f, w * 0.85f, h * 0.35f, w * 0.72f, h * 0.35f)
            lineTo(w * 0.22f, h * 0.35f)
            close()
        }

        // Layer 1: Glass Background
        drawPath(
            path = mainCloudPath,
            brush = Brush.linearGradient(
                colors = listOf(Color.White, Color.White.copy(alpha = 0.85f), Color(0xFFF8FAFC)),
                start = Offset(w * 0.2f, h * 0.1f),
                end = Offset(w * 0.7f, h * 0.35f)
            )
        )
        
        // Layer 2: Internal Shadow for Puffy look
        drawPath(
            path = mainCloudPath,
            brush = Brush.verticalGradient(
                0.2f to Color.Transparent,
                1.0f to Color(0xFFE2E8F0).copy(alpha = 0.4f)
            )
        )

        // Layer 3: Sharp Outline
        drawPath(
            path = mainCloudPath,
            color = ServiceLine.copy(alpha = 0.8f),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Layer 4: Highlight Border (Glass edge)
        val highlightPath = Path().apply {
            moveTo(w * 0.18f, h * 0.25f)
            cubicTo(w * 0.20f, h * 0.12f, w * 0.30f, h * 0.05f, w * 0.45f, h * 0.05f)
        }
        drawPath(
            path = highlightPath,
            color = Color.White,
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )

        // --- 4. ERROR INDICATOR (Polished Alert) ---
        val alertCenter = Offset(w * 0.78f, h * 0.22f)
        
        // Soft Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ServiceRed.copy(alpha = 0.25f), Color.Transparent),
                center = alertCenter,
                radius = w * 0.15f
            ),
            radius = w * 0.15f,
            center = alertCenter
        )
        
        // Red Badge
        drawCircle(ServiceRed, radius = w * 0.08f, center = alertCenter)
        // Subtle white inner ring
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = w * 0.065f,
            center = alertCenter,
            style = Stroke(width = 1.dp.toPx())
        )
        
        // Exclamation Mark (Designer style: Clean Point & Dot)
        drawCircle(
            color = Color.White,
            radius = 3.5.dp.toPx() * excScale,
            center = Offset(alertCenter.x, alertCenter.y - (h * 0.015f * excScale))
        )
        drawCircle(
            color = Color.White, 
            radius = 2.8.dp.toPx() * excScale, 
            center = Offset(alertCenter.x, alertCenter.y + (h * 0.035f * excScale))
        )
        
        // --- 5. FLOATING PARTICLES (Minimalist) ---
        val particleColor = Color(0xFFDED6D1).copy(alpha = 0.6f)
        drawCircle(particleColor, radius = 3.dp.toPx(), center = Offset(w * 0.1f, h * 0.5f))
        drawCircle(particleColor, radius = 2.dp.toPx(), center = Offset(w * 0.9f, h * 0.2f))
        drawCircle(particleColor, radius = 4.dp.toPx(), center = Offset(w * 0.85f, h * 0.8f))
    }
}





