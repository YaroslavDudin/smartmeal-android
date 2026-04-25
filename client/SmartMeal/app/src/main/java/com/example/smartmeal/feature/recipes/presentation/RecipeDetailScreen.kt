package com.example.smartmeal.feature.recipes.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.SubcomposeAsyncImage
import com.example.smartmeal.ui.components.feedback.shimmerEffect
import com.example.smartmeal.R
import coil.request.ImageRequest
import com.example.smartmeal.R
import com.example.smartmeal.feature.home.data.menu.RecipeIngredientDto
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.buttons.QuantityStepper
import com.example.smartmeal.ui.theme.Padding
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.utils.ShareUtils
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
    val context = LocalContext.current

    LaunchedEffect(recipeId) {
        viewModel.loadRecipe(recipeId, menuItemId, portionSize)
    }

    Scaffold(
        containerColor = LightCream,
        topBar = { 
            CustomRecipeTopBar(
                isFavorite = state.recipe?.is_favorite ?: false,
                isInMenu = state.isInMenuOnSelectedDay,
                onFavoriteClick = { viewModel.toggleFavorite() },
                onAddClick = { viewModel.addToMenu() },
                onShareClick = {
                    state.recipe?.let { recipe ->
                        // Мы отправляем только текст с прямой ссылкой в приложение
                        // Это 100% открывает приложение у друга
                        ShareUtils.shareRecipe(context, recipe.title, recipe.id)
                    }
                },
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
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = Padding.SCREEN),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Левая колонка: Фото и КБЖУ
                            Column(
                                modifier = Modifier
                                    .weight(0.42f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 8.dp)
                            ) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(recipe.image_url)
                                        .crossfade(500)
                                        .build(),
                                    loading = {
                                        Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                                    },
                                    error = {
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(id = R.drawable.food),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    },
                                    contentDescription = recipe.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(20.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                SmartMealText(
                                    text = recipe.title,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    lineHeight = 28.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    InfoChip(label = "${recipe.cook_time} мин")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    InfoChip(label = "${recipe.per_serving_calories.toInt()} ккал")
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                SmartMealText(
                                    text = "Пищевая ценность (на 100 г)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                NutritionCard(
                                    calories = formatNutritionValue(calculatePer100(recipe.total_calories, totalWeight)),
                                    proteins = formatNutritionValue(calculatePer100(recipe.total_proteins, totalWeight)),
                                    fats = formatNutritionValue(calculatePer100(recipe.total_fats, totalWeight)),
                                    carbs = formatNutritionValue(calculatePer100(recipe.total_carbs, totalWeight))
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                SmartMealText(
                                    text = "Общий вес: ${formatWeightLabel(totalWeight)}",
                                    fontSize = 14.sp,
                                    color = TextGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Правая колонка: Ингредиенты и Шаги
                            Column(
                                modifier = Modifier
                                    .weight(0.58f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SmartMealText(
                                        text = "Ингредиенты",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SmartMealText(
                                            text = "${state.currentServings} порц.",
                                            fontSize = 13.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(end = 6.dp)
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

                                Spacer(modifier = Modifier.height(12.dp))
                                IngredientsCard(ingredients = recipe.ingredients ?: emptyList())

                                Spacer(modifier = Modifier.height(24.dp))

                                SmartMealText(
                                    text = "Приготовление",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                (recipe.steps ?: emptyList()).forEach { step ->
                                    StepItem(
                                        number = step.step_number,
                                        description = step.description,
                                        imageUrl = step.image_url,
                                        timeMinutes = step.timer
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = Padding.SCREEN)
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(recipe.image_url)
                                    .crossfade(500)
                                    .build(),
                                loading = {
                                    Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                                },
                                error = {
                                    androidx.compose.foundation.Image(
                                        painter = painterResource(id = R.drawable.food),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                },
                                contentDescription = recipe.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
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

                            SmartMealText(
                                text = "Пищевая ценность на 100 г",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            NutritionCard(
                                calories = formatNutritionValue(calculatePer100(recipe.total_calories, totalWeight)),
                                proteins = formatNutritionValue(calculatePer100(recipe.total_proteins, totalWeight)),
                                fats = formatNutritionValue(calculatePer100(recipe.total_fats, totalWeight)),
                                carbs = formatNutritionValue(calculatePer100(recipe.total_carbs, totalWeight))
                            )

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
}

@Composable
fun CustomRecipeTopBar(
    isFavorite: Boolean,
    isInMenu: Boolean,
    onFavoriteClick: () -> Unit,
    onAddClick: () -> Unit,
    onShareClick: () -> Unit,
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
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Поделиться",
                    modifier = Modifier.size(24.dp),
                    tint = Color.DarkGray
                )
            }
            
            // Кнопка "+" отображается только если рецепт в избранном
            if (isFavorite) {
                RecipePlusButton(
                    isActive = isInMenu,
                    onClick = onAddClick
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
                        painter = painterResource(id = if (favorite) R.drawable.star else R.drawable.badstar),
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
fun RecipePlusButton(
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            painter = painterResource(
                id = if (isActive) R.drawable.ic_plus_active else R.drawable.ic_plus_inactive
            ),
            contentDescription = "Add to menu",
            modifier = Modifier.size(40.dp),
            tint = Color.Unspecified
        )
    }
}

@Composable
fun NutritionCard(calories: String, proteins: String, fats: String, carbs: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NutritionBgColor.copy(alpha = 0.95f),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NutritionItem(value = calories, label = "ккал")
            NutritionDivider()
            NutritionItem(value = proteins, label = "белки")
            NutritionDivider()
            NutritionItem(value = fats, label = "жиры")
            NutritionDivider()
            NutritionItem(value = carbs, label = "угл.")
        }
    }
}

@Composable
fun NutritionDivider() {
    Box(
        modifier = Modifier
            .width(1.5.dp)
            .height(38.dp)
            .background(Color.Black.copy(alpha = 0.15f))
    )
}

@Composable
fun NutritionItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        SmartMealText(
            text = value,
            fontSize = 22.sp,
            color = Color.Black,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 24.sp
        )
        SmartMealText(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black.copy(alpha = 0.6f)
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

        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(500)
                .build(),
            loading = {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).shimmerEffect())
            },
            error = {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.food),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
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
