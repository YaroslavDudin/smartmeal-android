package com.example.smartmeal.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.TextBlack
import java.util.Calendar
import java.util.Date

private val MY_PLAN_MONTH_NAMES = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)
private val MY_PLAN_DAY_LABELS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@Composable
fun MyPlanButton(
    customPlan: CustomPlan?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryGreen,
            contentColor = Color.White,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        contentPadding = PaddingValues(vertical = 0.dp)
    ) {
        Text(
            text = if (customPlan != null) "Мой план ✓" else "Мой план",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun MyPlanCalendarDialog(
    plan: CustomPlan,
    selectedDate: Date?,
    isRangeSelection: Boolean = false,
    onDateClick: (Date) -> Unit,
    onRangeSelected: ((Date, Date) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val startCal = Calendar.getInstance().apply { time = plan.startDate }
    val endCal = Calendar.getInstance().apply { time = plan.endDate }
    
    // Текущая дата для фильтрации прошедших дней
    val today = remember { 
        Calendar.getInstance().apply { 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time 
    }
    
    // Состояние для выбора диапазона (всегда сбрасывается при открытии)
    var rangeStart by remember { mutableStateOf<Date?>(null) }
    
    val normalizedSelectedDateMillis = if (isRangeSelection) null else selectedDate?.let(::normalizeDateMillis)
    
    val initialCalendar = Calendar.getInstance().apply {
        time = when {
            normalizedSelectedDateMillis != null &&
                normalizedSelectedDateMillis in normalizeDateMillis(plan.startDate)..normalizeDateMillis(plan.endDate) -> {
                Date(normalizedSelectedDateMillis)
            }
            else -> if (plan.startDate.before(today) && plan.endDate.after(today)) today else plan.startDate
        }
    }

    var displayYear by remember { mutableIntStateOf(initialCalendar.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(initialCalendar.get(Calendar.MONTH)) }

    val planDates: Set<Triple<Int, Int, Int>> = remember(plan, today, isRangeSelection) {
        buildSet {
            val cur = Calendar.getInstance().apply { time = plan.startDate }
            while (!cur.time.after(plan.endDate)) {
                // В режиме выбора диапазона (продукты) убираем прошедшие даты
                val isPast = isRangeSelection && cur.time.before(today)
                if (!isPast) {
                    add(Triple(cur.get(Calendar.DAY_OF_MONTH), cur.get(Calendar.MONTH), cur.get(Calendar.YEAR)))
                }
                cur.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 400.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isRangeSelection) "Выберите период" else "Выберите дату",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack,
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val canGoPrev = displayYear > startCal.get(Calendar.YEAR) ||
                            (displayYear == startCal.get(Calendar.YEAR) && displayMonth > startCal.get(Calendar.MONTH))

                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { set(displayYear, displayMonth, 1); add(Calendar.MONTH, -1) }
                                displayYear = cal.get(Calendar.YEAR)
                                displayMonth = cal.get(Calendar.MONTH)
                            },
                            enabled = canGoPrev,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Text("‹", fontSize = 28.sp, color = if (canGoPrev) PrimaryGreen else Color.LightGray, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "${MY_PLAN_MONTH_NAMES[displayMonth]} $displayYear",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextBlack,
                        )

                        val canGoNext = displayYear < endCal.get(Calendar.YEAR) ||
                            (displayYear == endCal.get(Calendar.YEAR) && displayMonth < endCal.get(Calendar.MONTH))

                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { set(displayYear, displayMonth, 1); add(Calendar.MONTH, 1) }
                                displayYear = cal.get(Calendar.YEAR)
                                displayMonth = cal.get(Calendar.MONTH)
                            },
                            enabled = canGoNext,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Text("›", fontSize = 28.sp, color = if (canGoNext) PrimaryGreen else Color.LightGray, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        MY_PLAN_DAY_LABELS.forEach { label ->
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                            )
                        }
                    }

                    PlanCalendarGrid(
                        year = displayYear,
                        month = displayMonth,
                        planDates = planDates,
                        selectedDateMillis = normalizedSelectedDateMillis,
                        rangeStartMillis = rangeStart?.let(::normalizeDateMillis),
                        onDateClick = { day ->
                            val clickedDate = Calendar.getInstance().apply {
                                set(displayYear, displayMonth, day, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.time
                            
                            if (isRangeSelection) {
                                if (rangeStart == null) {
                                    rangeStart = clickedDate
                                } else {
                                    val start = rangeStart!!
                                    if (clickedDate.before(start)) {
                                        onRangeSelected?.invoke(clickedDate, start)
                                    } else {
                                        onRangeSelected?.invoke(start, clickedDate)
                                    }
                                    onDismiss()
                                }
                            } else {
                                onDateClick(clickedDate)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCalendarGrid(
    year: Int,
    month: Int,
    planDates: Set<Triple<Int, Int, Int>>,
    selectedDateMillis: Long?,
    rangeStartMillis: Long? = null,
    onDateClick: (Int) -> Unit,
) {
    val cal = Calendar.getInstance().apply { set(year, month, 1) }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val rawFirstDay = cal.get(Calendar.DAY_OF_WEEK)
    val firstDayOffset = if (rawFirstDay == Calendar.SUNDAY) 6 else rawFirstDay - 2

    val totalCells = firstDayOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(modifier = Modifier.fillMaxWidth()) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - firstDayOffset + 1
                    val isCurrentMonth = dayNumber in 1..daysInMonth
                    val isInPlan = isCurrentMonth && planDates.contains(Triple(dayNumber, month, year))
                    
                    val currentDayMillis = if (isCurrentMonth) createHomeDateMillis(year, month, dayNumber) else null
                    
                    val isSelected = isCurrentMonth && currentDayMillis == selectedDateMillis
                    val isRangeStart = isCurrentMonth && currentDayMillis == rangeStartMillis
                    
                    // Подсветка промежутка (если выбран старт)
                    val isInSelectionRange = isCurrentMonth && rangeStartMillis != null && 
                        currentDayMillis != null && selectedDateMillis != null &&
                        currentDayMillis in minOf(rangeStartMillis, selectedDateMillis)..maxOf(rangeStartMillis, selectedDateMillis)

                    // Для режима "в процессе выбора" (когда нажали первый раз, но не второй)
                    val isTentativeRange = isCurrentMonth && rangeStartMillis != null && currentDayMillis != null &&
                        currentDayMillis == rangeStartMillis

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .then(
                                if (isInPlan) {
                                    Modifier
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected || isRangeStart || isTentativeRange -> PrimaryGreen
                                                isInSelectionRange -> PrimaryGreen.copy(alpha = 0.3f)
                                                else -> PrimaryGreen.copy(alpha = 0.16f)
                                            }
                                        )
                                        .clickable { onDateClick(dayNumber) }
                                } else if (isCurrentMonth) {
                                    Modifier.clip(CircleShape).clickable(enabled = false) {}
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isCurrentMonth) {
                            Text(
                                text = dayNumber.toString(),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected || isRangeStart || isTentativeRange) FontWeight.Bold else if (isInPlan) FontWeight.SemiBold else FontWeight.Normal,
                                color = when {
                                    isSelected || isRangeStart || isTentativeRange -> Color.White
                                    isInPlan -> PrimaryGreen
                                    else -> TextBlack.copy(alpha = 0.35f)
                                },
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyPlanSection(
    customPlan: CustomPlan?,
    selectedDate: Date?,
    isRangeSelection: Boolean = false,
    onDateSelectedFromPlan: (Date) -> Unit,
    onRangeSelected: ((Date, Date) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    MyPlanButton(
        customPlan = customPlan,
        onClick = { if (customPlan != null) showDialog = true },
        modifier = modifier,
    )

    if (showDialog && customPlan != null) {
        MyPlanCalendarDialog(
            plan = customPlan,
            selectedDate = selectedDate,
            isRangeSelection = isRangeSelection,
            onDateClick = { date ->
                onDateSelectedFromPlan(date)
                showDialog = false
            },
            onRangeSelected = { start, end ->
                onRangeSelected?.invoke(start, end)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

private fun normalizeDateMillis(date: Date): Long {
    return Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun createHomeDateMillis(year: Int, month: Int, day: Int): Long {
    return Calendar.getInstance().apply {
        set(year, month, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
