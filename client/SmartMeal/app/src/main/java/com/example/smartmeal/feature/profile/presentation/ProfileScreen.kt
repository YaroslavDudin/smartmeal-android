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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler
import com.example.smartmeal.feature.home.data.api.UserFavoriteDto
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.cards.MealCard
import com.example.smartmeal.ui.theme.BgLightGray
import com.example.smartmeal.ui.theme.BorderGray
import com.example.smartmeal.ui.theme.HintGray
import com.example.smartmeal.ui.theme.PrimaryGreen
import java.text.SimpleDateFormat
import java.util.Locale

import coil.compose.SubcomposeAsyncImage
import com.example.smartmeal.ui.components.feedback.shimmerEffect
import androidx.compose.ui.layout.ContentScale

private val CardYellow = Color(0xFFF4F4F4)
private val LogoutRed = Color(0xFFE53935)
private val AvatarFallbackBg = Color(0xFFEEEEEE)

// Подэкраны внутри вкладки профиля
enum class ProfileSubScreen { NONE, SETTINGS, ALLERGIES, DIET, FAVORITES, COOK_TIME, CALORIES }

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onGoToProducts: (Boolean) -> Unit,      // переключает BottomNav → вкладка "Продукты"
    onRecipeClick: (Int) -> Unit = {},
    onProfileUpdatedSuccessfully: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var subScreen by remember { mutableStateOf(ProfileSubScreen.NONE) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Обработка системной кнопки "Назад": если не в корне, возвращаемся в корень профиля
    BackHandler(enabled = subScreen != ProfileSubScreen.NONE) {
        subScreen = ProfileSubScreen.NONE
    }

    when (subScreen) {
        ProfileSubScreen.SETTINGS ->
            SettingsScreen(
                viewModel = viewModel,
                onBack = { subScreen = ProfileSubScreen.NONE }
            )

        ProfileSubScreen.ALLERGIES ->
            AllergiesScreen(
                viewModel = viewModel,
                onBack = { subScreen = ProfileSubScreen.NONE }
            )

        ProfileSubScreen.DIET ->
            DietScreen(
                viewModel = viewModel,
                onBack = { subScreen = ProfileSubScreen.NONE }
            )

        ProfileSubScreen.FAVORITES ->
            FavoritesScreen(
                viewModel = viewModel,
                onBack = { subScreen = ProfileSubScreen.NONE },
                onRecipeClick = onRecipeClick
            )

        ProfileSubScreen.COOK_TIME ->
            CookTimeSettingsScreen(
                viewModel = viewModel,
                onBack = { subScreen = ProfileSubScreen.NONE }
            )

        ProfileSubScreen.CALORIES ->
            CalorieSettingsScreen(
                viewModel = viewModel,
                onBack = { subScreen = ProfileSubScreen.NONE }
            )

        ProfileSubScreen.NONE -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgLightGray)
                        .verticalScroll(rememberScrollState())
                ) {
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                    // ── Шапка ──────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = if (isLandscape) 4.dp else 4.dp)
                    ) {
                        SmartMealText(
                            text = "Профиль",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // ── Загрузка ───────────────────────────────────────────
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(if (isLandscape) 16.dp else 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryGreen)
                        }
                    }

                    // ── Аватар + имя + "Настройки >" ───────────────────────
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { subScreen = ProfileSubScreen.SETTINGS },
                        color = CardYellow
                    ) {
                        Row(
                            modifier = Modifier.padding(if (isLandscape) 12.dp else 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isLandscape) 48.dp else 64.dp)
                                    .clip(CircleShape)
                                    .background(if (state.avatarUrl.isNullOrBlank()) AvatarFallbackBg else Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!state.avatarUrl.isNullOrBlank()) {
                                    SubcomposeAsyncImage(
                                        model = state.avatarUrl,
                                        loading = {
                                            Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                                        },
                                        contentDescription = "Аватар",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    SmartMealText(
                                        text = state.userName.take(1).uppercase(),
                                        fontSize = if (isLandscape) 18.sp else 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                SmartMealText(
                                    text = state.userName,
                                    fontSize = if (isLandscape) 18.sp else 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (state.userEmail.isNotBlank()) {
                                    SmartMealText(
                                        text = state.userEmail,
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.LightGray
                            )
                        }
                    }

                    if (isLandscape) {
                        // ЛАНДШАФТ: Группируем настройки в сетку
                        ProfileSectionHeader("ПИТАНИЕ И НАСТРОЙКИ")
                        
                        val allergySub = if (state.currentAllergyNames.isNotEmpty())
                            state.currentAllergyNames.joinToString(", ")
                        else "Нет ограничений"
                        val dietSub = state.currentDietTypeName ?: "Не выбран"
                        val calorieSub = if (viewModel.isCaloriesEnabled()) 
                            "${state.totalCalories} ккал/день" 
                        else "Любая"

                        val items = listOf(
                            Triple("Мои аллергии", allergySub) { subScreen = ProfileSubScreen.ALLERGIES },
                            Triple("Мой рацион", dietSub) { subScreen = ProfileSubScreen.DIET },
                            Triple("Целевая калорийность", calorieSub) { subScreen = ProfileSubScreen.CALORIES },
                            Triple("Время готовки", "Тайминги приемов пищи") { subScreen = ProfileSubScreen.COOK_TIME },
                            Triple("Заказать продукты", "Сформировать корзину") { onGoToProducts(true) },
                            Triple("Избранное", "Ваши рецепты") { 
                                viewModel.loadFavorites()
                                subScreen = ProfileSubScreen.FAVORITES 
                            }
                        )

                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            items.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { (title, sub, action) ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            ProfileMenuCard(title = title, subtitle = sub, onClick = action)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            PortionStepperCard(
                                count = state.pendingPortionSize,
                                isSaving = state.isSaving,
                                onDecrement = { viewModel.decrementPortion() },
                                onIncrement = { viewModel.incrementPortion() },
                                onSave = { viewModel.savePortion() }
                            )
                        }
                    } else {
                        // ПОРТРЕТ: Обычный список
                        ProfileSectionHeader("ПИТАНИЕ")
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val allergySub = if (state.currentAllergyNames.isNotEmpty())
                                state.currentAllergyNames.joinToString(", ")
                            else "Нет ограничений"

                            ProfileMenuCard(title = "Мои аллергии", subtitle = allergySub) {
                                subScreen = ProfileSubScreen.ALLERGIES
                            }

                            val dietSub = state.currentDietTypeName ?: "Не выбран"
                            ProfileMenuCard(title = "Мой рацион", subtitle = dietSub) {
                                subScreen = ProfileSubScreen.DIET
                            }

                            val calorieSub = if (viewModel.isCaloriesEnabled()) 
                                "${state.totalCalories} ккал/день" 
                            else "Любая"
                            
                            ProfileMenuCard(title = "Целевая калорийность", subtitle = calorieSub) {
                                subScreen = ProfileSubScreen.CALORIES
                            }

                            PortionStepperCard(
                                count = state.pendingPortionSize,
                                isSaving = state.isSaving,
                                onDecrement = { viewModel.decrementPortion() },
                                onIncrement = { viewModel.incrementPortion() },
                                onSave = { viewModel.savePortion() }
                            )
                        }

                        ProfileSectionHeader("НАСТРОЙКИ ПЛАНА")
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProfileMenuCard(title = "Время готовки", subtitle = "Настроить тайминги приемов пищи") { 
                                subScreen = ProfileSubScreen.COOK_TIME
                            }

                            ProfileMenuCard(title = "Заказать продукты", subtitle = "Сформировать корзину в магазин") {
                                onGoToProducts(true)
                            }

                            ProfileMenuCard(title = "Избранное", subtitle = "Ваши сохраненные рецепты") {
                                viewModel.loadFavorites()
                                subScreen = ProfileSubScreen.FAVORITES
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardYellow)
                            .clickable { showLogoutDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        SmartMealText(
                            text = "Выйти из аккаунта",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LogoutRed,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }

                // --- Диалоги ---
                if (state.error != null) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { viewModel.clearError() },
                        title = { SmartMealText("Внимание", fontWeight = FontWeight.Bold) },
                        text = { SmartMealText(state.error!!) },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { viewModel.clearError() }) {
                                SmartMealText("ОК", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                if (showLogoutDialog) {
                    LogoutConfirmDialog(
                        onConfirm = {
                            showLogoutDialog = false
                            onLogout()
                            onLogoutSuccess()
                        },
                        onDismiss = { showLogoutDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionHeader(text: String) {
    SmartMealText(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun ProfileMenuCard(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardYellow)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SmartMealText(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                if (!subtitle.isNullOrBlank()) {
                    SmartMealText(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PortionStepperCard(
    count: Int,
    isSaving: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardYellow)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.7f))
                    .clickable { onDecrement() },
                contentAlignment = Alignment.Center
            ) {
                SmartMealText(text = "−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            }

            Spacer(modifier = Modifier.width(12.dp))
            SmartMealText(
                text = "$count ${personLabel(count)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
                    .clickable { onIncrement() },
                contentAlignment = Alignment.Center
            ) {
                SmartMealText(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSaving) Color.LightGray else Color(0xFF4CAF50))
                    .clickable(enabled = !isSaving) { onSave() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                SmartMealText(
                    text = if (isSaving) "..." else "✓",
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LogoutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SmartMealText(
                    text = "Вы уверены, что хотите\nвыйти из профиля?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                HorizontalDivider(color = BorderGray, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier.weight(1f).clickable { onConfirm() }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SmartMealText("Да", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LogoutRed)
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 1.dp, height = 44.dp)
                            .background(BorderGray)
                            .align(Alignment.CenterVertically)
                    )

                    Box(
                        modifier = Modifier.weight(1f).clickable { onDismiss() }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SmartMealText("Нет", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun personLabel(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "персона"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "персоны"
    else -> "персон"
}
