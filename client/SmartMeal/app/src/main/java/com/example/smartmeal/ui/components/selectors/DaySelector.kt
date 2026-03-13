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
import com.example.smartmeal.ui.components.chips_filters.FilterChip

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
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        days.forEach { day ->

            FilterChip(
                label = day,
                isSelected = day == selectedDay,
                onClick = { onDaySelected(day) }
            )

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