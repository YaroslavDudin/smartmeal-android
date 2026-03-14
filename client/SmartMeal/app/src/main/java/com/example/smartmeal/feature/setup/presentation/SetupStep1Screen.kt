package com.example.smartmeal.feature.setup.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.smartmeal.ui.theme.LightGreenBg
import com.example.smartmeal.ui.theme.PrimaryGreen

/**
 * Шаг 1 из 3: выбор типа питания и размера порции (количество персон).
 * Кнопка «Дальше» активна только если выбран хотя бы один тип питания.
 */
@Composable
fun SetupStep1Screen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Шаг: 1 / 3",
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
            text = "Выберите тип питания",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.dietTypes.isEmpty()) {
            Text(text = "Загрузка...", color = Color.Gray)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(state.dietTypes) { dietType ->
                    val isSelected = state.selectedDietTypeId == dietType.id
                    DietTypeChip(
                        label = dietType.name,
                        isSelected = isSelected,
                        onClick = { viewModel.selectDietType(dietType.id) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Размер семьи",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        PortionStepper(
            value = state.portionSize,
            onIncrement = viewModel::incrementPortion,
            onDecrement = viewModel::decrementPortion,
        )

        Spacer(modifier = Modifier.weight(1f))

        SmartMealButton(
            text = "Дальше",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            variant = SmartMealButtonVariant.PRIMARY,
            color = SmartMealButtonColor.GREEN,
            enabled = state.selectedDietTypeId != null,
        )
    }
}

@Composable
private fun DietTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isSelected) PrimaryGreen else Color.Transparent
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
    val borderColor = if (isSelected) PrimaryGreen else Color.LightGray

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PortionStepper(
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onDecrement),
            shape = RoundedCornerShape(8.dp),
            color = LightGreenBg,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        Text(
            text = "$value ${personLabel(value)}",
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(modifier = Modifier.width(24.dp))

        Surface(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onIncrement),
            shape = RoundedCornerShape(8.dp),
            color = PrimaryGreen,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun personLabel(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "персона"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "персоны"
    else -> "персон"
}
