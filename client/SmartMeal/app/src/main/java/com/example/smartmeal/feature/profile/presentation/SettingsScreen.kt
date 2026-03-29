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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.buttons.SmartMealButton
import com.example.smartmeal.ui.components.buttons.SmartMealButtonColor
import com.example.smartmeal.ui.components.buttons.SmartMealButtonVariant
import com.example.smartmeal.ui.theme.BgLightGray
import com.example.smartmeal.ui.theme.BorderGray
import com.example.smartmeal.ui.theme.HintGray

object UserSession {
    var name: String = "Admin"
    var birthDate: String = ""
    var gender: String = "" // "male" | "female" | ""
    var email: String = "test@admin.com"
}

private val AvatarYellow = Color(0xFFFFF4C2)
private val GenderSelectedBg = Color(0xFFFFF176)
private val GenderUnselectedBg = Color(0xFFEEEEEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var birthDate by remember { mutableStateOf(UserSession.birthDate) }
    var gender by remember { mutableStateOf(UserSession.gender) }
    var email by remember { mutableStateOf(UserSession.email) }
    var emailError by remember { mutableStateOf<String?>(null) }

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
                    .background(AvatarYellow)
            )
        }

        // --- Имя ---
        SmartMealText(
            text = UserSession.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        HorizontalDivider(color = BorderGray, thickness = 1.dp)

        // --- Дата рождения ---
        SettingsFieldLabel("Дата рождения")
        OutlinedTextField(
            value = birthDate,
            onValueChange = { birthDate = it },
            placeholder = { SmartMealText(text = "ДД ММ ГГГГ", color = HintGray, fontSize = 15.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp),
            shape = RoundedCornerShape(0.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        HorizontalDivider(color = BorderGray, thickness = 1.dp)

        // --- Пол ---
        SettingsFieldLabel("Пол")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GenderButton(
                text = "Мужской",
                isSelected = gender == "male",
                onClick = { gender = "male" },
                modifier = Modifier.weight(1f)
            )
            GenderButton(
                text = "Женский",
                isSelected = gender == "female",
                onClick = { gender = "female" },
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = BorderGray, thickness = 1.dp)

        // --- Email ---
        SettingsFieldLabel("ЭЛ.ПОЧТА")
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailError = validateEmail(it) },
            placeholder = { SmartMealText(text = "email@example.com", color = HintGray, fontSize = 15.sp) },
            isError = emailError != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp),
            shape = RoundedCornerShape(0.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                errorBorderColor = Color.Transparent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        if (emailError != null) {
            SmartMealText(
                text = emailError!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        HorizontalDivider(color = BorderGray, thickness = 1.dp)

        Spacer(modifier = Modifier.height(32.dp))

        // --- Кнопка Подтвердить ---
        SmartMealButton(
            text = "Подтвердить",
            onClick = {
                val error = validateEmail(email)
                if (error != null) { emailError = error; return@SmartMealButton }
                UserSession.birthDate = birthDate
                UserSession.gender = gender
                UserSession.email = email
            },
            variant = SmartMealButtonVariant.PRIMARY,
            color = SmartMealButtonColor.GREEN,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsFieldLabel(label: String) {
    SmartMealText(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.Black,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 2.dp)
    )
}

@Composable
private fun GenderButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) GenderSelectedBg else GenderUnselectedBg)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        SmartMealText(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color.Black else Color(0xFF9E9E9E)
        )
    }
}

private fun validateEmail(email: String): String? = when {
    email.isBlank() -> null
    !email.contains("@") || !email.contains(".") -> "Введите корректный email"
    else -> null
}
