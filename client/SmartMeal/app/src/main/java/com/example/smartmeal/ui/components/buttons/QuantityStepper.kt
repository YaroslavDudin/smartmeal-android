package com.example.smartmeal.ui.components.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.SmartMealTheme
import com.example.smartmeal.ui.theme.TextBlack

@Composable
fun QuantityStepper(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
    minQuantity: Int = 1,
    maxQuantity: Int = 10
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Минус
        FilledIconButton(
            onClick = onDecrease,
            enabled = quantity > minQuantity,
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = PrimaryGreen,
                contentColor = Color.White,
                disabledContainerColor = Color.LightGray
            )
        ) {
            Text(text = "-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Значение
        Text(
            text = quantity.toString(),
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextBlack
        )

        // Плюс
        FilledIconButton(
            onClick = onIncrease,
            enabled = quantity < maxQuantity,
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = PrimaryGreen,
                contentColor = Color.White,
                disabledContainerColor = Color.LightGray
            )
        ) {
            Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
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
//QuantityStepper(
//    quantity = portions,
//    onIncrease = { portions++ },
//    onDecrease = { portions-- }
//)