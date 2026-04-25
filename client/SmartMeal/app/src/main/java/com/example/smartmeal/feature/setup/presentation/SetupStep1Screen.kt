package com.example.smartmeal.feature.setup.presentation

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.buttons.SmartMealButton
import com.example.smartmeal.ui.components.buttons.SmartMealButtonColor
import com.example.smartmeal.ui.components.buttons.SmartMealButtonVariant
import com.example.smartmeal.ui.theme.GreenBorder
import com.example.smartmeal.ui.theme.LightGreenBg
import com.example.smartmeal.ui.theme.MainGreen
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.TextBlack
import com.example.smartmeal.utils.ShadowData
import com.example.smartmeal.utils.dropShadow

private val SETUP_SHADOW = ShadowData(
    radius = 4.dp,
    spread = 0.dp,
    color = Color.Black.copy(alpha = 0.12f),
    offset = DpOffset(0.dp, 1.5.dp)
)

@Composable
fun SetupStep1Screen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    SetupStep1Content(
        state = state,
        onBack = onBack,
        onNext = onNext,
        onDietTypeClick = { viewModel.selectDietType(it) },
        onIncrement = viewModel::incrementPortion,
        onDecrement = viewModel::decrementPortion,
    )
}

@Composable
fun SetupStep1Content(
    state: SetupState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onDietTypeClick: (Int) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = androidx.compose.foundation.rememberScrollState()
    
    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Левая часть: Статика
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StepIndicatorLocal(current = 1, total = 3)
                        BackButtonLocal(onClick = onBack)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SmartMealText(
                        text = "Выберите тип питания",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("setup_step1_title")
                    )
                }

                SmartMealButton(
                    text = "Дальше",
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_step1_next"),
                    variant = SmartMealButtonVariant.PRIMARY,
                    color = SmartMealButtonColor.GREEN,
                    enabled = state.selectedDietTypeId != null,
                )
            }

            // Правая часть: Контент
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column {
                    SmartMealText(
                        text = "Предпочтения",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (state.dietTypes.isEmpty()) {
                        SmartMealText(text = "Загрузка...", color = Color.Gray)
                    } else {
                        val chunkedDietTypes = state.dietTypes.chunked(2)
                        Column(
                            modifier = Modifier.fillMaxWidth().testTag("setup_step1_diet_grid"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chunkedDietTypes.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { dietType ->
                                        val isSelected = state.selectedDietTypeId == dietType.id
                                        DietTypeChip(
                                            label = dietType.name,
                                            isSelected = isSelected,
                                            onClick = { onDietTypeClick(dietType.id) },
                                            modifier = Modifier.weight(1f).testTag("setup_step1_diet_${dietType.id}")
                                        )
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                Column {
                    SmartMealText(
                        text = "Размер семьи",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PortionStepper(
                        value = state.portionSize,
                        onIncrement = onIncrement,
                        onDecrement = onDecrement,
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepIndicatorLocal(current = 1, total = 3)
                BackButtonLocal(onClick = onBack)
            }

            Spacer(modifier = Modifier.height(24.dp))

            SmartMealText(
                text = "Выберите тип питания",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("setup_step1_title")
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.dietTypes.isEmpty()) {
                SmartMealText(text = "Загрузка...", color = Color.Gray)
            } else {
                val chunkedDietTypes = state.dietTypes.chunked(2)
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("setup_step1_diet_grid"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chunkedDietTypes.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { dietType ->
                                val isSelected = state.selectedDietTypeId == dietType.id
                                DietTypeChip(
                                    label = dietType.name,
                                    isSelected = isSelected,
                                    onClick = { onDietTypeClick(dietType.id) },
                                    modifier = Modifier.weight(1f).testTag("setup_step1_diet_${dietType.id}")
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            SmartMealText(
                text = "Размер семьи",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            PortionStepper(
                value = state.portionSize,
                onIncrement = onIncrement,
                onDecrement = onDecrement,
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            SmartMealButton(
                text = "Дальше",
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_step1_next"),
                variant = SmartMealButtonVariant.PRIMARY,
                color = SmartMealButtonColor.GREEN,
                enabled = state.selectedDietTypeId != null,
            )
        }
    }
}

@Composable
private fun StepIndicatorLocal(current: Int, total: Int) {
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
private fun BackButtonLocal(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .testTag("setup_step1_back")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(1.5.dp, Color(0xFFE6D36E)),
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
private fun DietTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val bgColor = if (isSelected) MainGreen else Color.White
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
    val borderColor = if (isSelected) MainGreen else GreenBorder

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(shape = RoundedCornerShape(12.dp), shadow = SETUP_SHADOW)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        SmartMealText(
            text = label,
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
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
                .testTag("setup_step1_portion_dec")
                .clickable(onClick = onDecrement),
            shape = RoundedCornerShape(8.dp),
            color = LightGreenBg,
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                SmartMealText(
                    text = "-",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        SmartMealText(
            text = "$value ${personLabel(value)}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("setup_step1_portion_value")
        )

        Spacer(modifier = Modifier.width(24.dp))

        Surface(
            modifier = Modifier
                .size(40.dp)
                .testTag("setup_step1_portion_inc")
                .clickable(onClick = onIncrement),
            shape = RoundedCornerShape(8.dp),
            color = PrimaryGreen,
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                SmartMealText(
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
