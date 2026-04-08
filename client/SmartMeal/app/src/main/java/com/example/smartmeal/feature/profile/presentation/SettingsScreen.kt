package com.example.smartmeal.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.buttons.SmartMealButton
import com.example.smartmeal.ui.components.buttons.SmartMealButtonColor
import com.example.smartmeal.ui.components.buttons.SmartMealButtonVariant
import com.example.smartmeal.ui.theme.BgLightGray
import com.example.smartmeal.ui.theme.BorderGray
import com.example.smartmeal.ui.theme.HintGray
import com.example.smartmeal.ui.theme.PrimaryGreen
import java.text.SimpleDateFormat
import java.util.*

private val AvatarYellow = Color(0xFFFFF4C2)
private val GenderSelectedBg = Color(0xFFFFC107) // Gold
private val GenderUnselectedBg = Color(0xFFEEEEEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    var showDatePicker by remember { mutableStateOf(false) }
    
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayDateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ru")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLightGray)
            .verticalScroll(rememberScrollState())
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
                text = "Настройки",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // --- Аватар ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(AvatarYellow),
                contentAlignment = Alignment.Center
            ) {
                SmartMealText(
                    text = state.userName.take(1).uppercase(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Имя пользователя ---
        SettingsFieldLabel("ИМЯ ПОЛЬЗОВАТЕЛЯ")
        OutlinedTextField(
            value = state.pendingUserName,
            onValueChange = { viewModel.updatePendingUserName(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            isError = state.usernameError != null,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderGray,
                focusedBorderColor = PrimaryGreen,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                errorBorderColor = Color.Red,
                errorContainerColor = Color.White
            ),
            singleLine = true
        )
        if (state.usernameError != null) {
            SmartMealText(
                text = state.usernameError!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 24.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Email (Read-only) ---
        SettingsFieldLabel("ЭЛ.ПОЧТА")
        OutlinedTextField(
            value = state.userEmail,
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = BorderGray,
                disabledContainerColor = Color(0xFFF5F5F5), // Light gray
                disabledTextColor = Color.Gray
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- Дата рождения ---
        SettingsFieldLabel("ДАТА РОЖДЕНИЯ")
        val displayDate = remember(state.pendingBirthDate) {
            if (state.pendingBirthDate.isNullOrBlank()) "Не указана"
            else {
                try {
                    val date = dateFormatter.parse(state.pendingBirthDate!!)
                    date?.let { displayDateFormatter.format(it) } ?: "Не указана"
                } catch (e: Exception) {
                    state.pendingBirthDate ?: "Не указана"
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { showDatePicker = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            SmartMealText(
                text = displayDate,
                color = if (state.pendingBirthDate.isNullOrBlank()) HintGray else Color.Black,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Пол ---
        SettingsFieldLabel("ПОЛ")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GenderButton(
                text = "Мужской",
                isSelected = state.pendingGender == "male",
                onClick = { viewModel.updatePendingGender("male") },
                modifier = Modifier.weight(1f)
            )
            GenderButton(
                text = "Женский",
                isSelected = state.pendingGender == "female",
                onClick = { viewModel.updatePendingGender("female") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Кнопка Сохранить ---
        SmartMealButton(
            text = if (state.isSaving) "Сохранение..." else "Сохранить",
            onClick = { viewModel.savePersonalData() },
            variant = SmartMealButtonVariant.PRIMARY,
            color = SmartMealButtonColor.GREEN,
            modifier = Modifier.padding(horizontal = 20.dp),
            enabled = !state.isSaving
        )

        if (state.savedSuccess) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1500)
                viewModel.clearSavedSuccess()
            }
            SmartMealText(
                text = "Данные успешно сохранены",
                color = PrimaryGreen,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showDatePicker) {
        val russianLocale = remember { Locale("ru", "RU") }
        val configuration = LocalConfiguration.current
        val context = LocalContext.current

        val localizedConfiguration = remember(configuration) {
            Configuration(configuration).apply {
                setLocale(russianLocale)
                setLayoutDirection(russianLocale)
            }
        }
        val localizedContext = remember(context, localizedConfiguration) {
            context.createConfigurationContext(localizedConfiguration)
        }

        // Устанавливаем глобальную локаль ПЕРЕД созданием стейта
        SideEffect {
            Locale.setDefault(russianLocale)
        }

        CompositionLocalProvider(
            LocalConfiguration provides localizedConfiguration,
            LocalContext provides localizedContext
        ) {
            val datePickerState = rememberDatePickerState()

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            viewModel.updatePendingBirthDate(dateFormatter.format(date))
                        }
                        showDatePicker = false
                    }) {
                        SmartMealText("OK", color = PrimaryGreen)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        SmartMealText("Отмена", color = Color.Gray)
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    title = {
                        SmartMealText(
                            text = "Дата рождения",
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    },
                    headline = {
                        SmartMealText(
                            text = if (datePickerState.selectedDateMillis != null) {
                                displayDateFormatter.format(Date(datePickerState.selectedDateMillis!!))
                            } else {
                                "Выберите дату"
                            },
                            modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsFieldLabel(label: String) {
    SmartMealText(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)
    )
}

@Composable
private fun GenderButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) GenderSelectedBg else GenderUnselectedBg
    val contentColor = if (isSelected) Color.Black else Color.Gray
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            SmartMealText(
                text = text,
                fontSize = 15.sp,
                fontWeight = fontWeight,
                color = contentColor
            )
        }
    }
}
