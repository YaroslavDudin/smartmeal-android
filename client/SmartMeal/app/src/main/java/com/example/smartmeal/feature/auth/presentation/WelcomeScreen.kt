package com.example.smartmeal.feature.auth.presentation

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.R
import com.example.smartmeal.ui.components.buttons.SmartMealButton
import com.example.smartmeal.ui.components.buttons.SmartMealButtonColor
import com.example.smartmeal.ui.components.buttons.SmartMealButtonVariant
import com.example.smartmeal.ui.theme.SmartMealTheme

@Composable
fun WelcomeScreen(
    onNavigateNext: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 600
    val isCompactWidth = configuration.screenWidthDp < 360

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val horizontalPadding = if (isCompactHeight || isCompactWidth) 16.dp else 24.dp
        val topSpacing = if (isCompactHeight) 16.dp else 40.dp
        val imageMaxHeight = if (isCompactHeight) 200.dp else 280.dp
        val imageWidthFraction = if (isCompactWidth) 0.85f else 0.9f
        val titleSize = if (isCompactHeight) 32.sp else 40.sp
        val titleLineHeight = if (isCompactHeight) 38.sp else 46.sp
        val subtitleSize = if (isCompactHeight) 16.sp else 20.sp
        val subtitleSpacing = if (isCompactHeight) 12.dp else 16.dp
        val bottomSpacing = if (isCompactHeight) 24.dp else 40.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(topSpacing))

            Image(
                painter = painterResource(id = R.drawable.food),
                contentDescription = "Food illustration",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .heightIn(max = imageMaxHeight)
                    .fillMaxWidth(imageWidthFraction)
                    .testTag("food_image")
            )

            Spacer(modifier = Modifier.height(subtitleSpacing))

            Text(
                text = "SmartMeal",
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                lineHeight = titleLineHeight,
                modifier = Modifier.testTag("welcome_title")
            )

            Spacer(modifier = Modifier.height(subtitleSpacing))

            Text(
                text = "Сгенерируйте своё недельное\nменю за пару минут",
                fontSize = subtitleSize,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("welcome_subtitle")
            )

            Spacer(modifier = Modifier.weight(1f, fill = true))

            SmartMealButton(
                text = "Начать",
                onClick = onNavigateNext,
                variant = SmartMealButtonVariant.PRIMARY,
                color = SmartMealButtonColor.GREEN,
                modifier = Modifier.testTag("welcome_start_button")
            )

            Spacer(modifier = Modifier.height(bottomSpacing))
        }
    }
}
@Preview(showBackground = true, showSystemUi = true, name = "Welcome Screen Normal")
@Composable
fun WelcomeScreenPreview() {
    SmartMealTheme {
        WelcomeScreen(onNavigateNext = {})
    }
}
