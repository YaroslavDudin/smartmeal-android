package com.example.smartmeal.feature.setup.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.calendar.SmartMealCalendar
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.TextBlack
import java.util.Calendar

/**
 * Шаг 2 из 3: выбор типа периода (день / неделя / свой план) и даты через интерактивный календарь.
 */
@Composable
fun SetupStep2Screen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
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
    )
}

@Composable
fun SetupStep2Content(
    state: SetupState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSelectPeriodType: (PeriodType) -> Unit,
    onSelectDay: (Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 640
    val isCompactWidth = configuration.screenWidthDp < 360
    val horizontalPadding = if (isCompactHeight || isCompactWidth) 16.dp else 24.dp
    val verticalPadding = if (isCompactHeight) 16.dp else 28.dp
    val sectionSpacing = if (isCompactHeight) 12.dp else 24.dp
    val chipRowSpacing = if (isCompactHeight) 10.dp else 12.dp
    val calendarPadding = if (isCompactHeight) 12.dp else 16.dp
    val calendarWidth = when {
        isCompactWidth -> 0.9f
        isCompactHeight -> 0.95f
        else -> 1f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
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

        Spacer(modifier = Modifier.height(sectionSpacing))

        SmartMealText(
            text = "Тип периода",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = TextBlack,
        )

        Spacer(modifier = Modifier.height(chipRowSpacing))

        // --- Period type selector ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(chipRowSpacing),
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
                    compact = isCompactHeight,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(tag),
                )
            }
        }

        Spacer(modifier = Modifier.height(chipRowSpacing))

        PeriodChip(
            label = PeriodType.CUSTOM.label,
            isSelected = state.periodType == PeriodType.CUSTOM,
            onClick = { onSelectPeriodType(PeriodType.CUSTOM) },
            compact = isCompactHeight,
            modifier = Modifier
                .width(if (isCompactHeight) 160.dp else 180.dp)
                .align(Alignment.CenterHorizontally)
                .testTag("setup_step2_period_custom"),
        )

        Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else 20.dp))

        // --- Calendar ---
        Surface(
            modifier = Modifier
                .fillMaxWidth(calendarWidth)
                .align(Alignment.CenterHorizontally)
                .testTag("setup_step2_calendar"),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(1.5.dp, PrimaryGreen),
        ) {
            SmartMealCalendar(
                year = state.calendarYear,
                month = state.calendarMonth,
                periodType = state.periodType,
                selectedDay = state.selectedDay,
                selectedEndDay = state.selectedEndDay,
                onDaySelected = onSelectDay,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                showNavigation = true,
                showYear = false,
                showAdjacentMonths = true,
                compact = isCompactHeight,
                modifier = Modifier.padding(calendarPadding),
                isDateSelectable = { year, month, day ->
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val candidate = Calendar.getInstance().apply {
                        set(year, month, day, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    !candidate.before(today)
                },
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else 24.dp))

        WidePrimaryButton(
            text = "Дальше",
            onClick = onNext,
            enabled = when (state.periodType) {
                PeriodType.CUSTOM -> state.selectedDay != null && state.selectedEndDay != null
                else -> state.selectedDay != null
            },
            modifier = Modifier.testTag("setup_step2_next")
        )
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
    val borderColor = Color(0xFFE6D36E)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(1.5.dp, borderColor),
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
    val borderColor = PrimaryGreen
    val elevation = if (isSelected) 8.dp else 6.dp
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
            disabledContainerColor = Color.LightGray.copy(alpha = 0.5f),
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
