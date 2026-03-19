package com.example.smartmeal.feature.recipes.presentation
import com.example.smartmeal.ui.components.buttons.QuantityStepper

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.R
import com.example.smartmeal.ui.theme.Padding
import com.example.smartmeal.ui.theme.PrimaryGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: Int,
    portionSize: Int,
    viewModel: RecipeDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(recipeId) {
        viewModel.loadRecipe(recipeId, portionSize)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рецепт") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                            .padding(horizontal = Padding.SCREEN)
                    ) {
                        // Картинка
                        Image(
                            painter = painterResource(id = R.drawable.food),
                            contentDescription = recipe.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Заголовок
                        Text(
                            text = recipe.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Время и КБЖУ
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            InfoChip(label = "${recipe.cook_time} мин")
                            InfoChip(label = "${recipe.per_serving_calories.toInt()} ккал/порция")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Ингредиенты с динамическим количеством порций
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ингредиенты",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "(на ${state.currentServings} порц.)",
                                fontSize = 18.sp,
                                color = Color.Gray
                            )
                            QuantityStepper(
                                quantity = state.currentServings,
                                onIncrease = { viewModel.changeServings(state.currentServings + 1) },
                                onDecrease = { viewModel.changeServings(state.currentServings - 1) },
                                minQuantity = 1,
                                maxQuantity = 20
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        recipe.ingredients.forEach { ingredient ->
                            IngredientItem(
                                name = ingredient.ingredient_name,
                                amount = "${ingredient.amount} ${ingredient.unit_name}"
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Шаги
                        Text(
                            text = "Способ приготовления",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        recipe.steps.forEach { step ->
                            StepItem(
                                number = step.step_number,
                                description = step.description
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        Text(text = name, color = Color.Gray)
        Text(text = amount, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StepItem(number: Int, description: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$number.",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = description,
            fontSize = 16.sp,
            color = Color.Black,
            lineHeight = 22.sp
        )
    }
}
