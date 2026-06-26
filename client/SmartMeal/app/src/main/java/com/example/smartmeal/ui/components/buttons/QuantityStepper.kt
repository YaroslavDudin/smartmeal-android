package com.example.smartmeal.ui.components.buttons

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.theme.LightGreenBg
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.SmartMealTheme
import com.example.smartmeal.ui.theme.TextBlack
import com.example.smartmeal.ui.components.SmartMealText

@Composable
fun QuantityStepper(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
    minQuantity: Int = 1,
    maxQuantity: Int = 20
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(LightGreenBg.copy(alpha = 0.5f))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Кнопка уменьшения
        StepperButton(
            icon = Icons.Default.Remove,
            enabled = quantity > minQuantity,
            onClick = onDecrease
        )

        // Значение с анимацией
        AnimatedContent(
            targetState = quantity,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInVertically { height -> height } + fadeIn()).togetherWith(
                        slideOutVertically { height -> -height } + fadeOut())
                } else {
                    (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                        slideOutVertically { height -> height } + fadeOut())
                }.using(
                    SizeTransform(clip = false)
                )
            }, label = "QuantityAnimation"
        ) { targetQuantity ->
            SmartMealText(
                text = targetQuantity.toString(),
                modifier = Modifier.widthIn(min = 20.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryGreen,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Кнопка увеличения
        StepperButton(
            icon = Icons.Default.Add,
            enabled = quantity < maxQuantity,
            onClick = onIncrease
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (enabled) PrimaryGreen else Color.LightGray.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuantityStepperPreview() {
    SmartMealTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            QuantityStepper(
                quantity = 3,
                onIncrease = {},
                onDecrease = {}
            )
        }
    }
}
