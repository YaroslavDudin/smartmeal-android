package com.example.smartmeal.feature.profile.presentation

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.theme.BgLightGray
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.SmartMealBlue
import com.example.smartmeal.ui.theme.SmartMealCardBorder
import com.example.smartmeal.ui.theme.SmartMealOrange
import com.example.smartmeal.ui.theme.SmartMealSurfaceSoft
import com.example.smartmeal.ui.theme.SmartMealTextSecondary
import com.example.smartmeal.ui.theme.TextBlack
import kotlin.math.roundToInt

private val CalorieBorder = SmartMealCardBorder
private val CalorieMuted = SmartMealTextSecondary
private const val MIN_MACRO_PERCENT = 10
private const val MAX_MACRO_PERCENT = 80

@Composable
fun CalorieSettingsScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    var isEnabled by remember { mutableStateOf(viewModel.isCaloriesEnabled()) }
    var totalCalories by remember { mutableIntStateOf(state.totalCalories.coerceIn(1200, 9000)) }
    var calorieMargin by remember { mutableIntStateOf(viewModel.getCalorieMargin()) }
    var proteinPercent by remember {
        mutableIntStateOf(state.proteinPercent.coerceIn(MIN_MACRO_PERCENT, MAX_MACRO_PERCENT))
    }
    var fatPercent by remember {
        val initialProtein = state.proteinPercent.coerceIn(MIN_MACRO_PERCENT, MAX_MACRO_PERCENT)
        mutableIntStateOf(state.fatPercent.coerceIn(MIN_MACRO_PERCENT, 100 - initialProtein - MIN_MACRO_PERCENT))
    }

    // NEW: Manual meal distributions
    var breakfastCals by remember { mutableIntStateOf(state.mealCalories["Завтрак"] ?: (totalCalories * 0.3).roundToInt()) }
    var lunchCals by remember { mutableIntStateOf(state.mealCalories["Обед"] ?: (totalCalories * 0.4).roundToInt()) }
    var dinnerCals by remember { mutableIntStateOf(state.mealCalories["Ужин"] ?: (totalCalories * 0.3).roundToInt()) }

    val carbsPercent = (100 - proteinPercent - fatPercent).coerceAtLeast(MIN_MACRO_PERCENT)
    val animatedCalories by animateIntAsState(
        targetValue = totalCalories,
        animationSpec = tween(durationMillis = 220),
        label = "calorie_target"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLightGray)
    ) {
        Header(onBack = onBack)

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ToggleCard(
                    isEnabled = isEnabled,
                    onCheckedChange = {
                        isEnabled = it
                        if (it) calorieMargin = 100
                    }
                )

                if (isEnabled) {
                    CalorieGoalCard(
                        calories = animatedCalories,
                        onCaloriesChange = { newTotal ->
                            val oldTotal = totalCalories.coerceAtLeast(1)
                            val scale = newTotal.toFloat() / oldTotal
                            totalCalories = newTotal
                            breakfastCals = (breakfastCals * scale).roundToInt()
                            lunchCals = (lunchCals * scale).roundToInt()
                            dinnerCals = (dinnerCals * scale).roundToInt()
                            
                            // Ensure sum matches exactly after scaling due to rounding
                            val currentSum = breakfastCals + lunchCals + dinnerCals
                            val diff = newTotal - currentSum
                            lunchCals += diff
                        },
                    )

                    // Meal Specific Distribution
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, CalorieBorder),
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SmartMealText(
                                text = "Распределение по приемам",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextBlack
                            )

                            MealCalorieRow(
                                label = "Завтрак",
                                calories = breakfastCals,
                                totalCalories = 3000,
                                color = SmartMealOrange,
                                onValueChange = { 
                                    breakfastCals = it
                                    totalCalories = (breakfastCals + lunchCals + dinnerCals).coerceIn(1200, 9000)
                                }
                            )
                            MealCalorieRow(
                                label = "Обед",
                                calories = lunchCals,
                                totalCalories = 3000,
                                color = PrimaryGreen,
                                onValueChange = { 
                                    lunchCals = it
                                    totalCalories = (breakfastCals + lunchCals + dinnerCals).coerceIn(1200, 9000)
                                }
                            )
                            MealCalorieRow(
                                label = "Ужин",
                                calories = dinnerCals,
                                totalCalories = 3000,
                                color = SmartMealBlue,
                                onValueChange = { 
                                    dinnerCals = it
                                    totalCalories = (breakfastCals + lunchCals + dinnerCals).coerceIn(1200, 9000)
                                }
                            )
                            
                            // Progress bar showing the balance
                            val currentSum = breakfastCals + lunchCals + dinnerCals
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SmartMealText(
                                        text = "Итого распределено",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CalorieMuted
                                    )
                                    SmartMealText(
                                        text = "$currentSum / $totalCalories",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentSum == totalCalories) PrimaryGreen else SmartMealOrange
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, CalorieBorder),
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            SmartMealText(
                                text = "Распределение макронутриентов",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextBlack
                            )
                            MacroTargetRow(
                                label = "Белки",
                                valuePercent = proteinPercent,
                                grams = macroGrams(totalCalories, proteinPercent, caloriesPerGram = 4),
                                color = PrimaryGreen,
                                icon = Icons.Default.CheckCircle,
                                onPercentChange = { raw ->
                                    proteinPercent = raw
                                        .coerceIn(MIN_MACRO_PERCENT, 100 - fatPercent - MIN_MACRO_PERCENT)
                                }
                            )
                            MacroTargetRow(
                                label = "Жиры",
                                valuePercent = fatPercent,
                                grams = macroGrams(totalCalories, fatPercent, caloriesPerGram = 9),
                                color = SmartMealOrange,
                                icon = Icons.Default.LocalFireDepartment,
                                onPercentChange = { raw ->
                                    fatPercent = raw
                                        .coerceIn(MIN_MACRO_PERCENT, 100 - proteinPercent - MIN_MACRO_PERCENT)
                                }
                            )
                            MacroTargetRow(
                                label = "Углеводы",
                                valuePercent = carbsPercent,
                                grams = macroGrams(totalCalories, carbsPercent, caloriesPerGram = 4),
                                color = SmartMealBlue,
                                icon = Icons.Default.Restaurant,
                                enabled = false,
                                onPercentChange = {}
                            )
                        }
                    }
                } else {
                    DisabledHintCard()
                }
            }

            Button(
                onClick = {
                    val mealCalories = mapOf(
                        "Завтрак" to breakfastCals,
                        "Обед" to lunchCals,
                        "Ужин" to dinnerCals
                    )
                    viewModel.saveCalorieSettings(
                        enabled = isEnabled,
                        total = totalCalories,
                        margin = calorieMargin,
                        meals = mealCalories,
                        proteinPercent = proteinPercent,
                        fatPercent = fatPercent,
                        carbsPercent = carbsPercent
                    )
                    onBack()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                SmartMealText(
                    text = "Сохранить",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealCalorieRow(
    label: String,
    calories: Int,
    totalCalories: Int,
    color: Color,
    onValueChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmartMealText(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            SmartMealText(
                text = "$calories ккал",
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = calories.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..totalCalories.toFloat(),
            thumb = { CalorieSliderThumb(color) },
            colors = SliderDefaults.colors(
                activeTrackColor = color,
                inactiveTrackColor = SmartMealSurfaceSoft
            )
        )
    }
}

@Composable
private fun CalorieSliderThumb(color: Color) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .shadow(elevation = 4.dp, shape = CircleShape)
            .background(color, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Subtle inner dot for depth
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color.White.copy(alpha = 0.35f), CircleShape)
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = TextBlack
            )
        }
        SmartMealText(
            text = "Целевая калорийность",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ToggleCard(
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CalorieBorder),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SmartMealText(
                    text = "Планировать по калориям",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                SmartMealText(
                    text = if (isEnabled) "Активно" else "Отключено",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled) PrimaryGreen else CalorieMuted
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryGreen
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalorieGoalCard(
    calories: Int,
    onCaloriesChange: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CalorieBorder),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SmartMealText(
                text = "Общая цель",
                style = MaterialTheme.typography.bodyMedium,
                color = CalorieMuted
            )
            SmartMealText(
                text = "$calories ккал",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
            Slider(
                value = calories.toFloat(),
                onValueChange = { onCaloriesChange(it.roundToInt()) },
                valueRange = 1200f..9000f,
                thumb = { CalorieSliderThumb(PrimaryGreen) },
                colors = SliderDefaults.colors(
                    activeTrackColor = PrimaryGreen,
                    inactiveTrackColor = SmartMealSurfaceSoft
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SmartMealText("1200", style = MaterialTheme.typography.labelMedium, color = CalorieMuted)
                SmartMealText("9000", style = MaterialTheme.typography.labelMedium, color = CalorieMuted)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MacroTargetRow(
    label: String,
    valuePercent: Int,
    grams: Int,
    color: Color,
    icon: ImageVector,
    enabled: Boolean = true,
    onPercentChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            SmartMealText(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextBlack
            )
            SmartMealText(
                text = "$grams г",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextBlack
            )
            Spacer(modifier = Modifier.width(12.dp))
            SmartMealText(
                text = "$valuePercent%",
                style = MaterialTheme.typography.labelMedium,
                color = CalorieMuted
            )
        }
        Slider(
            value = valuePercent.toFloat(),
            onValueChange = { onPercentChange(it.roundToInt()) },
            enabled = enabled,
            valueRange = MIN_MACRO_PERCENT.toFloat()..MAX_MACRO_PERCENT.toFloat(),
            thumb = { CalorieSliderThumb(if (enabled) color else Color.LightGray) },
            colors = SliderDefaults.colors(
                activeTrackColor = color,
                inactiveTrackColor = SmartMealSurfaceSoft,
                disabledActiveTrackColor = color.copy(alpha = 0.5f),
                disabledInactiveTrackColor = SmartMealSurfaceSoft
            )
        )
    }
}

@Composable
private fun DisabledHintCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CalorieBorder)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryGreen.copy(alpha = 0.14f), SmartMealSurfaceSoft)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = PrimaryGreen
                )
            }
            SmartMealText(
                text = "Калорийная цель отключена",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            SmartMealText(
                text = "Меню продолжит подбираться по рациону, аллергиям, порциям и времени готовки.",
                style = MaterialTheme.typography.bodyMedium,
                color = CalorieMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun macroGrams(totalCalories: Int, percent: Int, caloriesPerGram: Int): Int {
    return (totalCalories * (percent / 100.0) / caloriesPerGram).roundToInt()
}
