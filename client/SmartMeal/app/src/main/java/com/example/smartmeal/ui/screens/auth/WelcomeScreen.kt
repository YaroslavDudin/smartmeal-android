package com.example.smartmeal.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.PrimaryButton
import com.example.smartmeal.ui.theme.SmartMealTheme
import androidx.compose.ui.res.painterResource
import com.example.smartmeal.R
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

            Image(
                painter = painterResource(id = R.drawable.food),
                contentDescription = "Food illustration",
                modifier = Modifier.size(350.dp).testTag("food_image")
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "SmartMeal",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                lineHeight = 46.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Сгенерируйте своё недельное\nменю за пару минут",
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }

        PrimaryButton(
            text = "Начать",
            onClick = onNavigateNext,
            containerColor = Color(0xFF4CAF50)
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