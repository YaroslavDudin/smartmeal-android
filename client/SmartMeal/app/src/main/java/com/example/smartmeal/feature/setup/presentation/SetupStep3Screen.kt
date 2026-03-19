package com.example.smartmeal.feature.setup.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.buttons.SmartMealButton
import com.example.smartmeal.ui.components.buttons.SmartMealButtonColor
import com.example.smartmeal.ui.components.buttons.SmartMealButtonVariant
import com.example.smartmeal.ui.theme.GreenBorder
import com.example.smartmeal.ui.theme.MainGreen
import com.example.smartmeal.ui.theme.YellowBorder
import com.example.smartmeal.utils.ShadowData
import com.example.smartmeal.utils.dropShadow

private val SETUP_SHADOW = ShadowData(
    radius = 4.dp,
    spread = 0.dp,
    color = Color.Black.copy(alpha = 0.12f),
    offset = DpOffset(0.dp, 1.5.dp)
)

private val COOK_TIME_OPTIONS = listOf(
    Triple("short", "До 30 мин", false),
    Triple("medium", "От 30 до часа", true),
    Triple("long", "От часа и более", false),
)

/**
 * Шаг 3 из 3: выбор исключений (аллергии) и предпочтений по времени приготовления.
 */
@Composable
fun SetupStep3Screen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Navigate when setup is complete
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    SetupStep3Content(
        state = state,
        onBack = onBack,
        onSubmit = { viewModel.submitSetup() },
        onToggleAllergy = { viewModel.toggleAllergy(it) },
        onSetEatAll = { viewModel.setEatAll(it) },
        onSelectCookTime = { viewModel.selectCookTime(it) },
    )
}

@Composable
fun SetupStep3Content(
    state: SetupState,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onToggleAllergy: (Int) -> Unit,
    onSetEatAll: (Boolean) -> Unit,
    onSelectCookTime: (String) -> Unit,
) {
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
                text = "Шаг: 3 / 3",
                style = MaterialTheme.typography.bodyMedium,
                color = MainGreen,
                fontWeight = FontWeight.Medium,
            )
            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(1.dp, YellowBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(36.dp)
                    .dropShadow(shape = RoundedCornerShape(12.dp), shadow = SETUP_SHADOW),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text(text = "Назад", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Чего бы Вы не хотели\nвидеть в своём рационе?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("setup_step3_title")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Allergy chips ---
        if (state.allergies.isEmpty()) {
            Text(text = "Загрузка...", color = Color.Gray)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(((state.allergies.size / 2 + 1) * 60).dp)
                    .testTag("setup_step3_allergy_grid"),
            ) {
                items(state.allergies) { allergy ->
                    val isSelected = allergy.id in state.selectedAllergyIds
                    SelectableChip(
                        label = allergy.name,
                        isSelected = isSelected,
                        onClick = { onToggleAllergy(allergy.id) },
                        modifier = Modifier.testTag("setup_step3_allergy_${allergy.id}")
                    )
                }
                item {
                    SelectableChip(
                        label = "Ем всё",
                        isSelected = state.eatAll,
                        onClick = { onSetEatAll(!state.eatAll) },
                        modifier = Modifier.testTag("setup_step3_allergy_all")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Насколько сложные блюда\nВы хотите приготовить?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Cook time options ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            COOK_TIME_OPTIONS.take(2).forEach { (key, label, _) ->
                val tag = when (key) {
                    "short" -> "setup_step3_cook_under30"
                    "medium" -> "setup_step3_cook_30to60"
                    else -> "setup_step3_cook_other"
                }
                SelectableChip(
                    label = label,
                    isSelected = state.cookTimePreference == key,
                    onClick = { onSelectCookTime(key) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(tag),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        COOK_TIME_OPTIONS.drop(2).forEach { (key, label, _) ->
            val tag = when (key) {
                "long" -> "setup_step3_cook_over60"
                else -> "setup_step3_cook_other"
            }
            SelectableChip(
                label = label,
                isSelected = state.cookTimePreference == key,
                onClick = { onSelectCookTime(key) },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterHorizontally)
                    .testTag(tag),
            )
        }

        // --- Error message ---
        if (state.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        SmartMealButton(
            text = if (state.isLoading) "Сохраняем..." else "Сгенерировать",
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("setup_step3_submit"),
            variant = SmartMealButtonVariant.PRIMARY,
            color = SmartMealButtonColor.GREEN,
            enabled = !state.isLoading,
        )
    }
}

@Composable
private fun SelectableChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor = when {
        isSelected -> MainGreen
        else -> Color.White
    }
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
    val borderColor = if (isSelected) MainGreen else GreenBorder

    Surface(
        modifier = modifier
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
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
