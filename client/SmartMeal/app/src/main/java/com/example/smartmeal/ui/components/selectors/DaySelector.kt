package com.example.smartmeal.ui.components.selectors

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

@Composable
fun DaySelector(
    selectedDay: String,
    onDaySelected: (String) -> Unit
) {
    val days = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")

    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            val selected = day == selectedDay

            Button(
                onClick = { onDaySelected(day) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) Color(0xFF2E7D32) else Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
            ) {
                Text(
                    text = day,
                    color = if (selected) Color.White else Color.Black,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DaySelectorPreview() {
    DaySelector(
        selectedDay = "Ср",
        onDaySelected = {}
    )
}