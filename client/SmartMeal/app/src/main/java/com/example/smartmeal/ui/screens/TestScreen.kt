package com.example.smartmeal.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.smartmeal.ui.components.buttons.*
import com.example.smartmeal.ui.components.feedback.*
import com.example.smartmeal.ui.components.chips_filters.*
import com.example.smartmeal.ui.theme.SmartMealTheme

@Composable
fun TestScreen() {
    val context = LocalContext.current

    // Состояния для демонстрации интерактивности
    var selectedChip by remember { mutableStateOf(false) }
    var selectedNoGluten by remember { mutableStateOf(false) }
    var selectedVegan by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf(3) }
    var isFavorite by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Функция показа тоста с информацией о нажатии
    fun showButtonTap(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок
        item {
            Text(
                text = "🎨 Тестирование компонентов",
                style = MaterialTheme.typography.headlineMedium
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // 1. SmartMealButton - все вариации
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1️⃣ SmartMealButton",
                        style = MaterialTheme.typography.titleLarge
                    )

                    // Primary Green
                    SmartMealButton(
                        text = "Primary Green (Сгенерировать)",
                        onClick = { showButtonTap("Нажата Primary Green кнопка") },
                        variant = SmartMealButtonVariant.PRIMARY,
                        color = SmartMealButtonColor.GREEN
                    )

                    // Primary Orange
                    SmartMealButton(
                        text = "Primary Orange (Заказать)",
                        onClick = { showButtonTap("Нажата Primary Orange кнопка") },
                        variant = SmartMealButtonVariant.PRIMARY,
                        color = SmartMealButtonColor.ORANGE
                    )

                    // Secondary
                    SmartMealButton(
                        text = "Secondary (Прозрачная)",
                        onClick = { showButtonTap("Нажата Secondary кнопка") },
                        variant = SmartMealButtonVariant.SECONDARY,
                        color = SmartMealButtonColor.GREEN
                    )

                    // Outlined
                    SmartMealButton(
                        text = "Outlined (С обводкой)",
                        onClick = { showButtonTap("Нажата Outlined кнопка") },
                        variant = SmartMealButtonVariant.OUTLINED,
                        color = SmartMealButtonColor.ORANGE
                    )

                    // Disabled state
                    SmartMealButton(
                        text = "Disabled кнопка",
                        onClick = { showButtonTap("Эта кнопка не должна нажиматься") },
                        enabled = false
                    )
                }
            }
        }

        // 2. CircleIconButton
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "2️⃣ CircleIconButton",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Избранное (toggle)
                        CircleIconButton(
                            iconType = CircleIconType.FAVORITE,
                            onClick = {
                                isFavorite = !isFavorite
                                showButtonTap("Избранное: ${if (isFavorite) "добавлено" else "удалено"}")
                            },
                            isSelected = isFavorite
                        )

                        // Заменить
                        CircleIconButton(
                            iconType = CircleIconType.REPLACE,
                            onClick = { showButtonTap("Заменить ингредиент") }
                        )

                        // Назад
                        CircleIconButton(
                            iconType = CircleIconType.BACK,
                            onClick = { showButtonTap("Навигация назад") }
                        )
                    }

                    Text(
                        text = "❤️ - toggle избранного (сейчас ${if (isFavorite) "в избранном" else "не в избранном"})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // 3. FilterChip
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3️⃣ FilterChip",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            label = "Кето",
                            isSelected = selectedChip,
                            onClick = {
                                selectedChip = !selectedChip
                                showButtonTap("Фильтр Кето: ${if (selectedChip) "выбран" else "снят"}")
                            }
                        )

                        FilterChip(
                            label = "Веган",
                            isSelected = selectedVegan,

                            onClick = {
                                selectedVegan = !selectedVegan
                                showButtonTap("Фильтр Веган: ${if (selectedVegan) "выбран" else "снят"}") }
                        )

                        FilterChip(
                            label = "Без глютена",
                            isSelected = selectedNoGluten,
                            onClick = {
                                selectedNoGluten = !selectedNoGluten
                                showButtonTap("Фильтр Без глютена: ${if (selectedNoGluten) "выбран" else "снят"}") }
                        )
                    }
                }
            }
        }

        // 4. QuantityStepper
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "4️⃣ QuantityStepper",
                        style = MaterialTheme.typography.titleLarge
                    )

                    QuantityStepper(
                        quantity = quantity,
                        onIncrease = {
                            quantity++
                            showButtonTap("Порций: $quantity")
                        },
                        onDecrease = {
                            quantity--
                            showButtonTap("Порций: $quantity")
                        },
                        minQuantity = 1,
                        maxQuantity = 5
                    )

                    Text(
                        text = "Текущее значение: $quantity порций",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 5. ShimmerEffect
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "5️⃣ ShimmerEffect",
                        style = MaterialTheme.typography.titleLarge
                    )

                    // Кнопка для переключения загрузки
                    SmartMealButton(
                        text = if (isLoading) "Показать контент" else "Показать загрузку",
                        onClick = { isLoading = !isLoading },
                        variant = SmartMealButtonVariant.SECONDARY
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Демонстрация скелетона
                    if (isLoading) {
                        RecipeCardShimmer()
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Загруженный контент",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Здесь могли быть ваши рецепты",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Отступ в конце
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TestScreenPreview() {
    SmartMealTheme {
        TestScreen()
    }
}