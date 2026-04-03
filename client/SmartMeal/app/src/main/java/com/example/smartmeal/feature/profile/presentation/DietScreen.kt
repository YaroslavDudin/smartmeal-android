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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.feature.setup.data.models.DietTypeDto
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.buttons.SmartMealButton
import com.example.smartmeal.ui.components.buttons.SmartMealButtonColor
import com.example.smartmeal.ui.components.buttons.SmartMealButtonVariant
import com.example.smartmeal.ui.theme.BgLightGray
import com.example.smartmeal.ui.theme.PrimaryGreen

private val YellowDivider = Color(0xFFD4B800)

@Composable
fun DietScreen(
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
                text = "Рацион",
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
                    text = "Мой рацион:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SmartMealText(
                    text = "Нажмите по рациону, чтобы выбрать его или снять выбор.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (state.allDietTypes.isEmpty()) {
                    SmartMealText(
                        text = "Список рационов пока недоступен",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                } else {
                    state.allDietTypes.forEach { diet ->
                        DietRow(
                            diet = diet,
                            isChecked = state.pendingDietTypeId == diet.id,
                            onClick = { viewModel.selectPendingDiet(diet.id) }
                        )
                    }
                }

                HorizontalDivider(
                    color = YellowDivider,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                val selectedDietName = state.allDietTypes
                    .firstOrNull { it.id == state.pendingDietTypeId }
                    ?.name

                SmartMealText(
                    text = if (selectedDietName == null) {
                        "Сейчас рацион не выбран"
                    } else {
                        "Текущий выбор: $selectedDietName"
                    },
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (state.error != null) {
                    SmartMealText(
                        text = state.error!!,
                        color = Color.Red,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        SmartMealButton(
            text = if (state.isSaving) "Сохраняем..." else "Подтвердить",
            onClick = { viewModel.saveDiet() },
            variant = SmartMealButtonVariant.PRIMARY,
            color = SmartMealButtonColor.GREEN,
            enabled = !state.isSaving,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun DietRow(
    diet: DietTypeDto,
    isChecked: Boolean,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isChecked) PrimaryGreen else Color.LightGray,
            modifier = Modifier.size(22.dp)
        )
        SmartMealText(text = diet.name, fontSize = 15.sp)
    }
}
