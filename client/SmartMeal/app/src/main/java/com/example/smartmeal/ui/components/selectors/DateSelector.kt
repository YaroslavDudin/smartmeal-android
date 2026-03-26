package com.example.smartmeal.ui.components.selectors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.theme.LightGreenBg
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.TextBlack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DateSelectorItem(
    val id: String,
    val date: Date,
    val weekdayLabel: String,
    val dayLabel: String,
)

@Composable
fun DateSelector(
    items: List<DateSelectorItem>,
    selectedStartId: String?,
    selectedEndId: String? = null,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    val selectedRange = remember(items, selectedStartId, selectedEndId) {
        buildSelectionRange(items, selectedStartId, selectedEndId)
    }

    
    LaunchedEffect(selectedStartId, items) {
        if (selectedStartId != null && items.isNotEmpty()) {
            val index = items.indexOfFirst { it.id == selectedStartId }
            if (index != -1) {
                listState.animateScrollToItem(maxOf(0, index - 2))
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            val isStart = item.id == selectedStartId
            val isEnd = item.id == selectedEndId
            val isSelected = isStart || isEnd || (selectedRange?.contains(index) == true)
            val isRangeInterior = selectedRange?.contains(index) == true && !isStart && !isEnd

            DateChip(
                item = item,
                isSelected = isSelected,
                isRangeInterior = isRangeInterior,
                onClick = { onItemClick(item.id) },
                modifier = Modifier.testTag("date_chip_$index")
            )
        }
    }
}

@Composable
private fun DateChip(
    item: DateSelectorItem,
    isSelected: Boolean,
    isRangeInterior: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipShape = RoundedCornerShape(18.dp)
    val backgroundColor = when {
        isRangeInterior -> PrimaryGreen.copy(alpha = 0.2f)
        isSelected -> PrimaryGreen
        else -> LightGreenBg
    }
    val contentColor = when {
        isRangeInterior -> PrimaryGreen
        isSelected -> Color.White
        else -> TextBlack
    }

    Column(
        modifier = modifier
            .clip(chipShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SmartMealText(
            text = item.weekdayLabel,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Medium,
        )
        SmartMealText(
            text = item.dayLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

internal fun buildDateSelectorItems(dates: List<Date>): List<DateSelectorItem> {
    val weekdayFormatter = SimpleDateFormat("EE", Locale("ru"))
    val dayFormatter = SimpleDateFormat("d", Locale("ru"))

    return dates.map { date ->
        DateSelectorItem(
            id = buildDateSelectorId(date),
            date = date,
            weekdayLabel = weekdayFormatter.format(date).replaceFirstChar { it.titlecase(Locale("ru")) },
            dayLabel = dayFormatter.format(date),
        )
    }
}

internal fun buildDateSelectorId(date: Date): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
}

internal fun formatSelectedDateLabel(date: Date): String {
    val formatter = SimpleDateFormat("EEEE - d MMMM yyyy", Locale("ru"))
    return formatter.format(date).replaceFirstChar { it.titlecase(Locale("ru")) }
}

private fun buildSelectionRange(
    items: List<DateSelectorItem>,
    selectedStartId: String?,
    selectedEndId: String?
): IntRange? {
    if (selectedStartId == null || selectedEndId == null) return null

    val startIndex = items.indexOfFirst { it.id == selectedStartId }
    val endIndex = items.indexOfFirst { it.id == selectedEndId }
    if (startIndex == -1 || endIndex == -1) return null

    return if (startIndex <= endIndex) startIndex..endIndex else endIndex..startIndex
}