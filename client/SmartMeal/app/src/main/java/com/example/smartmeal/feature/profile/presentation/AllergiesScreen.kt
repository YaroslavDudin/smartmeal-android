package com.example.smartmeal.feature.profile.presentation

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.smartmeal.feature.setup.data.models.AllergyDto
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.buttons.SmartMealButton
import com.example.smartmeal.ui.components.buttons.SmartMealButtonColor
import com.example.smartmeal.ui.components.buttons.SmartMealButtonVariant
import com.example.smartmeal.ui.theme.BgLightGray
import com.example.smartmeal.ui.theme.BorderGray
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.TextBlack

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllergiesScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.savedSuccess) {
        if (state.savedSuccess) {
            viewModel.clearSavedSuccess()
            onBack()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgLightGray)) {
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
                text = "Аллергии",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // --- Основная область с плавающей кнопкой ---
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryGreen)
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))

                    SmartMealText(
                        text = "Настройте список ваших аллергий",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextBlack,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    SmartMealText(
                        text = "Мы будем исключать рецепты с этими ингредиентами из вашего плана питания.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (state.allAllergies.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            SmartMealText(
                                text = "Список аллергенов пока недоступен",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        if (isLandscape) {
                            // ЛАНДШАФТ: Две колонки для аллергий
                            state.allAllergies.chunked(2).forEach { rowAllergies ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowAllergies.forEach { allergy ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            AllergyCard(
                                                allergy = allergy,
                                                isChecked = allergy.id in state.pendingAllergyIds,
                                                onClick = { viewModel.togglePendingAllergy(allergy.id) }
                                            )
                                        }
                                    }
                                    if (rowAllergies.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else {
                            // ПОРТРЕТ: Обычный список
                            state.allAllergies.forEach { allergy ->
                                AllergyCard(
                                    allergy = allergy,
                                    isChecked = allergy.id in state.pendingAllergyIds,
                                    onClick = { viewModel.togglePendingAllergy(allergy.id) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val selectedAllergies = state.allAllergies.filter { it.id in state.pendingAllergyIds }

                    SmartMealText(
                        text = if (selectedAllergies.isEmpty()) "Аллергии не выбраны" else "Выбрано:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        selectedAllergies.forEach { allergy ->
                            AllergyChip(name = allergy.name)
                        }
                    }

                    if (state.error != null) {
                        SmartMealText(
                            text = state.error!!,
                            color = Color.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    // --- Кнопка в конце списка (не фиксированная) ---
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SmartMealButton(
                            text = if (state.isSaving) "..." else "Подтвердить",
                            onClick = { viewModel.saveAllergies() },
                            variant = SmartMealButtonVariant.PRIMARY,
                            color = SmartMealButtonColor.GREEN,
                            enabled = !state.isSaving,
                            modifier = if (isLandscape) Modifier.width(200.dp).height(42.dp)
                                       else Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllergyCard(
    allergy: AllergyDto,
    isChecked: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isChecked) Color(0xFFF4F9F4) else Color.White,
        border = if (isChecked)
            androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen)
        else
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SmartMealText(
                text = allergy.name,
                fontSize = 16.sp,
                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isChecked) PrimaryGreen else TextBlack
            )
            Icon(
                imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isChecked) PrimaryGreen else Color(0xFFE0E0E0),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun AllergyChip(name: String) {
    Surface(
        color = Color(0xFFFFF5F5),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC1C1).copy(alpha = 0.5f))
    ) {
        SmartMealText(
            text = name,
            color = Color(0xFFD32F2F),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
