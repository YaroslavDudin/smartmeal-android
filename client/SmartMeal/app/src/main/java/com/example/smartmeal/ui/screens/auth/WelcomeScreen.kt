package com.example.smartmeal.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.R
import com.example.smartmeal.ui.components.buttons.PrimaryButton
import com.example.smartmeal.ui.theme.SmartMealTheme

@Composable
fun WelcomeScreen(
    onNavigateNext: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.weight(0.5f))

        Image(
            painter = painterResource(id = R.drawable.food),
            contentDescription = "Food illustration",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .weight(3f)
                .testTag("food_image")
        )

        Spacer(modifier = Modifier.weight(0.5f))

        Text(
            text = "SmartMeal",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 46.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Сгенерируйте своё недельное меню за пару минут",
                    fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "Начать",
            onClick = onNavigateNext,
            containerColor = Color(0xFF4CAF50),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Welcome Screen Normal")
@Composable
fun WelcomeScreenPreview() {
    SmartMealTheme {
        WelcomeScreen(onNavigateNext = {})
    }
}
