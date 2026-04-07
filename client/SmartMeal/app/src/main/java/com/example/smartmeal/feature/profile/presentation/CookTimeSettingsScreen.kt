package com.example.smartmeal.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.theme.*

private val CardYellow = Color(0xFFFFF4C2)
private val YellowDivider = Color(0xFFD4B800)

private val COOK_TIME_OPTIONS = listOf(
    Triple("short", "До 30 мин", false),
    Triple("medium", "От 30 до часа", true),
    Triple("long", "От часа и более", false),
)

private val MEAL_TYPES_3 = listOf("Завтрак", "Обед", "Ужин")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookTimeSettingsScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var selectedMeal by remember { mutableStateOf<String?>(null) }
    var showEditModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLightGray)
    ) {
        // --- Шапка ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clickable { onBack() },
                tint = Color.Black
            )
            SmartMealText(
                text = "Время готовки",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // --- 3 Кнопки времен готовки ---
            MEAL_TYPES_3.forEach { meal ->
                val isSelected = selectedMeal == meal
                val currentPref = state.mealCookTimes[meal] ?: "any"
                val displayPref = COOK_TIME_OPTIONS.find { it.first == currentPref }?.second ?: "Любое время"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MainGreen else Color.White)
                        .border(2.dp, GreenBorder, RoundedCornerShape(16.dp))
                        .clickable { selectedMeal = if (isSelected) null else meal }
                        .padding(vertical = 14.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SmartMealText(
                        text = "$meal: $displayPref",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = YellowDivider,
                thickness = 1.5.dp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        // --- Кнопка Изменить ---
        Button(
            onClick = { showEditModal = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp),
            enabled = selectedMeal != null && !state.isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = CardYellow,
                contentColor = Color.Black,
                disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            SmartMealText(
                text = "Изменить",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // --- Кнопка Подтвердить (Обновляет план) ---
        Button(
            onClick = { 
                viewModel.confirmCookTimes()
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            enabled = !state.isRegenerating,
            colors = ButtonDefaults.buttonColors(
                containerColor = MainGreen,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (state.isRegenerating) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                SmartMealText(
                    text = "Подтвердить",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showEditModal && selectedMeal != null) {
        val currentApiValue = state.mealCookTimes[selectedMeal] ?: "any"

        CookTimeEditModal(
            mealName = selectedMeal!!,
            currentSelection = currentApiValue,
            onDismiss = { showEditModal = false },
            onConfirm = { newApiValue ->
                if (newApiValue != currentApiValue) {
                    val newMap = state.mealCookTimes.toMutableMap().apply {
                        put(selectedMeal!!, newApiValue)
                    }
                    viewModel.saveMealCookTimes(newMap)
                }
                showEditModal = false
                selectedMeal = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookTimeEditModal(
    mealName: String,
    currentSelection: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedOption by remember { mutableStateOf(currentSelection) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            SmartMealText(
                text = "Время готовки для: $mealName",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    TimeSelectableChip(
                        label = COOK_TIME_OPTIONS[0].second,
                        isSelected = selectedOption == COOK_TIME_OPTIONS[0].first,
                        onClick = { selectedOption = COOK_TIME_OPTIONS[0].first },
                        modifier = Modifier.weight(1f)
                    )
                    TimeSelectableChip(
                        label = COOK_TIME_OPTIONS[1].second,
                        isSelected = selectedOption == COOK_TIME_OPTIONS[1].first,
                        onClick = { selectedOption = COOK_TIME_OPTIONS[1].first },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    TimeSelectableChip(
                        label = COOK_TIME_OPTIONS[2].second,
                        isSelected = selectedOption == COOK_TIME_OPTIONS[2].first,
                        onClick = { selectedOption = COOK_TIME_OPTIONS[2].first },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onConfirm(selectedOption) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MainGreen, contentColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                SmartMealText(text = "Сохранить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TimeSelectableChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isSelected) MainGreen else Color.White
    val textColor = if (isSelected) Color.White else Color.Black
    val borderColor = if (isSelected) MainGreen else GreenBorder

    Surface(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp)
        ) {
            SmartMealText(
                text = label,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}
