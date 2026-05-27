package com.example.smartmeal.feature.profile.presentation

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.feedback.shimmerEffect
import com.example.smartmeal.ui.theme.BorderGray
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.SmartMealCardBorder
import com.example.smartmeal.ui.theme.SmartMealSurfaceSoft
import com.example.smartmeal.ui.theme.SmartMealTextSecondary
import com.example.smartmeal.ui.theme.TextBlack

private val LogoutRed = Color(0xFFE53935)
private val AvatarFallbackBg = Color(0xFFEEEEEE)
private val ProfileHeroStart = Color(0xFFFFFFFF)
private val ProfileHeroEnd = Color(0xFFFFF0EB)
private val ProfileSurface = SmartMealSurfaceSoft
private val ProfileBorder = SmartMealCardBorder
private val ProfileMutedText = SmartMealTextSecondary

enum class ProfileSubScreen { NONE, SETTINGS, ALLERGIES, DIET, FAVORITES, COOK_TIME, CALORIES }

private data class ProfileActionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onGoToProducts: (Boolean) -> Unit,
    onRecipeClick: (Int) -> Unit = {},
    onProfileUpdatedSuccessfully: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var subScreen by remember { mutableStateOf(ProfileSubScreen.NONE) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = subScreen != ProfileSubScreen.NONE) {
        subScreen = ProfileSubScreen.NONE
    }

    androidx.compose.animation.AnimatedContent(
        targetState = subScreen,
        transitionSpec = {
            (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(320, delayMillis = 40)) +
                androidx.compose.animation.scaleIn(
                    initialScale = 0.985f,
                    animationSpec = androidx.compose.animation.core.tween(320, delayMillis = 40)
                )) togetherWith
                (androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(220)) +
                    androidx.compose.animation.scaleOut(
                        targetScale = 1.01f,
                        animationSpec = androidx.compose.animation.core.tween(220)
                    ))
        },
        label = "ProfileSubScreenAnimation"
    ) { targetSubScreen ->
        when (targetSubScreen) {
            ProfileSubScreen.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                onBack = { subScreen = ProfileSubScreen.NONE }
            )

            ProfileSubScreen.ALLERGIES -> AllergiesScreen(
                viewModel = viewModel,
                onBack = { subScreen = ProfileSubScreen.NONE }
            )

            ProfileSubScreen.DIET -> DietScreen(
                viewModel = viewModel,
                onBack = { subScreen = ProfileSubScreen.NONE }
            )

            ProfileSubScreen.FAVORITES -> FavoritesScreen(
                viewModel = viewModel,
                onBack = { subScreen = ProfileSubScreen.NONE },
                onRecipeClick = onRecipeClick
            )

            ProfileSubScreen.COOK_TIME -> CookTimeSettingsScreen(
                viewModel = viewModel,
                onBack = { subScreen = ProfileSubScreen.NONE }
            )

            ProfileSubScreen.CALORIES -> CalorieSettingsScreen(
                viewModel = viewModel,
                onBack = { subScreen = ProfileSubScreen.NONE }
            )

            ProfileSubScreen.NONE -> {
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val columns = if (isLandscape) 3 else 2

                val allergySummary = if (state.currentAllergyNames.isNotEmpty()) {
                    state.currentAllergyNames.joinToString(", ")
                } else {
                    "Нет ограничений"
                }
                val dietSummary = state.currentDietTypeName ?: "Не выбран"
                val calorieSummary = if (viewModel.isCaloriesEnabled()) {
                    "${state.totalCalories} ккал в день"
                } else {
                    "Гибкая цель"
                }

                val actionItems = listOf(
                    ProfileActionItem(
                        title = "Аллергии",
                        subtitle = allergySummary,
                        icon = Icons.Default.MedicalServices,
                        onClick = { subScreen = ProfileSubScreen.ALLERGIES }
                    ),
                    ProfileActionItem(
                        title = "Рацион",
                        subtitle = dietSummary,
                        icon = Icons.Default.Restaurant,
                        onClick = { subScreen = ProfileSubScreen.DIET }
                    ),
                    ProfileActionItem(
                        title = "Калории",
                        subtitle = calorieSummary,
                        icon = Icons.Default.LocalFireDepartment,
                        onClick = { subScreen = ProfileSubScreen.CALORIES }
                    ),
                    ProfileActionItem(
                        title = "Время готовки",
                        subtitle = "Тайминги приема пищи",
                        icon = Icons.Default.AccessTime,
                        onClick = { subScreen = ProfileSubScreen.COOK_TIME }
                    ),
                    ProfileActionItem(
                        title = "Список покупок",
                        subtitle = "Открыть корзину",
                        icon = Icons.Default.ShoppingCart,
                        onClick = { onGoToProducts(true) }
                    ),
                    ProfileActionItem(
                        title = "Избранное",
                        subtitle = if (state.favorites.isNotEmpty()) {
                            "${state.favorites.size} рецептов"
                        } else {
                            "Сохраненные блюда"
                        },
                        icon = Icons.Default.FavoriteBorder,
                        onClick = {
                            viewModel.loadFavorites()
                            subScreen = ProfileSubScreen.FAVORITES
                        }
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 116.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            ProfileHeroCard(
                                userName = state.userName,
                                userEmail = state.userEmail,
                                avatarUrl = state.avatarUrl,
                                favoritesCount = state.favorites.size,
                                portionSize = state.pendingPortionSize,
                                onClick = { subScreen = ProfileSubScreen.SETTINGS }
                            )
                        }

                        item { ProfileSectionLabel(text = "Быстрые настройки") }

                        items(actionItems.chunked(columns)) { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowItems.forEach { item ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ProfileActionCard(
                                            title = item.title,
                                            subtitle = item.subtitle,
                                            icon = item.icon,
                                            onClick = item.onClick
                                        )
                                    }
                                }
                                repeat(columns - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        item { ProfileSectionLabel(text = "Порции") }

                        item {
                            PortionStepperCard(
                                count = state.pendingPortionSize,
                                isSaving = state.isSaving,
                                onDecrement = { viewModel.decrementPortion() },
                                onIncrement = { viewModel.incrementPortion() },
                                onSave = { viewModel.savePortion() }
                            )
                        }

                        item {
                            ProfileLogoutCard(onClick = { showLogoutDialog = true })
                        }
                    }

                    if (state.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                shadowElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp,
                                        color = PrimaryGreen
                                    )
                                    SmartMealText(
                                        text = "Обновляем профиль",
                                        color = TextBlack,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (state.error != null) {
                        AlertDialog(
                            onDismissRequest = { viewModel.clearError() },
                            title = { SmartMealText("Внимание", fontWeight = FontWeight.Bold) },
                            text = { SmartMealText(state.error!!) },
                            confirmButton = {
                                TextButton(onClick = { viewModel.clearError() }) {
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
}

@Composable
private fun ProfileHeroCard(
    userName: String,
    userEmail: String,
    avatarUrl: String?,
    favoritesCount: Int,
    portionSize: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, ProfileBorder)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(ProfileHeroStart, ProfileHeroEnd)))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (avatarUrl.isNullOrBlank()) AvatarFallbackBg else Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = avatarUrl,
                                loading = { Box(modifier = Modifier.fillMaxSize().shimmerEffect()) },
                                contentDescription = "Аватар",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            SmartMealText(
                                text = userName.take(1).uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        SmartMealText(
                            text = "Профиль",
                            style = MaterialTheme.typography.labelLarge,
                            color = ProfileMutedText,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        SmartMealText(
                            text = userName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (userEmail.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            SmartMealText(
                                text = userEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ProfileMutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.74f)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileHeroPill(
                        title = "Порции",
                        value = "$portionSize ${personLabel(portionSize)}"
                    )
                    ProfileHeroPill(
                        title = "Избранное",
                        value = if (favoritesCount > 0) "$favoritesCount рецептов" else "Пока пусто"
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeroPill(
    title: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, ProfileBorder.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SmartMealText(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = ProfileMutedText,
                fontWeight = FontWeight.Medium
            )
            SmartMealText(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = TextBlack,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ProfileSectionLabel(text: String) {
    SmartMealText(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = ProfileMutedText,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun ProfileActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        color = Color.White,
        border = BorderStroke(1.dp, ProfileBorder),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ProfileSurface
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SmartMealText(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack
                )
                SmartMealText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ProfileMutedText
                )
            }
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ProfileBorder),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SmartMealText(
                text = "Размер порции",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextBlack
            )
            SmartMealText(
                text = "Подберите комфортное количество персон для генерации меню и списка продуктов.",
                style = MaterialTheme.typography.bodySmall,
                color = ProfileMutedText
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable(onClick = onDecrement),
                    shape = CircleShape,
                    color = ProfileSurface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SmartMealText(text = "-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = ProfileSurface
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SmartMealText(
                            text = "$count ${personLabel(count)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextBlack
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable(onClick = onIncrement),
                    shape = CircleShape,
                    color = PrimaryGreen
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SmartMealText(text = "+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSaving, onClick = onSave),
                shape = RoundedCornerShape(16.dp),
                color = if (isSaving) Color(0xFFE0E0E0) else PrimaryGreen
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SmartMealText(
                        text = if (isSaving) "Сохраняем..." else "Сохранить",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileLogoutCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFF4F4),
        border = BorderStroke(1.dp, Color(0xFFF5D0D0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SmartMealText(
                text = "Выйти из аккаунта",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = LogoutRed
            )
            SmartMealText(
                text = "Выход не затронет сохраненные настройки на устройстве.",
                style = MaterialTheme.typography.bodySmall,
                color = LogoutRed.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SmartMealText(
                    text = "Выйти из профиля?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack
                )
                Spacer(modifier = Modifier.height(10.dp))
                SmartMealText(
                    text = "Вы всегда сможете снова войти в аккаунт позже.",
                    textAlign = TextAlign.Center,
                    color = ProfileMutedText
                )
                Spacer(modifier = Modifier.height(20.dp))
                androidx.compose.material3.HorizontalDivider(color = BorderGray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onDismiss),
                        shape = RoundedCornerShape(16.dp),
                        color = PrimaryGreen
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SmartMealText("Отмена", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onConfirm),
                        shape = RoundedCornerShape(16.dp),
                        color = LogoutRed
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SmartMealText("Выйти", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
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
