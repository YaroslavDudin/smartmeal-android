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
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.platform.LocalConfiguration
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SmartMealText(
                text = "Выберите прием пищи, чтобы изменить предпочтительное время приготовления",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            // --- 3 Кнопки времен готовки ---
            MEAL_TYPES_3.forEach { meal ->
                val isSelected = selectedMeal == meal
                val currentPref = state.mealCookTimes[meal] ?: "any"
                val displayPref = COOK_TIME_OPTIONS.find { it.first == currentPref }?.second ?: "Любое время"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { selectedMeal = if (isSelected) null else meal },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PrimaryGreen else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            SmartMealText(
                                text = meal,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Black
                            )
                            SmartMealText(
                                text = displayPref,
                                fontSize = 14.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Кнопки действий внизу ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- Кнопка Изменить ---
                Button(
                    onClick = { showEditModal = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = selectedMeal != null && !state.isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CardYellow,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.LightGray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    SmartMealText(
                        text = "Изменить",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // --- Кнопка Подтвердить ---
                Button(
                    onClick = { 
                        viewModel.confirmCookTimes()
                        onBack()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = !state.isRegenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (state.isRegenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        SmartMealText(
                            text = "Подтвердить",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            SmartMealText(
                text = "Время готовки для: $mealName",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                SmartMealText(text = "Сохранить", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
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
    val bgColor = if (isSelected) PrimaryGreen else Color.White
    val textColor = if (isSelected) Color.White else Color.Black
    val borderColor = if (isSelected) PrimaryGreen else BorderGray

    Surface(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
        ) {
            SmartMealText(
                text = label,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
