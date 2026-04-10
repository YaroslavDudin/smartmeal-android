package com.example.smartmeal.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieSettingsScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    // Локальное состояние для редактирования
    var isEnabled by remember { mutableStateOf(viewModel.isCaloriesEnabled()) }
    var totalCalories by remember { mutableIntStateOf(state.totalCalories) }
    var calorieMargin by remember { mutableIntStateOf(viewModel.getCalorieMargin()) }
    
    var breakfastCals by remember { mutableStateOf(state.mealCalories["Завтрак"]?.toString() ?: "600") }
    var lunchCals by remember { mutableStateOf(state.mealCalories["Обед"]?.toString() ?: "800") }
    var dinnerCals by remember { mutableStateOf(state.mealCalories["Ужин"]?.toString() ?: "600") }

    // Sync logic: Slider -> Meals
    val updateMealsFromTotal = { total: Int ->
        breakfastCals = (total * 0.3).toInt().toString()
        lunchCals = (total * 0.4).toInt().toString()
        dinnerCals = (total * 0.3).toInt().toString()
    }

    // Sync logic: Meals -> Total
    val updateTotalFromMeals = {
        val b = breakfastCals.toIntOrNull() ?: 0
        val l = lunchCals.toIntOrNull() ?: 0
        val d = dinnerCals.toIntOrNull() ?: 0
        totalCalories = b + l + d
    }

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
                text = "Целевая калорийность",
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
            // --- Переключатель включения ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SmartMealText(
                            text = "Планировать по калориям",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        SmartMealText(
                            text = if (isEnabled) "Активно" else "Выключено (классический режим)",
                            fontSize = 13.sp,
                            color = if (isEnabled) PrimaryGreen else Color.Gray
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.LightGray
                        )
                    )
                }
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(20.dp))
                
                // --- Основные настройки ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SmartMealText(
                            text = "Общая цель: $totalCalories ккал",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Slider(
                            value = totalCalories.toFloat(),
                            onValueChange = { 
                                val newValue = it.toInt()
                                totalCalories = newValue
                                updateMealsFromTotal(newValue)
                            },
                            valueRange = 800f..4000f,
                            steps = 32,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryGreen,
                                activeTrackColor = PrimaryGreen,
                                inactiveTrackColor = Color(0xFFE0E0E0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- Выбор разброса (Margin) ---
                SmartMealText(
                    text = "Допустимый разброс (± ккал)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100, 150, 200).forEach { margin ->
                        FilterChip(
                            selected = calorieMargin == margin,
                            onClick = { calorieMargin = margin },
                            label = { SmartMealText("±$margin", fontSize = 14.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Color.Gray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (calorieMargin == margin) PrimaryGreen else Color(0xFFE0E0E0),
                                borderWidth = 1.dp,
                                enabled = true,
                                selected = calorieMargin == margin
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- Приемы пищи ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CalorieInputBox(
                        label = "Завтрак",
                        value = breakfastCals,
                        onValueChange = { 
                            breakfastCals = it
                            updateTotalFromMeals()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    CalorieInputBox(
                        label = "Обед",
                        value = lunchCals,
                        onValueChange = { 
                            lunchCals = it
                            updateTotalFromMeals()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    CalorieInputBox(
                        label = "Ужин",
                        value = dinnerCals,
                        onValueChange = { 
                            dinnerCals = it
                            updateTotalFromMeals()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // --- Кнопка сброса ---
            OutlinedButton(
                onClick = {
                    isEnabled = false
                    totalCalories = 2000
                    calorieMargin = 100
                    updateMealsFromTotal(2000)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
            ) {
                SmartMealText("Сбросить (По умолчанию)")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- Кнопка Сохранить ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        val meals = mapOf(
                            "Завтрак" to (breakfastCals.toIntOrNull() ?: 600),
                            "Обед" to (lunchCals.toIntOrNull() ?: 800),
                            "Ужин" to (dinnerCals.toIntOrNull() ?: 600)
                        )
                        viewModel.saveCalorieSettings(isEnabled, totalCalories, calorieMargin, meals)
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    SmartMealText(
                        text = "Сохранить настройки",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CalorieInputBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SmartMealText(text = label, fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = value,
                onValueChange = { 
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        onValueChange(it)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    textAlign = TextAlign.Center, 
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFF9F9F9),
                    unfocusedContainerColor = Color(0xFFF9F9F9),
                    cursorColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp)
            )
            SmartMealText(text = "ккал", fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
