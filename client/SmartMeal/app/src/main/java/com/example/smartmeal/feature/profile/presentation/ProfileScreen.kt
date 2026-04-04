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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartmeal.feature.home.data.api.UserFavoriteDto
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.cards.MealCard
import com.example.smartmeal.ui.theme.BgLightGray
import com.example.smartmeal.ui.theme.BorderGray
import com.example.smartmeal.ui.theme.HintGray
import com.example.smartmeal.ui.theme.PrimaryGreen

private val CardYellow = Color(0xFFFFF4C2)
private val LogoutRed = Color(0xFFE53935)

// Подэкраны внутри вкладки профиля
enum class ProfileSubScreen { NONE, SETTINGS, ALLERGIES, DIET, FAVORITES, ORDERS }

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onGoToProducts: () -> Unit,      // переключает BottomNav → вкладка "Продукты"
    onRecipeClick: (Int) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var subScreen by remember { mutableStateOf(ProfileSubScreen.NONE) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    when (subScreen) {
        ProfileSubScreen.SETTINGS ->
            SettingsScreen(onBack = { subScreen = ProfileSubScreen.NONE })

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
                favorites = state.favorites,
                onBack = { subScreen = ProfileSubScreen.NONE },
                onRecipeClick = onRecipeClick,
                onFavoriteClick = { viewModel.toggleFavorite(it) }
            )

        ProfileSubScreen.ORDERS ->
            com.example.smartmeal.feature.products.presentation.OrdersScreen(
                onBack = { subScreen = ProfileSubScreen.NONE },
                onGoToProducts = {
                    subScreen = ProfileSubScreen.NONE
                    onGoToProducts()
                }
            )

        ProfileSubScreen.NONE -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgLightGray)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Шапка ──────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
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
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryGreen)
                        }
                    }

                    // ── Аватар + имя + "Настройки >" ───────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clickable { subScreen = ProfileSubScreen.SETTINGS },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CardYellow)
                        )
                        Column {
                            SmartMealText(
                                text = state.userName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            SmartMealText(text = "Настройки >", fontSize = 13.sp, color = HintGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Кнопки меню ────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val allergyLabel = if (state.currentAllergyNames.isNotEmpty())
                            "Мои аллергии:\n${state.currentAllergyNames.joinToString(", ")}"
                        else "Мои аллергии"

                        ProfileMenuCard(emoji = "🍽️", label = allergyLabel) {
                            subScreen = ProfileSubScreen.ALLERGIES
                        }

                        val dietLabel = if (state.currentDietTypeName != null)
                            "Мой рацион:\n${state.currentDietTypeName}"
                        else "Мой рацион"

                        ProfileMenuCard(emoji = "🌿", label = dietLabel) {
                            subScreen = ProfileSubScreen.DIET
                        }

                        PortionStepperCard(
                            count = state.pendingPortionSize,
                            isSaving = state.isSaving,
                            onDecrement = { viewModel.decrementPortion() },
                            onIncrement = { viewModel.incrementPortion() },
                            onSave = { viewModel.savePortion() }
                        )

                        ProfileMenuCard(emoji = "⏱️", label = "Изменить время готовки") { /* TODO */ }

                        ProfileMenuCard(emoji = "🛒", label = "Заказать продукты") {
                            subScreen = ProfileSubScreen.ORDERS
                        }

                        ProfileMenuCard(emoji = "⭐", label = "Избранное") {
                            viewModel.loadFavorites()
                            subScreen = ProfileSubScreen.FAVORITES
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showLogoutDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        SmartMealText(
                            text = "Выйти",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = LogoutRed,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
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
private fun ProfileMenuCard(emoji: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardYellow)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmartMealText(text = emoji, fontSize = 20.sp)
            SmartMealText(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
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
            .clip(RoundedCornerShape(14.dp))
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
                .background(CardYellow)
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

@Composable
fun FavoritesScreen(
    favorites: List<UserFavoriteDto>,
    onBack: () -> Unit,
    onRecipeClick: (Int) -> Unit,
    onFavoriteClick: (Int) -> Unit
) {
    val recentlyRemoved = remember { mutableStateListOf<UserFavoriteDto>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLightGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CardYellow)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                SmartMealText(text = "<", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            SmartMealText(
                text = "Избранное",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (favorites.isEmpty() && recentlyRemoved.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SmartMealText(text = "У вас пока нет избранных рецептов", color = HintGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = favorites,
                    key = { it.id } // Используем id самого Favorite для ключа
                ) { favorite ->
                    MealCard(
                        title = favorite.recipe_title,
                        cookTime = "${favorite.recipe_cook_time} мин",
                        imageUrl = favorite.recipe_image_url,
                        isFavorite = true,
                        onFavoriteClick = {
                            recentlyRemoved.add(favorite)
                            onFavoriteClick(favorite.recipe)
                        },
                        modifier = Modifier
                            .animateItem()
                            .clickable { onRecipeClick(favorite.recipe) }
                    )
                }

                if (recentlyRemoved.isNotEmpty()) {
                    item(key = "recently_removed_header") {
                        Column(modifier = Modifier.animateItem()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SmartMealText(
                                text = "Недавно удаленные",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = HintGray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    items(
                        items = recentlyRemoved,
                        key = { "removed_${it.id}" }
                    ) { item ->
                        MealCard(
                            title = item.recipe_title,
                            cookTime = "${item.recipe_cook_time} мин",
                            imageUrl = item.recipe_image_url,
                            isFavorite = false,
                            onFavoriteClick = {
                                recentlyRemoved.remove(item)
                                onFavoriteClick(item.recipe)
                            },
                            modifier = Modifier
                                .animateItem()
                                .clickable { onRecipeClick(item.recipe) }
                        )
                    }
                }
            }
        }
    }
}
