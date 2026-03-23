package com.example.smartmeal.feature.recipes.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.R
import com.example.smartmeal.ui.theme.PrimaryGreen
// Удали этот импорт, если студия подчеркнет его красным, и нажми Alt+Enter на слове RecipeIngredientDto ниже, чтобы импортировать заново
import com.example.smartmeal.feature.home.data.menu.RecipeIngredientDto 

// --- Цвета для нового дизайна ---
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
    viewModel: RecipeDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(recipeId) {
        viewModel.loadRecipe(recipeId)
    }

    Scaffold(
        containerColor = LightCream, // Светлый фон экрана
        topBar = { CustomRecipeTopBar(onBack = onBack) } // Новая верхняя панель без заливки
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
                Text(
                    text = state.error ?: "Ошибка",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                state.recipe?.let { recipe ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        // Главная картинка
                        Image(
                            painter = painterResource(id = R.drawable.food),
                            contentDescription = recipe.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Заголовок и вес (вес поставил 500г как заглушку, если в модели его нет)
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = recipe.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "500 г", 
                                fontSize = 20.sp,
                                color = TextGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Карточка КБЖУ (беру данные из твоей модели, судя по автотесту Борща)
                        NutritionCard(
                            calories = recipe.total_calories.toInt().toString(),
                            proteins = recipe.total_proteins?.toString() ?: "0.0", // Если total_proteins нет, покажет 0.0
                            fats = recipe.total_fats?.toString() ?: "0.0",
                            carbs = recipe.total_carbs?.toString() ?: "0.0"
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Заголовок "Продукты"
                        Text(
                            text = "Продукты",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Полосатый блок ингредиентов (используем найденный RecipeIngredientDto)
                        IngredientsCard(ingredients = recipe.ingredients)

                        Spacer(modifier = Modifier.height(24.dp))

                        // Заголовок шагов
                        Text(
                            text = "Пошаговый фото рецепт",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Шаги
                        recipe.steps.forEach { step ->
                            StepItem(
                                number = step.step_number,
                                description = step.description,
                                time = "3 мин" // В RecipeStepDto нет времени, оставляем текст-заглушку
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
fun CustomRecipeTopBar(onBack: () -> Unit) {
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
        Row {
            IconButton(onClick = { /* TODO Действие */ }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить",
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = { /* TODO Избранное */ }) {
                Icon(
                    imageVector = Icons.Default.StarBorder,
                    contentDescription = "В избранное",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun NutritionCard(calories: String, proteins: String, fats: String, carbs: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NutritionBgColor,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
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

@Composable
fun NutritionItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            color = TextGreen,
            fontWeight = FontWeight.Medium
        )
        Text(
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
        color = Color.Transparent,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ingredients.forEachIndexed { index, ingredient ->
                val backgroundColor = if (index % 2 == 0) IngredientRowDark else IngredientRowLight
                
                // Убираем ".0", если количество целое (например, 1.0 -> 1)
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
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "${ingredient.ingredient_name} - $formattedAmount ${ingredient.unit_name}",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun StepItem(number: Int, description: String, time: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = TextGreen, fontSize = 18.sp, fontWeight = FontWeight.Normal)) {
                    append("Шаг $number: ")
                }
                withStyle(style = SpanStyle(color = Color.Black, fontSize = 18.sp)) {
                    append(description)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_recent_history), 
                contentDescription = "Время",
                modifier = Modifier.size(14.dp),
                tint = TextGray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = time,
                fontSize = 14.sp,
                color = TextGray
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Image(
            painter = painterResource(id = R.drawable.food), // Используем основную картинку
            contentDescription = "Фото шага $number",
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
    }
}
