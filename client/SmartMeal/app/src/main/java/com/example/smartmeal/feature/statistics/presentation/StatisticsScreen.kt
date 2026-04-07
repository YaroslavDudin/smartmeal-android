package com.example.smartmeal.feature.statistics.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import com.example.smartmeal.feature.home.presentation.CustomPlan
import com.example.smartmeal.feature.home.presentation.MyPlanSection
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.theme.PrimaryGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticsScreen() {
    val context = LocalContext.current
    val preferences = remember { SetupPreferences(context) }
    val viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModelFactory(preferences))
    
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Кастомный план
    val planType = preferences.getPlanType()
    val planRange = preferences.getCustomPlanRange()
    val customPlan = if (planType == SetupPreferences.PLAN_TYPE_CUSTOM) {
        planRange?.let { (start, end) -> CustomPlan(Date(start), Date(end)) }
    } else {
        null
    }

    // Обновляем данные при каждом входе на экран статистики
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    
    // Пейджер создается с начальной страницей из ViewModel (индекс сегодняшнего дня)
    if (uiState.dailyStats.isNotEmpty()) {
        val pagerState = rememberPagerState(
            initialPage = uiState.selectedIndex,
            pageCount = { uiState.dailyStats.size }
        )

        // Синхронизация: если индекс изменился во ViewModel (например, при загрузке), скроллим пейджер
        LaunchedEffect(uiState.selectedIndex) {
            if (pagerState.currentPage != uiState.selectedIndex) {
                pagerState.scrollToPage(uiState.selectedIndex)
            }
        }

        // Синхронизация в обратную сторону: если пользователь листает пейджер, обновляем индекс в VM
        LaunchedEffect(pagerState.currentPage) {
            if (uiState.selectedIndex != pagerState.currentPage) {
                viewModel.setSelectedIndex(pagerState.currentPage)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SmartMealText(
                text = "Статистика",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                fontWeight = FontWeight.Bold
            )

            // Секция "Мой план" (календарик), если есть кастомный план
            if (customPlan != null) {
                MyPlanSection(
                    customPlan = customPlan,
                    selectedDate = uiState.dailyStats.getOrNull(pagerState.currentPage)?.date,
                    onDateSelectedFromPlan = { date ->
                        val index = uiState.dailyStats.indexOfFirst { 
                            val cal1 = Calendar.getInstance().apply { time = it.date }
                            val cal2 = Calendar.getInstance().apply { time = date }
                            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
                        }
                        if (index != -1) {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Заголовок навигации со стрелками (стрелки только если больше 1 дня)
            DateNavigationHeader(
                currentDate = uiState.dailyStats.getOrNull(pagerState.currentPage)?.date ?: Date(),
                showArrows = uiState.dailyStats.size > 1,
                canGoBack = pagerState.currentPage > 0,
                canGoForward = pagerState.currentPage < uiState.dailyStats.size - 1,
                onBackClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                onForwardClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                pageSpacing = 16.dp,
                verticalAlignment = Alignment.Top
            ) { page ->
                val stats = uiState.dailyStats[page]
                DailyStatsContent(stats = stats)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = PrimaryGreen)
            } else {
                SmartMealText(text = uiState.error ?: "Нет данных для отображения")
            }
        }
    }
}

@Composable
fun DateNavigationHeader(
    currentDate: Date,
    showArrows: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMMM", Locale("ru")) }
    val dateStr = dateFormatter.format(currentDate).replaceFirstChar { it.titlecase(Locale("ru")) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (showArrows) {
            IconButton(onClick = onBackClick, enabled = canGoBack) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Предыдущий день",
                    tint = if (canGoBack) PrimaryGreen else Color.Gray
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }

        SmartMealText(
            text = dateStr,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        if (showArrows) {
            IconButton(onClick = onForwardClick, enabled = canGoForward) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Следующий день",
                    tint = if (canGoForward) PrimaryGreen else Color.Gray
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
fun DailyStatsContent(stats: DailyStats) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            DailyNutritionCard(stats = stats)
        }

        item {
            SmartMealText(
                text = "Приёмы пищи",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(stats.meals, key = { it.id }) { meal ->
            MealNutritionRow(meal = meal)
        }
    }
}

@Composable
fun DailyNutritionCard(stats: DailyStats) {
    val targetCalories = 2000.0
    val calorieProgress = (stats.totalCalories / targetCalories).toFloat().coerceIn(0f, 1f)
    
    val animatedProgress by animateFloatAsState(
        targetValue = calorieProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "CalorieProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round,
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = PrimaryGreen,
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SmartMealText(
                        text = stats.totalCalories.toInt().toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SmartMealText(
                        text = "ккал",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroNutrientItem("Белки", stats.totalProteins, Color(0xFF4CAF50))
                MacroNutrientItem("Жиры", stats.totalFats, Color(0xFFFFC107))
                MacroNutrientItem("Углеводы", stats.totalCarbs, Color(0xFF2196F3))
            }
        }
    }
}

@Composable
fun MacroNutrientItem(label: String, value: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SmartMealText(
            text = "${value.toInt()}г",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        SmartMealText(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun MealNutritionRow(meal: MenuItemDto) {
    val mealTypeTitle = when(meal.meal_type) {
        "breakfast" -> "Завтрак"
        "lunch" -> "Обед"
        "dinner" -> "Ужин"
        else -> meal.meal_type.replaceFirstChar { it.titlecase() }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SmartMealText(
                    text = mealTypeTitle,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
                SmartMealText(
                    text = meal.recipe_title,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                SmartMealText(
                    text = "${meal.per_serving_calories.toInt()} ккал (1 порция)",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            SmartMealText(
                text = "${meal.cook_time} мин",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
