package com.example.smartmeal.ui.components.buttons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.smartmeal.utils.softBottomShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.theme.AccentOrange
import com.example.smartmeal.ui.theme.MainGreen
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.SmartMealTheme
import com.example.smartmeal.ui.theme.TextBlack
import com.example.smartmeal.ui.components.SmartMealText

enum class SmartMealButtonVariant {
    PRIMARY,    // Залитая, цветной фон
    SECONDARY,  // Прозрачный фон, цветной текст
    OUTLINED    // С обводкой
}

enum class SmartMealButtonColor {
    GREEN,      // Используем MainGreen (47B91C)
    ORANGE      // Используем AccentOrange
}

@Composable
fun SmartMealButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: SmartMealButtonVariant = SmartMealButtonVariant.PRIMARY,
    color: SmartMealButtonColor = SmartMealButtonColor.GREEN,
    enabled: Boolean = true,
    fullWidth: Boolean = true
) {
    val containerColor = when (color) {
        SmartMealButtonColor.GREEN -> PrimaryGreen
        SmartMealButtonColor.ORANGE -> AccentOrange
    }

    val contentColor = Color.White
    val disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
    val disabledContentColor = Color.Gray

    val buttonModifier = if (fullWidth) {
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .softBottomShadow(shape = RoundedCornerShape(16.dp))
    } else {
        modifier
            .height(56.dp)
            .softBottomShadow(shape = RoundedCornerShape(16.dp))
    }

    when (variant) {
        SmartMealButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor,
                    disabledContainerColor = disabledContainerColor,
                    disabledContentColor = disabledContentColor
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                SmartMealText(
                    text = text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        SmartMealButtonVariant.SECONDARY -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = containerColor,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = disabledContentColor
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                SmartMealText(
                    text = text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        SmartMealButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = containerColor,
                    disabledContentColor = disabledContentColor
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                SmartMealText(
                    text = text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SmartMealButtonPreview() {
    SmartMealTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SmartMealButton(
                text = "Сгенерировать",
                onClick = {},
                variant = SmartMealButtonVariant.PRIMARY,
                color = SmartMealButtonColor.GREEN
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SmartMealButtonVariantsPreview() {
    SmartMealTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            // Просто для превью разных состояний
        }
    }
}

//SmartMealButton(
//    text = "Сгенерировать",
//    onClick = { /* действие */ },
//    variant = SmartMealButtonVariant.PRIMARY,
//    color = SmartMealButtonColor.GREEN
//)
//
//SmartMealButton(
//    text = "В корзину",
//    onClick = { /* действие */ },
//    variant = SmartMealButtonVariant.OUTLINED,
//    color = SmartMealButtonColor.ORANGE
//)