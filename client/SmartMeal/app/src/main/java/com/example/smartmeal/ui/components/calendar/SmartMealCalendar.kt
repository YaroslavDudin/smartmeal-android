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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.feature.setup.presentation.PeriodType
import com.example.smartmeal.ui.theme.PrimaryGreen
import java.util.Calendar

private val MONTH_NAMES = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)
private val DAY_LABELS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

/**
 * Возвращает первый день недели (понедельник) для заданного дня.
 * Calendar.DAY_OF_WEEK: 1=Sun, 2=Mon, ..., 7=Sat
 */
private fun mondayOfWeek(year: Int, month: Int, day: Int): Int {
    val cal = Calendar.getInstance()
    cal.set(year, month, day)
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    val offset = if (dow == Calendar.SUNDAY) -6 else (2 - dow)
    cal.add(Calendar.DAY_OF_MONTH, offset)
    return if (cal.get(Calendar.MONTH) == month) cal.get(Calendar.DAY_OF_MONTH) else 1
}

private fun sundayOfWeek(year: Int, month: Int, day: Int): Int {
    val cal = Calendar.getInstance()
    cal.set(year, month, day)
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    val offset = if (dow == Calendar.SUNDAY) 0 else (8 - dow)
    cal.add(Calendar.DAY_OF_MONTH, offset)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val result = cal.get(Calendar.DAY_OF_MONTH)
    // If week overflows into next month, cap at last day of current month
    return if (cal.get(Calendar.MONTH) == month) result else daysInMonth
}

/**
 * Интерактивный календарь для выбора даты в процессе настройки меню.
 *
 * Поведение в зависимости от [periodType]:
 * - DAILY  — подсвечивается один выбранный день (зелёный круг)
 * - WEEKLY — при нажатии на любой день подсвечивается вся неделя (Пн–Вс)
 * - CUSTOM — аналогично DAILY, фронтенд может доработать под range-выбор
 */
@Composable
fun SmartMealCalendar(
    year: Int,
    month: Int,
    periodType: PeriodType,
    selectedDay: Int?,
    selectedEndDay: Int? = null,
    onDaySelected: (day: Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cal = Calendar.getInstance()
    cal.set(year, month, 1)

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // firstDayOfWeek: Calendar.DAY_OF_WEEK is 1=Sun..7=Sat, we want Mon=0..Sun=6
    val rawFirstDay = cal.get(Calendar.DAY_OF_WEEK)
    val firstDayOffset = if (rawFirstDay == Calendar.SUNDAY) 6 else rawFirstDay - 2  // Mon=0

    val weekStart = selectedDay?.let { mondayOfWeek(year, month, it) }
    val weekEnd = selectedDay?.let { sundayOfWeek(year, month, it) }

    Column(modifier = modifier) {
        // --- Header: month name + navigation ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Предыдущий месяц")
            }
            Text(
                text = "${MONTH_NAMES[month]} $year",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Следующий месяц")
            }
        }

        // --- Day-of-week labels ---
        Row(modifier = Modifier.fillMaxWidth()) {
            DAY_LABELS.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- Days grid ---
        val totalCells = firstDayOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstDayOffset + 1

                    if (day < 1 || day > daysInMonth) {
                        // Empty cell
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val isCustomRangeActive = periodType == PeriodType.CUSTOM &&
                                selectedDay != null && selectedEndDay != null
                        CalendarDayCell(
                            day = day,
                            periodType = periodType,
                            isSelected = selectedDay == day || selectedEndDay == day,
                            isInWeek = periodType == PeriodType.WEEKLY &&
                                    weekStart != null && weekEnd != null &&
                                    day in weekStart..weekEnd,
                            isWeekStart = periodType == PeriodType.WEEKLY && day == weekStart,
                            isWeekEnd = periodType == PeriodType.WEEKLY && day == weekEnd,
                            isInRange = isCustomRangeActive && day in (selectedDay!! + 1) until selectedEndDay!!,
                            isRangeStart = isCustomRangeActive && day == selectedDay,
                            isRangeEnd = isCustomRangeActive && day == selectedEndDay,
                            modifier = Modifier.weight(1f),
                            onClick = { onDaySelected(day) },
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
    periodType: PeriodType,
    isSelected: Boolean,
    isInWeek: Boolean,
    isWeekStart: Boolean,
    isWeekEnd: Boolean,
    isInRange: Boolean = false,
    isRangeStart: Boolean = false,
    isRangeEnd: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bgColor = when {
        isInWeek || isInRange -> PrimaryGreen.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val rowShape = when {
        isWeekStart || isRangeStart -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
        isWeekEnd || isRangeEnd -> RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
        else -> RoundedCornerShape(0.dp)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(bgColor, if (isWeekStart || isWeekEnd) rowShape else RoundedCornerShape(0.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            // Green circle for the selected day
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        } else {
            Text(
                text = day.toString(),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = if (isInWeek) PrimaryGreen else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
