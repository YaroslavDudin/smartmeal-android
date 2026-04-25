package com.example.smartmeal.feature.statistics.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.smartmeal.ui.theme.BgLightGray
import com.example.smartmeal.ui.theme.BorderGray
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.TextBlack
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.BorderStroke

import com.example.smartmeal.ui.components.feedback.StatisticsSkeleton

@Composable
fun StatisticsScreen(
    preferences: SetupPreferences,
    onRecipeClick: (Int, Int?) -> Unit = { _, _ -> }
) {
    val viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModelFactory(preferences))
    
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val planType = preferences.getPlanType()
    val planRange = preferences.getCustomPlanRange()
    val customPlan = if (planType == SetupPreferences.PLAN_TYPE_CUSTOM) {
        planRange?.let { (start, end) -> CustomPlan(Date(start), Date(end)) }
    } else {
        null
    }

    LaunchedEffect(Unit) {
        if (uiState.dailyStats.isEmpty()) {
            viewModel.refresh()
        }
    }
    
    if (uiState.dailyStats.isNotEmpty()) {
        val pagerState = rememberPagerState(
            initialPage = uiState.selectedIndex,
            pageCount = { uiState.dailyStats.size }
        )

        LaunchedEffect(uiState.selectedIndex) {
            if (pagerState.currentPage != uiState.selectedIndex) {
                pagerState.scrollToPage(uiState.selectedIndex)
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            if (uiState.selectedIndex != pagerState.currentPage) {
                viewModel.setSelectedIndex(pagerState.currentPage)
            }
        }

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BgLightGray),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp)
                ) {
                    SmartMealText(
                        text = "Статистика",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextBlack,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(vertical = 4.dp)
                ) {
                    if (customPlan != null) {
                        val diff = customPlan.endDate.time - customPlan.startDate.time
                        val days = (diff / (1000L * 60 * 60 * 24)) + 1
                        
                        if (days > 7) {
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
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
                                thickness = 1.dp,
                                color = BgLightGray
                            )
                        }
                    }

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
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                val pagerHeight = if (isLandscape) 400.dp else 600.dp
                
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = pagerHeight, max = 1500.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 16.dp,
                    verticalAlignment = Alignment.Top
                ) { page ->
                    val stats = uiState.dailyStats.getOrNull(page)
                    if (stats != null) {
                        DailyStatsContent(
                            stats = stats,
                            isCaloriesEnabled = uiState.isCaloriesEnabled,
                            targetCalories = uiState.targetCalories,
                            targetProteins = uiState.targetProteins,
                            targetFats = uiState.targetFats,
                            targetCarbs = uiState.targetCarbs,
                            isLandscape = isLandscape,
                            onRecipeClick = onRecipeClick
                        )
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                StatisticsSkeleton()
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SmartMealText(text = uiState.error ?: "Нет данных для отображения")
                }
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
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (showArrows) {
            IconButton(onClick = onBackClick, enabled = canGoBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Предыдущий день",
                    tint = if (canGoBack) PrimaryGreen else Color.LightGray,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }

        SmartMealText(
            text = dateStr,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = TextBlack
        )

        if (showArrows) {
            IconButton(onClick = onForwardClick, enabled = canGoForward) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Следующий день",
                    tint = if (canGoForward) PrimaryGreen else Color.LightGray,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
fun DailyStatsContent(
    stats: DailyStats,
    isCaloriesEnabled: Boolean,
    targetCalories: Double,
    targetProteins: Double,
    targetFats: Double,
    targetCarbs: Double,
    isLandscape: Boolean = false,
    onRecipeClick: (Int, Int?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DailyNutritionCard(
            stats = stats,
            isCaloriesEnabled = isCaloriesEnabled,
            targetCalories = targetCalories,
            targetProteins = targetProteins,
            targetFats = targetFats,
            targetCarbs = targetCarbs,
            isLandscape = isLandscape
        )

        SmartMealText(
            text = "ПРИЁМЫ ПИЩИ",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = PrimaryGreen,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        stats.meals.forEach { meal ->
            MealNutritionRow(meal = meal, onClick = { onRecipeClick(meal.recipe, meal.id) })
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DailyNutritionCard(
    stats: DailyStats,
    isCaloriesEnabled: Boolean,
    targetCalories: Double,
    targetProteins: Double,
    targetFats: Double,
    targetCarbs: Double,
    isLandscape: Boolean = false
) {
    val totalCalsConsumed = (stats.totalProteins * 4 + stats.totalFats * 9 + stats.totalCarbs * 4).coerceAtLeast(1.0)
    val ratioP_any = (stats.totalProteins * 4 / totalCalsConsumed).toFloat()
    val ratioF_any = (stats.totalFats * 9 / totalCalsConsumed).toFloat()
    val ratioC_any = (stats.totalCarbs * 4 / totalCalsConsumed).toFloat()

    val ratioP_target = (stats.totalProteins * 4 / targetCalories.coerceAtLeast(1.0)).toFloat()
    val ratioF_target = (stats.totalFats * 9 / targetCalories.coerceAtLeast(1.0)).toFloat()
    val ratioC_target = (stats.totalCarbs * 4 / targetCalories.coerceAtLeast(1.0)).toFloat()

    val finalRatioP = if (isCaloriesEnabled) ratioP_target else ratioP_any
    val finalRatioF = if (isCaloriesEnabled) ratioF_target else ratioF_any
    val finalRatioC = if (isCaloriesEnabled) ratioC_target else ratioC_any
    
    val animatedP by animateFloatAsState(targetValue = finalRatioP, animationSpec = tween(1500), label = "P")
    val animatedF by animateFloatAsState(targetValue = finalRatioF, animationSpec = tween(1500), label = "F")
    val animatedC by animateFloatAsState(targetValue = finalRatioC, animationSpec = tween(1500), label = "C")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF8F8F8)),
        shadowElevation = 4.dp
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier.padding(vertical = 24.dp, horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    NutritionCircle(
                        stats = stats,
                        isCaloriesEnabled = isCaloriesEnabled,
                        targetCalories = targetCalories,
                        animatedP = animatedP,
                        animatedF = animatedF,
                        animatedC = animatedC
                    )
                }

                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                    MacroNutrientItem("Белки", stats.totalProteins, Color(0xFF00C853))
                    MacroNutrientItem("Жиры", stats.totalFats, Color(0xFFFFAB00))
                    MacroNutrientItem("Углеводы", stats.totalCarbs, Color(0xFF0091EA))
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(vertical = 36.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
                    NutritionCircle(
                        stats = stats,
                        isCaloriesEnabled = isCaloriesEnabled,
                        targetCalories = targetCalories,
                        animatedP = animatedP,
                        animatedF = animatedF,
                        animatedC = animatedC
                    )
                }

                Spacer(modifier = Modifier.height(44.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MacroNutrientItem("Белки", stats.totalProteins, Color(0xFF00C853))
                    MacroNutrientItem("Жиры", stats.totalFats, Color(0xFFFFAB00))
                    MacroNutrientItem("Углеводы", stats.totalCarbs, Color(0xFF0091EA))
                }
            }
        }
    }
}

@Composable
private fun NutritionCircle(
    stats: DailyStats,
    isCaloriesEnabled: Boolean,
    targetCalories: Double,
    animatedP: Float,
    animatedF: Float,
    animatedC: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 20.dp.toPx()
        val gap = 2.5f
        
        val brushP = Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF00C853)))
        val brushF = Brush.linearGradient(listOf(Color(0xFFFFD600), Color(0xFFFFAB00)))
        val brushC = Brush.linearGradient(listOf(Color(0xFF00B0FF), Color(0xFF0091EA)))
        
        drawArc(
            brush = Brush.sweepGradient(listOf(Color(0xFFF0F0F0), Color(0xFFFAFAFA), Color(0xFFF0F0F0))),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        if (stats.totalCalories > 0) {
            val sweepP = (animatedP * 360f).coerceIn(0f, 360f)
            val sweepF = (animatedF * 360f).coerceIn(0f, 360f - sweepP)
            val sweepC = if (isCaloriesEnabled) {
                (animatedC * 360f).coerceIn(0f, 360f - sweepP - sweepF)
            } else {
                (360f - sweepP - sweepF).coerceAtLeast(0f)
            }

            if (sweepP > 1f) {
                drawArc(
                    brush = brushP,
                    startAngle = -90f + gap,
                    sweepAngle = (sweepP - gap).coerceAtLeast(0.1f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            if (sweepF > 1f) {
                drawArc(
                    brush = brushF,
                    startAngle = -90f + sweepP + gap,
                    sweepAngle = (sweepF - gap).coerceAtLeast(0.1f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            if (sweepC > 1f) {
                drawArc(
                    brush = brushC,
                    startAngle = -90f + sweepP + sweepF + gap,
                    sweepAngle = (sweepC - gap).coerceAtLeast(0.1f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SmartMealText(
            text = stats.totalCalories.toInt().toString(),
            fontSize = if (targetCalories > 0) 36.sp else 48.sp,
            fontWeight = FontWeight.Medium,
            color = TextBlack,
            letterSpacing = (-1.5).sp
        )
        if (isCaloriesEnabled) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmartMealText(text = "из ", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                SmartMealText(text = "${targetCalories.toInt()}", fontSize = 18.sp, color = TextBlack.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
            }
        }
        SmartMealText(
            text = "ккал",
            fontSize = 12.sp,
            color = Color.Gray.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MacroNutrientItem(label: String, value: Double, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        SmartMealText(
            text = "${value.toInt()}г",
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            color = TextBlack
        )
        SmartMealText(
            text = label,
            fontSize = 13.sp,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(7.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f)))
                )
        )
    }
}

@Composable
fun MealNutritionRow(meal: MenuItemDto, onClick: () -> Unit = {}) {
    val mealTypeTitle = when(meal.meal_type) {
        "breakfast" -> "Завтрак"
        "lunch" -> "Обед"
        "dinner" -> "Ужин"
        else -> meal.meal_type.replaceFirstChar { it.titlecase() }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SmartMealText(
                    text = mealTypeTitle.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                SmartMealText(
                    text = meal.recipe_title,
                    fontSize = 16.sp,
                    color = TextBlack,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmartMealText(
                        text = "${meal.per_serving_calories.toInt()} ккал",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    SmartMealText(text = " • ", fontSize = 13.sp, color = Color.Gray)
                    SmartMealText(
                        text = "Б: ${meal.per_serving_proteins.toInt()}г",
                        fontSize = 13.sp,
                        color = Color(0xFF00C853),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SmartMealText(
                        text = "Ж: ${meal.per_serving_fats.toInt()}г",
                        fontSize = 13.sp,
                        color = Color(0xFFFFAB00),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SmartMealText(
                        text = "У: ${meal.per_serving_carbs.toInt()}г",
                        fontSize = 13.sp,
                        color = Color(0xFF0091EA),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                color = BgLightGray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                SmartMealText(
                    text = "${meal.cook_time} мин",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

