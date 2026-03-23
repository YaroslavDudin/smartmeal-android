package com.example.smartmeal.ui.components.chips_filters

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.theme.LightGreenBg
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.SmartMealTheme
import com.example.smartmeal.ui.theme.TextBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = {
            SmartMealText(
                text = label,
                color = if (isSelected) Color.White else TextBlack
            )
        },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isSelected) PrimaryGreen else LightGreenBg,
            labelColor = if (isSelected) Color.White else TextBlack
        ),
        shape = RoundedCornerShape(50.dp),
        border = null // Убираем границу для чистого дизайна
    )
}

@Preview(showBackground = true)
@Composable
fun FilterChipPreview() {
    SmartMealTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FilterChip(
                label = "Кето",
                isSelected = true,
                onClick = {}
            )
        }
    }
}
