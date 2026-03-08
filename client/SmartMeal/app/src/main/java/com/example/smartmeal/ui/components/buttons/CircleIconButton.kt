package com.example.smartmeal.ui.components.buttons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

enum class CircleIconType {
    FAVORITE,   // ❤️
    REPLACE,    // 🔄
    BACK        // ⬅️
}

@Composable
fun CircleIconButton(
    iconType: CircleIconType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false, // Для избранного (❤️ vs 🖤)
    backgroundColor: Color = Color.White,
    contentColor: Color = PrimaryGreen
) {
    val icon = when (iconType) {
        CircleIconType.FAVORITE -> if (isSelected) "❤️" else "🖤"
        CircleIconType.REPLACE -> "🔄"
        CircleIconType.BACK -> "⬅️"
    }

    Button(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CircleIconButtonPreview() {
    SmartMealTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CircleIconButton(
                iconType = CircleIconType.FAVORITE,
                onClick = {},
                isSelected = true
            )
        }
    }
}

//CircleIconButton(
//    iconType = CircleIconType.BACK,
//    onClick = { navController.popBackStack() }
//)