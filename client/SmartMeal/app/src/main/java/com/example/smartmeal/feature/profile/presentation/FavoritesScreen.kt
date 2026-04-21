package com.example.smartmeal.feature.profile.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.cards.MealCard
import com.example.smartmeal.ui.theme.BgLightGray
import com.example.smartmeal.ui.theme.HintGray
import com.example.smartmeal.ui.theme.PrimaryGreen
import java.util.Locale

private val CardYellow = Color(0xFFF4F4F4)

@Composable
fun FavoritesScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onRecipeClick: (Int) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val recipesInMenu = state.recipesInMenuOnSelectedDay
    
    val recentlyRemoved = remember { mutableStateListOf<com.example.smartmeal.feature.home.data.api.UserFavoriteDto>() }
    val groupedFavorites = remember(state.favorites) { state.getGroupedFavorites() }
    
    // Динамический список категорий: вкладка исчезает, если в ней нет блюд
    val categories = remember(groupedFavorites) { 
        listOf("Все") + listOf("Завтрак", "Обед", "Ужин", "Перекус", "Напитки").filter { 
            it in groupedFavorites.keys && groupedFavorites[it]?.isNotEmpty() == true 
        }
    }
    
    var selectedCategory by remember { mutableStateOf("Все") }

    // Авто-переключение на "Все", если выбранная категория исчезла
    LaunchedEffect(categories) {
        if (selectedCategory != "Все" && !categories.contains(selectedCategory)) {
            selectedCategory = "Все"
        }
    }

    // Умная проверка наличия в меню
    fun isRecipeInMenuSlot(recipeId: Int, category: String): Boolean {
        val normalizedCategory = category.lowercase(Locale.US)
        return recipesInMenu.any { inMenu ->
            inMenu.recipeId == recipeId && (
                inMenu.mealType.lowercase(Locale.US).contains(normalizedCategory) ||
                normalizedCategory.contains(inMenu.mealType.lowercase(Locale.US)) ||
                (inMenu.mealType.lowercase(Locale.US) == "lunch" && (normalizedCategory == "обед" || normalizedCategory == "lunch")) ||
                (inMenu.mealType.lowercase(Locale.US) == "breakfast" && (normalizedCategory == "завтрак" || normalizedCategory == "breakfast")) ||
                (inMenu.mealType.lowercase(Locale.US) == "dinner" && (normalizedCategory == "ужин" || normalizedCategory == "dinner"))
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLightGray)
    ) {
        // --- Custom Top App Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            SmartMealText(
                text = "Избранное",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // --- Modern Category Selector (Filter Chips) ---
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                val itemCount = if (category == "Все") state.favorites.size else groupedFavorites[category]?.size ?: 0
                
                Surface(
                    onClick = { selectedCategory = category },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) PrimaryGreen else CardYellow,
                    shadowElevation = if (isSelected) 4.dp else 0.dp,
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmartMealText(
                            text = category,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color.Black
                        )
                        
                        AnimatedVisibility(visible = itemCount > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White.copy(alpha = 0.25f) else Color.LightGray.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                SmartMealText(
                                    text = itemCount.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Favorites List ---
        if (state.favorites.isEmpty() && recentlyRemoved.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SmartMealText(text = "❤", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    SmartMealText(text = "У вас пока нет избранных рецептов", color = HintGray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val displayData = if (selectedCategory == "Все") {
                    groupedFavorites
                } else {
                    mapOf(selectedCategory to (groupedFavorites[selectedCategory] ?: emptyList()))
                }

                displayData.forEach { (category, items) ->
                    if (items.isNotEmpty()) {
                        if (selectedCategory == "Все") {
                            item(key = "header_$category") {
                                SmartMealText(
                                    text = category,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                        }

                        items(
                            items = items,
                            key = { it.id }
                        ) { favorite ->
                            MealCard(
                                title = favorite.recipe_title,
                                cookTime = "${favorite.recipe_cook_time} мин",
                                imageUrl = favorite.recipe_image_url,
                                isFavorite = true,
                                onFavoriteClick = {
                                    recentlyRemoved.add(favorite)
                                    viewModel.toggleFavorite(favorite.recipe, favorite.meal_type_name)
                                },
                                isInMenu = isRecipeInMenuSlot(favorite.recipe, category),
                                onPlusClick = { viewModel.addToMenu(favorite.recipe, category) },
                                modifier = Modifier
                                    .animateItem()
                                    .clickable { onRecipeClick(favorite.recipe) }
                            )
                        }
                    }
                }

                // Недавно удаленные (только во вкладке "Все")
                if (selectedCategory == "Все" && recentlyRemoved.isNotEmpty()) {
                    item(key = "recently_removed_header") {
                        Column(modifier = Modifier.animateItem()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            SmartMealText(
                                text = "Недавно удаленные",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = HintGray,
                                modifier = Modifier.padding(bottom = 8.dp)
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
                                viewModel.toggleFavorite(item.recipe, item.meal_type_name)
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
