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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

private val COOK_TIME_OPTIONS = listOf(
    Triple("under30", "До 30 мин", false),
    Triple("30to60", "От 30 до часа", true),
    Triple("over60", "От часа и более", false),
)

@Composable
fun SetupStep3Screen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    SetupStep3Content(
        state = state,
        onBack = onBack,
        onSubmit = { viewModel.submitSetup() },
        onToggleAllergy = { viewModel.toggleAllergy(it) },
        onSetEatAll = { viewModel.setEatAll(it) },
        onSelectCookTime = { viewModel.selectCookTime(mapUiCookTimeToApi(it)) },
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Левая часть: Заголовок и Кнопка
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
                        StepIndicatorLocal(current = 3, total = 3)
                        BackButtonLocal(onClick = onBack)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SmartMealText(
                        text = "Настройте Ваш рацион",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("setup_step3_title")
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SmartMealText(
                        text = "Выберите исключения и сложность блюд",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

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

            // Правая часть: Настройки (скролл)
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Аллергии
                Column {
                    SmartMealText(
                        text = "Исключить продукты",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (state.allergies.isEmpty()) {
                        SmartMealText(text = "Загрузка...", color = Color.Gray)
                    } else {
                        val allChipData = state.allergies.map { allergy ->
                            AllergyChipData(
                                label = allergy.name,
                                isSelected = allergy.id in state.selectedAllergyIds,
                                onClick = { onToggleAllergy(allergy.id) },
                                tag = "setup_step3_allergy_${allergy.id}"
                            )
                        } + AllergyChipData(
                            label = "Ем всё",
                            isSelected = state.eatAll,
                            onClick = { onSetEatAll(!state.eatAll) },
                            tag = "setup_step3_allergy_all"
                        )

                        // В ландшафте используем 3 колонки для экономии места
                        val chunkedChips = allChipData.chunked(3)
                        Column(
                            modifier = Modifier.fillMaxWidth().testTag("setup_step3_allergy_grid"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chunkedChips.forEach { rowChips ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowChips.forEach { chipData ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            SelectableChip(
                                                label = chipData.label,
                                                isSelected = chipData.isSelected,
                                                onClick = chipData.onClick,
                                                modifier = Modifier.testTag(chipData.tag)
                                            )
                                        }
                                    }
                                    repeat(3 - rowChips.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Сложность
                Column {
                    SmartMealText(
                        text = "Сложность блюд",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            COOK_TIME_OPTIONS.take(2).forEach { (key, label, _) ->
                                SelectableChip(
                                    label = label,
                                    isSelected = mapApiCookTimeToUi(state.cookTimePreference) == key,
                                    onClick = { onSelectCookTime(key) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        SelectableChip(
                            label = COOK_TIME_OPTIONS[2].second,
                            isSelected = mapApiCookTimeToUi(state.cookTimePreference) == COOK_TIME_OPTIONS[2].first,
                            onClick = { onSelectCookTime(COOK_TIME_OPTIONS[2].first) },
                            modifier = Modifier.fillMaxWidth(0.5f)
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepIndicatorLocal(current = 3, total = 3)
                BackButtonLocal(onClick = onBack)
            }

            Spacer(modifier = Modifier.height(24.dp))

            SmartMealText(
                text = "Чего бы Вы не хотели\nвидеть в своём рационе?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("setup_step3_title")
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.allergies.isEmpty()) {
                SmartMealText(text = "Загрузка...", color = Color.Gray)
            } else {
                val allChipData = state.allergies.map { allergy ->
                    AllergyChipData(
                        label = allergy.name,
                        isSelected = allergy.id in state.selectedAllergyIds,
                        onClick = { onToggleAllergy(allergy.id) },
                        tag = "setup_step3_allergy_${allergy.id}"
                    )
                } + AllergyChipData(
                    label = "Ем всё",
                    isSelected = state.eatAll,
                    onClick = { onSetEatAll(!state.eatAll) },
                    tag = "setup_step3_allergy_all"
                )

                val chunkedChips = allChipData.chunked(2)
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("setup_step3_allergy_grid"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chunkedChips.forEach { rowChips ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowChips.forEach { chipData ->
                                androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                                    SelectableChip(
                                        label = chipData.label,
                                        isSelected = chipData.isSelected,
                                        onClick = chipData.onClick,
                                        modifier = Modifier.testTag(chipData.tag)
                                    )
                                }
                            }
                            if (rowChips.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            SmartMealText(
                text = "Насколько сложные блюда\nВы хотите приготовить?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                COOK_TIME_OPTIONS.take(2).forEach { (key, label, _) ->
                    val tag = when (key) {
                        "under30" -> "setup_step3_cook_under30"
                        "30to60" -> "setup_step3_cook_30to60"
                        else -> "setup_step3_cook_other"
                    }
                    SelectableChip(
                        label = label,
                        isSelected = mapApiCookTimeToUi(state.cookTimePreference) == key,
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
                    "over60" -> "setup_step3_cook_over60"
                    else -> "setup_step3_cook_other"
                }
                SelectableChip(
                    label = label,
                    isSelected = mapApiCookTimeToUi(state.cookTimePreference) == key,
                    onClick = { onSelectCookTime(key) },
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .align(Alignment.CenterHorizontally)
                        .testTag(tag),
                )
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                SmartMealText(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

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
            .testTag("setup_step3_back")
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


private fun mapUiCookTimeToApi(value: String): String = when (value) {
    "under30" -> "short"
    "30to60" -> "medium"
    "over60" -> "long"
    else -> value
}

private fun mapApiCookTimeToUi(value: String?): String? = when (value) {
    "short" -> "under30"
    "medium" -> "30to60"
    "long" -> "over60"
    else -> value
}

@Composable
private fun SelectableChip(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private data class AllergyChipData(
    val label: String,
    val isSelected: Boolean,
    val onClick: () -> Unit,
    val tag: String
)
