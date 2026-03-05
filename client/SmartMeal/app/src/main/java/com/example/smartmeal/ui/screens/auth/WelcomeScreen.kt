package com.example.smartmeal.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.PrimaryButton
import com.example.smartmeal.ui.theme.SmartMealTheme

@Composable
fun WelcomeScreen(
    onNavigateNext: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Тут иллюстрация\nиз макета",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Избавим от вопроса\n«Что приготовить?»",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Персонализированное меню и умный список продуктов в пару кликов.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        PrimaryButton(
            text = "Начать",
            onClick = onNavigateNext
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Welcome Screen")
@Composable
fun WelcomeScreenPreview() {
    SmartMealTheme {
        WelcomeScreen(onNavigateNext = {})
    }
}