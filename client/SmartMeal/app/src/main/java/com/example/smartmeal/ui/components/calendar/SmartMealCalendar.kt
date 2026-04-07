package com.example.smartmeal.ui.components.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.example.smartmeal.feature.setup.presentation.PeriodType
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.TextBlack
import com.example.smartmeal.ui.components.SmartMealText
import java.util.Calendar

private val MONTH_NAMES = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)
private val DAY_LABELS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

/**
 * Интерактивный календарь для выбора даты в процессе настройки меню.
 */
@Composable
fun SmartMealCalendar(
    year: Int,
    month: Int,
    periodType: PeriodType?,
    selectedStartDateMillis: Long?,
    selectedEndDateMillis: Long? = null,
    onDaySelected: (year: Int, month: Int, day: Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    canNavigatePrevious: Boolean = true,
    canNavigateNext: Boolean = true,
    showNavigation: Boolean = true,
    showYear: Boolean = true,
    showAdjacentMonths: Boolean = false,
    compact: Boolean = false,
    isDateSelectable: (year: Int, month: Int, day: Int) -> Boolean = { _, _, _ -> true },
) {
    val cal = Calendar.getInstance()
    cal.set(year, month, 1)

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val prevCal = Calendar.getInstance()
    prevCal.set(year, month, 1)
    prevCal.add(Calendar.MONTH, -1)
    val prevMonthDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val nextCal = Calendar.getInstance()
    nextCal.set(year, month, 1)
    nextCal.add(Calendar.MONTH, 1)

    // firstDayOfWeek: Calendar.DAY_OF_WEEK is 1=Sun..7=Sat, we want Mon=0..Sun=6
    val rawFirstDay = cal.get(Calendar.DAY_OF_WEEK)
    val firstDayOffset = if (rawFirstDay == Calendar.SUNDAY) 6 else rawFirstDay - 2  // Mon=0

    // Для WEEKLY: выбранный день — это начало, конец — через 6 дней (всего 7 дней)
    val weekStartMillis = selectedStartDateMillis
    val weekEndMillis = selectedStartDateMillis?.let { start ->
        Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.DAY_OF_YEAR, 6)
        }.timeInMillis
    }

    val headerTextStyle = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge
    val dayLabelStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    val cellTextSize = if (compact) 12.sp else 14.sp
    val circleSize = if (compact) 30.dp else 36.dp
    val headerSpacing = if (compact) 4.dp else 6.dp
    val labelSpacing = if (compact) 2.dp else 4.dp
    val iconSize = if (compact) 32.dp else 40.dp

    val calendarAlpha = if (enabled) 1f else 0.38f

    Column(modifier = modifier) {
        // --- Header: month name + optional navigation ---
        if (showNavigation) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.size(iconSize),
                    enabled = enabled && canNavigatePrevious,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Предыдущий месяц",
                        tint = if (enabled && canNavigatePrevious) TextBlack else Color.LightGray,
                    )
                }
                SmartMealText(
                    text = if (showYear) "${MONTH_NAMES[month]} $year" else MONTH_NAMES[month],
                    style = headerTextStyle,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack.copy(alpha = calendarAlpha),
                )
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(iconSize),
                    enabled = enabled && canNavigateNext,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Следующий месяц",
                        tint = if (enabled && canNavigateNext) TextBlack else Color.LightGray,
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                SmartMealText(
                    text = if (showYear) "${MONTH_NAMES[month]} $year" else MONTH_NAMES[month],
                    style = headerTextStyle,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack.copy(alpha = calendarAlpha),
                )
            }
        }

        Spacer(modifier = Modifier.height(headerSpacing))

        // --- Day-of-week labels ---
        Row(modifier = Modifier.fillMaxWidth()) {
            DAY_LABELS.forEach { label ->
                SmartMealText(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = dayLabelStyle,
                    color = TextBlack.copy(alpha = calendarAlpha),
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(modifier = Modifier.height(labelSpacing))

        // --- Days grid ---
        val totalCells = firstDayOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - firstDayOffset + 1

                    val isCurrentMonth = dayNumber in 1..daysInMonth
                    val cellYear: Int
                    val cellMonth: Int
                    val displayDay: Int

                    when {
                        isCurrentMonth -> {
                            cellYear = year
                            cellMonth = month
                            displayDay = dayNumber
                        }
                        dayNumber < 1 -> {
                            cellYear = prevCal.get(Calendar.YEAR)
                            cellMonth = prevCal.get(Calendar.MONTH)
                            displayDay = prevMonthDays + dayNumber
                        }
                        else -> {
                            cellYear = nextCal.get(Calendar.YEAR)
                            cellMonth = nextCal.get(Calendar.MONTH)
                            displayDay = dayNumber - daysInMonth
                        }
                    }

                    if (!isCurrentMonth && !showAdjacentMonths) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val cellDateMillis = createDateMillis(cellYear, cellMonth, displayDay)
                        val rangeStart = selectedStartDateMillis
                        val rangeEnd = selectedEndDateMillis
                        val isCustomRangeActive = periodType == PeriodType.CUSTOM &&
                            rangeStart != null && rangeEnd != null

                        val isSelectable = enabled && isCurrentMonth && isDateSelectable(cellYear, cellMonth, displayDay)

                        CalendarDayCell(
                            day = displayDay,
                            periodType = periodType,
                            isCurrentMonth = isCurrentMonth,
                            isSelected = cellDateMillis == selectedStartDateMillis || cellDateMillis == selectedEndDateMillis,
                            isInWeek = periodType == PeriodType.WEEKLY &&
                                weekStartMillis != null && weekEndMillis != null &&
                                cellDateMillis > weekStartMillis && cellDateMillis < weekEndMillis,
                            isWeekStart = periodType == PeriodType.WEEKLY && cellDateMillis == weekStartMillis,
                            isWeekEnd = periodType == PeriodType.WEEKLY && cellDateMillis == weekEndMillis,
                            isInRange = isCustomRangeActive &&
                                rangeStart != null && rangeEnd != null &&
                                cellDateMillis > rangeStart && cellDateMillis < rangeEnd,
                            isRangeStart = periodType == PeriodType.CUSTOM && cellDateMillis == selectedStartDateMillis,
                            isRangeEnd = periodType == PeriodType.CUSTOM && cellDateMillis == selectedEndDateMillis,
                            modifier = Modifier.weight(1f),
                            onClick = { if (isSelectable) onDaySelected(cellYear, cellMonth, displayDay) },
                            circleSize = circleSize,
                            textSize = cellTextSize,
                            isSelectable = isSelectable,
                            enabled = enabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    periodType: PeriodType?,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    isInWeek: Boolean,
    isWeekStart: Boolean,
    isWeekEnd: Boolean,
    isInRange: Boolean = false,
    isRangeStart: Boolean = false,
    isRangeEnd: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    circleSize: Dp = 36.dp,
    textSize: TextUnit = 14.sp,
    isSelectable: Boolean = true,
    enabled: Boolean = true
) {
    val isWeeklySelected = periodType == PeriodType.WEEKLY && (isInWeek || isWeekStart || isWeekEnd)
    val isInAnySelection = (periodType == PeriodType.WEEKLY && (isInWeek || isWeekStart || isWeekEnd)) ||
                          (periodType == PeriodType.CUSTOM && (isInRange || isRangeStart || isRangeEnd))

    val isCustomEndpoint = periodType == PeriodType.CUSTOM && (isRangeStart || isRangeEnd)
    val isCustomInterior = periodType == PeriodType.CUSTOM && isInRange
    val isWeeklyEndpoint = periodType == PeriodType.WEEKLY && (isWeekStart || isWeekEnd)
    val isWeeklyInterior = periodType == PeriodType.WEEKLY && isInWeek
    val isStandaloneSelected = isSelected && (periodType == PeriodType.DAILY || periodType == null)

    val selectionAlpha = if (enabled) 1f else 0.38f
    val primaryColorWithAlpha = PrimaryGreen.copy(alpha = selectionAlpha)

    val bgColor = when {
        isWeeklyInterior -> primaryColorWithAlpha.copy(alpha = 0.18f * selectionAlpha)
        isWeeklyEndpoint -> primaryColorWithAlpha.copy(alpha = 0.24f * selectionAlpha)
        isCustomInterior -> primaryColorWithAlpha.copy(alpha = 0.18f * selectionAlpha)
        isCustomEndpoint -> primaryColorWithAlpha.copy(alpha = 0.24f * selectionAlpha)
        isStandaloneSelected -> primaryColorWithAlpha.copy(alpha = 0.22f * selectionAlpha)
        isInAnySelection -> primaryColorWithAlpha.copy(alpha = 0.15f * selectionAlpha)
        else -> Color.Transparent
    }

    val rowShape = when {
        isWeeklySelected && isWeekStart -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
        isWeeklySelected && isWeekEnd -> RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
        isWeeklySelected -> RoundedCornerShape(0.dp)
        isRangeStart -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
        isRangeEnd -> RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
        else -> RoundedCornerShape(0.dp)
    }

    val textColor = when {
        !isCurrentMonth -> Color.LightGray
        !isSelectable && enabled -> Color.LightGray
        !enabled -> Color.LightGray.copy(alpha = 0.38f)
        isWeeklyEndpoint -> Color.White
        isWeeklyInterior -> PrimaryGreen
        isCustomEndpoint -> Color.White
        isCustomInterior -> PrimaryGreen
        isStandaloneSelected -> Color.White
        else -> PrimaryGreen
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(bgColor, rowShape)
            .then(if (isSelectable) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val showCircle = isCustomEndpoint || isWeeklyEndpoint || isStandaloneSelected

        if (showCircle) {
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(primaryColorWithAlpha),
                contentAlignment = Alignment.Center,
            ) {
                SmartMealText(
                    text = day.toString(),
                    color = Color.White.copy(alpha = selectionAlpha),
                    fontWeight = FontWeight.Bold,
                    fontSize = textSize,
                )
            }
        } else {
            SmartMealText(
                text = day.toString(),
                textAlign = TextAlign.Center,
                fontSize = textSize,
                color = textColor,
                fontWeight = if (isWeeklyInterior || isCustomInterior) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

private fun createDateMillis(year: Int, month: Int, day: Int): Long {
    return Calendar.getInstance().apply {
        set(year, month, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
