package com.example.smartmeal.feature.setup.presentation

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.calendar.SmartMealCalendar
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.SmartMealCardBorder
import com.example.smartmeal.ui.theme.SmartMealSurfaceSoft
import com.example.smartmeal.ui.theme.TextBlack
import java.util.Calendar
internal const val MAX_PLAN_SELECTION_DAYS = 256

/**
 * Шаг 2 из 3: выбор типа периода (день / неделя / свой план) и даты через интерактивный календарь.
 */
@Composable
fun SetupStep2Screen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextButtonText: String = "Дальше",
) {
    val state by viewModel.state.collectAsState()

    SetupStep2Content(
        state = state,
        onBack = onBack,
        onNext = onNext,
        onSelectPeriodType = { viewModel.selectPeriodType(it) },
        onSelectDay = viewModel::selectDay,
        onPreviousMonth = { viewModel.navigateCalendarMonth(-1) },
        onNextMonth = { viewModel.navigateCalendarMonth(1) },
        nextButtonText = nextButtonText,
    )
}

@Composable
fun SetupStep2Content(
    state: SetupState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextButtonText: String = "Дальше",
    onSelectPeriodType: (PeriodType) -> Unit,
    onSelectDay: (Int, Int, Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = configuration.screenWidthDp >= 600
    val today = createNormalizedTodayCalendar()
    val isCompactHeight = configuration.screenHeightDp < 640
    val isCompactWidth = configuration.screenWidthDp < 360
    val useCompactCalendar = isCompactHeight || isCompactWidth || isLandscape
    val horizontalPadding = if (isCompactWidth) 8.dp else if (isCompactHeight) 12.dp else 24.dp
    val verticalPadding = if (isCompactHeight) 12.dp else 28.dp
    val contentMaxWidth: Dp = if (isWideScreen) 460.dp else Dp.Unspecified
    val canNavigatePrevious = canNavigateToOffsetMonth(state.calendarYear, state.calendarMonth, -1, today)
    val canNavigateNext = canNavigateToOffsetMonth(state.calendarYear, state.calendarMonth, 1, today)
    val scrollState = rememberScrollState()

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Левая часть: Выбор периода
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepIndicator(current = 2, total = 3)
                        BackButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("setup_step2_back")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SmartMealText(
                        text = "Тип периода",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PeriodType.entries.take(2).forEach { type ->
                                val tag = when (type) {
                                    PeriodType.DAILY -> "setup_step2_period_daily"
                                    PeriodType.WEEKLY -> "setup_step2_period_weekly"
                                    else -> "setup_step2_period_other"
                                }
                                PeriodChip(
                                    label = type.label,
                                    isSelected = state.periodType == type,
                                    onClick = { onSelectPeriodType(type) },
                                    compact = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(tag)
                                )
                            }
                        }
                        PeriodChip(
                            label = PeriodType.CUSTOM.label,
                            isSelected = state.periodType == PeriodType.CUSTOM,
                            onClick = { onSelectPeriodType(PeriodType.CUSTOM) },
                            compact = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_step2_period_custom")
                        )
                    }
                }

                WidePrimaryButton(
                    text = nextButtonText,
                    onClick = onNext,
                    enabled = when (state.periodType) {
                        PeriodType.CUSTOM -> state.selectedStartDateMillis != null && state.selectedEndDateMillis != null
                        PeriodType.DAILY, PeriodType.WEEKLY -> state.selectedStartDateMillis != null
                        null -> false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_step2_next")
                )
            }

            // Правая часть: Календарь
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_step2_calendar"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, SmartMealCardBorder),
                ) {
                    SmartMealCalendar(
                        year = state.calendarYear,
                        month = state.calendarMonth,
                        periodType = state.periodType,
                        selectedStartDateMillis = state.selectedStartDateMillis,
                        selectedEndDateMillis = state.selectedEndDateMillis,
                        onDaySelected = onSelectDay,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        enabled = state.periodType != null,
                        canNavigatePrevious = canNavigatePrevious,
                        canNavigateNext = canNavigateNext,
                        showNavigation = true,
                        showYear = false,
                        showAdjacentMonths = false,
                        compact = true,
                        modifier = Modifier.padding(8.dp),
                        isDateSelectable = { year, month, day -> isPlanDateSelectable(year, month, day, today) },
                    )
                }
            }
        }
    } else {
        val bottomButtonSpace = if (isCompactHeight) 96.dp else 112.dp
        Box(
            modifier = Modifier.fillMaxSize().systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = horizontalPadding, vertical = 16.dp)
                    .padding(bottom = bottomButtonSpace)
                    .width(if (isWideScreen) contentMaxWidth else Dp.Unspecified)
                    .align(Alignment.TopCenter),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StepIndicator(current = 2, total = 3)
                    BackButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("setup_step2_back")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SmartMealText(
                    text = "Тип периода",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PeriodType.entries.take(2).forEach { type ->
                        val tag = when (type) {
                            PeriodType.DAILY -> "setup_step2_period_daily"
                            PeriodType.WEEKLY -> "setup_step2_period_weekly"
                            else -> "setup_step2_period_other"
                        }
                        PeriodChip(
                            label = type.label,
                            isSelected = state.periodType == type,
                            onClick = { onSelectPeriodType(type) },
                            compact = useCompactCalendar,
                            modifier = Modifier
                                .weight(1f)
                                .testTag(tag),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                PeriodChip(
                    label = PeriodType.CUSTOM.label,
                    isSelected = state.periodType == PeriodType.CUSTOM,
                    onClick = { onSelectPeriodType(PeriodType.CUSTOM) },
                    compact = useCompactCalendar,
                    modifier = if (isWideScreen) {
                        Modifier
                            .fillMaxWidth(0.5f)
                            .align(Alignment.CenterHorizontally)
                            .testTag("setup_step2_period_custom")
                    } else {
                        Modifier
                            .width(if (useCompactCalendar) 148.dp else 180.dp)
                            .align(Alignment.CenterHorizontally)
                            .testTag("setup_step2_period_custom")
                    },
                )

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .testTag("setup_step2_calendar"),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, SmartMealCardBorder),
                ) {
                    SmartMealCalendar(
                        year = state.calendarYear,
                        month = state.calendarMonth,
                        periodType = state.periodType,
                        selectedStartDateMillis = state.selectedStartDateMillis,
                        selectedEndDateMillis = state.selectedEndDateMillis,
                        onDaySelected = onSelectDay,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        enabled = state.periodType != null,
                        canNavigatePrevious = canNavigatePrevious,
                        canNavigateNext = canNavigateNext,
                        showNavigation = true,
                        showYear = false,
                        showAdjacentMonths = !isCompactWidth,
                        compact = useCompactCalendar,
                        modifier = Modifier.padding(16.dp),
                        isDateSelectable = { year, month, day -> isPlanDateSelectable(year, month, day, today) },
                    )
                }
            }

            WidePrimaryButton(
                text = nextButtonText,
                onClick = onNext,
                enabled = when (state.periodType) {
                    PeriodType.CUSTOM -> state.selectedStartDateMillis != null && state.selectedEndDateMillis != null
                    PeriodType.DAILY, PeriodType.WEEKLY -> state.selectedStartDateMillis != null
                    null -> false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = contentMaxWidth)
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = horizontalPadding, vertical = if (isCompactHeight) 16.dp else 24.dp)
                    .testTag("setup_step2_next")
            )
        }
    }
}


internal fun isPlanDateSelectable(
    year: Int,
    month: Int,
    day: Int,
    today: Calendar = createNormalizedTodayCalendar(),
): Boolean {
    val candidate = createNormalizedCalendar(year, month, day)
    val maxSelectableDate = createMaxSelectableCalendar(today)
    return !candidate.before(today) && !candidate.after(maxSelectableDate)
}

internal fun canNavigateToOffsetMonth(
    currentYear: Int,
    currentMonth: Int,
    monthOffset: Int,
    today: Calendar = createNormalizedTodayCalendar(),
): Boolean {
    val candidateMonth = createNormalizedCalendar(currentYear, currentMonth, 1).apply {
        add(Calendar.MONTH, monthOffset)
    }
    return canDisplayMonthForPlanSelection(
        year = candidateMonth.get(Calendar.YEAR),
        month = candidateMonth.get(Calendar.MONTH),
        today = today,
    )
}

internal fun canDisplayMonthForPlanSelection(
    year: Int,
    month: Int,
    today: Calendar = createNormalizedTodayCalendar(),
): Boolean {
    val maxSelectableDate = createMaxSelectableCalendar(today)
    val monthStart = createNormalizedCalendar(year, month, 1)
    val monthEnd = (monthStart.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
    }
    return !monthEnd.before(today) && !monthStart.after(maxSelectableDate)
}

internal fun createMaxSelectableCalendar(today: Calendar = createNormalizedTodayCalendar()): Calendar {
    return (today.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, MAX_PLAN_SELECTION_DAYS - 1)
    }
}

internal fun createNormalizedTodayCalendar(): Calendar {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

internal fun createNormalizedCalendar(year: Int, month: Int, day: Int): Calendar {
    return Calendar.getInstance().apply {
        set(year, month, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SmartMealText(
            text = "Шаг:",
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryGreen,
            fontWeight = FontWeight.Medium,
        )
        SmartMealText(
            text = " $current",
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryGreen,
            fontWeight = FontWeight.SemiBold,
        )
        SmartMealText(
            text = " /$total",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFBDBDBD),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = SmartMealCardBorder
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 5.dp,
        border = BorderStroke(1.dp, borderColor),
    ) {
        SmartMealText(
            text = "Назад",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextBlack,
        )
    }
}

@Composable
private fun PeriodChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val bgColor = if (isSelected) PrimaryGreen else Color.White
    val textColor = if (isSelected) Color.White else TextBlack
    val borderColor = if (isSelected) PrimaryGreen else SmartMealCardBorder
    val elevation = if (isSelected) 5.dp else 2.dp
    val verticalPadding = if (compact) 8.dp else 10.dp
    val horizontalPadding = if (compact) 12.dp else 16.dp
    val textStyle = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        shadowElevation = elevation,
        border = BorderStroke(1.dp, borderColor),
    ) {
        SmartMealText(
            text = label,
            modifier = Modifier.padding(vertical = verticalPadding, horizontal = horizontalPadding),
            color = textColor,
            style = textStyle,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun WidePrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryGreen,
            contentColor = Color.White,
            disabledContainerColor = SmartMealSurfaceSoft,
            disabledContentColor = Color.Gray,
        ),
    ) {
        SmartMealText(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
