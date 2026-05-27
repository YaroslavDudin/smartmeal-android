package com.example.smartmeal.ui.components.buttons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.theme.SmartMealTomato
import com.example.smartmeal.ui.theme.SmartMealTheme
import com.example.smartmeal.utils.softBottomShadow

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Теперь цвет по умолчанию берется из нашей темы
    containerColor: Color = SmartMealTomato
) {
    val shape = RoundedCornerShape(18.dp)
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .softBottomShadow(shape = shape, color = containerColor.copy(alpha = 0.22f), blurRadius = 10.dp, offsetY = 5.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = shape
    ) {
        SmartMealText(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Preview(showBackground = true, name = "Long Text Button")
@Composable
fun PrimaryButtonLongTextPreview() {
    SmartMealTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PrimaryButton(
                text = "Продолжить с длинным текстом",
                onClick = {}
            )
        }
    }
}
