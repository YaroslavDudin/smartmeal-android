package com.example.smartmeal.feature.setup.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartmeal.ui.components.buttons.SmartMealButton
import com.example.smartmeal.ui.components.buttons.SmartMealButtonColor
import com.example.smartmeal.ui.components.buttons.SmartMealButtonVariant
import com.example.smartmeal.ui.components.calendar.SmartMealCalendar
import com.example.smartmeal.ui.theme.LightGreenBg
import com.example.smartmeal.ui.theme.PrimaryGreen

/**
 * Шаг 2 из 3: выбор типа периода (день / неделя / свой план) и даты через интерактивный календарь.
 *
 * Поведение календаря зависит от выбранного типа:
 * - Дневной план  → выбирается один день (зелёный круг)
 * - Недельный план → выбирается вся неделя (Пн–Вс) одним нажатием
 * - Свой план     → выбирается один день как начало периода
 */
@Composable
fun SetupStep2Screen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Шаг: 2 / 3",
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryGreen,
                fontWeight = FontWeight.Medium,
            )
            TextButton(onClick = onBack) {
                Text(text = "Назад", color = MaterialTheme.colorScheme.onBackground)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Тип периода",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Period type selector ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PeriodType.entries.take(2).forEach { type ->
                PeriodChip(
                    label = type.label,
                    isSelected = state.periodType == type,
                    onClick = { viewModel.selectPeriodType(type) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        PeriodChip(
            label = PeriodType.CUSTOM.label,
            isSelected = state.periodType == PeriodType.CUSTOM,
            onClick = { viewModel.selectPeriodType(PeriodType.CUSTOM) },
            modifier = Modifier.fillMaxWidth(0.5f).align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Calendar ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
        ) {
            SmartMealCalendar(
                year = state.calendarYear,
                month = state.calendarMonth,
                periodType = state.periodType,
                selectedDay = state.selectedDay,
                selectedEndDay = state.selectedEndDay,
                onDaySelected = viewModel::selectDay,
                onPreviousMonth = { viewModel.navigateCalendarMonth(-1) },
                onNextMonth = { viewModel.navigateCalendarMonth(1) },
                modifier = Modifier.padding(16.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        SmartMealButton(
            text = "Дальше",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            variant = SmartMealButtonVariant.PRIMARY,
            color = SmartMealButtonColor.GREEN,
            enabled = when (state.periodType) {
                PeriodType.CUSTOM -> state.selectedDay != null && state.selectedEndDay != null
                else -> state.selectedDay != null
            },
        )
    }
}

@Composable
private fun PeriodChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isSelected) PrimaryGreen else Color.Transparent
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
    val borderColor = if (isSelected) PrimaryGreen else Color.LightGray

    Surface(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Text(
            text = label,
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
