package com.example.smartmeal.feature.sandbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.utils.softBottomShadow
import com.example.smartmeal.ui.theme.SmartMealTheme

/**
 * Экран песочницы для тестирования UI-компонентов.
 */
@Composable
fun TestScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(16.dp)
            ) {
                SmartMealText(text = "← Назад к регистрации", fontSize = 16.sp)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            SimpleDropShadowUsage()
        }
    }
}

@Composable
fun SimpleDropShadowUsage() {
    Box(
        modifier = Modifier
            .size(200.dp)
            .softBottomShadow(shape = RoundedCornerShape(20.dp))
            .background(
                color = Color.White,
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        SmartMealText(
            text = "Drop Shadow",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Preview(showBackground = true, name = "Sandbox Preview")
@Composable
fun TestScreenPreview() {
    SmartMealTheme {
        TestScreen(onBack = {})
    }
}
