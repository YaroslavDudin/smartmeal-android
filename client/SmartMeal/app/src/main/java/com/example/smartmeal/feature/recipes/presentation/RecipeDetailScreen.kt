package com.example.smartmeal.feature.recipes.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.smartmeal.feature.home.data.menu.RecipeIngredientDto
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.buttons.QuantityStepper
import com.example.smartmeal.ui.theme.Padding
import com.example.smartmeal.ui.theme.PrimaryGreen
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

val LightCream = Color(0xFFFAFAFA)
val NutritionBgColor = Color(0xFFFBE9A6)
val IngredientRowDark = Color(0xFFE8D385)
val IngredientRowLight = Color(0xFFF3E49B)
val TextGreen = Color(0xFF388E3C)
val TextGray = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: Int,
    menuItemId: Int? = null,
    portionSize: Int,
    viewModel: RecipeDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(recipeId) {
        viewModel.loadRecipe(recipeId, menuItemId, portionSize)
    }

    Scaffold(
        containerColor = LightCream,
        topBar = { 
            CustomRecipeTopBar(
                isFavorite = state.recipe?.is_favorite ?: false,
                onFavoriteClick = { viewModel.toggleFavorite() },
                onBack = onBack 
            ) 
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryGreen
                )
            } else if (state.error != null) {
                SmartMealText(
                    text = state.error ?: "Ошибка",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                state.recipe?.let { recipe ->
                    val totalWeight = recipe.total_weight ?: 0.0

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Padding.SCREEN)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(recipe.image_url)
                                .crossfade(500)
                                .build(),
                            contentDescription = recipe.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SmartMealText(
                                text = recipe.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.weight(1f)
                            )
                            SmartMealText(
                                text = formatWeightLabel(totalWeight),
                                fontSize = 20.sp,
                                color = TextGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            InfoChip(label = "${recipe.cook_time} мин")
                            Spacer(modifier = Modifier.width(8.dp))
                            InfoChip(
                                label = "${recipe.per_serving_calories.toInt()} ккал порция"
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmartMealText(
                                text = "Ингредиенты",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SmartMealText(
                                    text = "на ${state.currentServings} порц.",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                QuantityStepper(
                                    quantity = state.currentServings,
                                    onIncrease = { viewModel.changeServings(state.currentServings + 1) },
                                    onDecrease = { viewModel.changeServings(state.currentServings - 1) },
                                    minQuantity = 1,
                                    maxQuantity = 20
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        IngredientsCard(ingredients = recipe.ingredients ?: emptyList())

                        Spacer(modifier = Modifier.height(24.dp))

                        NutritionCard(
                            calories = formatNutritionValue(calculatePer100(recipe.total_calories, totalWeight)),
                            proteins = formatNutritionValue(calculatePer100(recipe.total_proteins, totalWeight)),
                            fats = formatNutritionValue(calculatePer100(recipe.total_fats, totalWeight)),
                            carbs = formatNutritionValue(calculatePer100(recipe.total_carbs, totalWeight))
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        SmartMealText(
                            text = "Пошаговый фото рецепт",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        (recipe.steps ?: emptyList()).forEach { step ->
                            StepItem(
                                number = step.step_number,
                                description = step.description,
                                imageUrl = step.image_url,
                                timeMinutes = step.timer
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CustomRecipeTopBar(
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                modifier = Modifier.size(28.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить",
                    modifier = Modifier.size(32.dp)
                )
            }
            // Используем Box + clickable(indication = null), чтобы убрать ripple эффект (серый круг)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        onClick = onFavoriteClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val scale by animateFloatAsState(
                    targetValue = if (isFavorite) 1.2f else 1f,
                    animationSpec = tween(durationMillis = 300),
                    label = "star_scale"
                )

                val rotation by animateFloatAsState(
                    targetValue = if (isFavorite) 360f else 0f,
                    animationSpec = tween(durationMillis = 400),
                    label = "star_rotation"
                )

                Crossfade(
                    targetState = isFavorite,
                    animationSpec = tween(durationMillis = 300),
                    label = "star_fade"
                ) { favorite ->
                    Icon(
                        painter = painterResource(id = if (favorite) com.example.smartmeal.R.drawable.star else com.example.smartmeal.R.drawable.badstar),
                        contentDescription = if (favorite) "Убрать из избранного" else "В избранное",
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                rotationZ = rotation
                            ),
                        tint = Color.Unspecified
                    )
                }
            }
        }
    }
}

@Composable
fun NutritionCard(calories: String, proteins: String, fats: String, carbs: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NutritionBgColor,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp)
        ) {
            SmartMealText(
                text = "на 100 г",
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NutritionItem(value = calories, label = "ккал")
                NutritionItem(value = proteins, label = "белки")
                NutritionItem(value = fats, label = "жиры")
                NutritionItem(value = carbs, label = "углеводы")
            }
        }
    }
}

@Composable
fun NutritionItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SmartMealText(
            text = value,
            fontSize = 20.sp,
            color = TextGreen,
            fontWeight = FontWeight.Bold
        )
        SmartMealText(
            text = label,
            fontSize = 12.sp,
            color = Color.Black
        )
    }
}

@Composable
fun IngredientsCard(ingredients: List<RecipeIngredientDto>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ingredients.forEachIndexed { index, ingredient ->
                val backgroundColor = if (index % 2 == 0) IngredientRowDark else IngredientRowLight
                val formattedAmount = if (ingredient.amount % 1.0 == 0.0) {
                    ingredient.amount.toInt().toString()
                } else {
                    ingredient.amount.toString()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SmartMealText(
                        text = ingredient.ingredient_name,
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    SmartMealText(
                        text = "$formattedAmount ${ingredient.unit_name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun StepItem(number: Int, description: String, imageUrl: String?, timeMinutes: Int?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SmartMealText(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = TextGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)) {
                    append("Шаг $number: ")
                }
                withStyle(style = SpanStyle(color = Color.Black, fontSize = 18.sp)) {
                    append(description)
                }
            }
        )

        formatStepTimerLabel(timeMinutes)?.let { timerLabel ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_recent_history),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextGray
                )
                Spacer(modifier = Modifier.width(4.dp))
                SmartMealText(
                    text = timerLabel,
                    fontSize = 14.sp,
                    color = TextGray
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(500)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun InfoChip(label: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF0F0F0),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        SmartMealText(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 14.sp,
            color = Color.DarkGray
        )
    }
}

@Composable
fun IngredientItem(name: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SmartMealText(text = name, fontSize = 16.sp, color = Color.Black)
        SmartMealText(text = amount, fontSize = 16.sp, color = Color.Gray)
    }
}

internal fun calculatePer100(totalValue: Double, totalWeightG: Double): Double {
    if (totalWeightG <= 0.0) return 0.0
    return (totalValue / totalWeightG) * 100.0
}

internal fun formatNutritionValue(value: Double): String {
    val formatter = DecimalFormat("0.#", DecimalFormatSymbols(Locale.US))
    return formatter.format(value)
}

internal fun formatWeightLabel(totalWeightG: Double): String {
    if (totalWeightG <= 0.0) return "—"
    val normalized = if (totalWeightG % 1.0 == 0.0) {
        totalWeightG.toInt().toString()
    } else {
        formatNutritionValue(totalWeightG)
    }
    return "$normalized г"
}

internal fun formatStepTimerLabel(timerMinutes: Int?): String? {
    val minutes = timerMinutes ?: return null
    if (minutes <= 0) return null
    return "$minutes мин"
}
