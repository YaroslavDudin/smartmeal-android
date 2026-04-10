package com.example.smartmeal.feature.statistics.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.smartmeal.ui.theme.BgLightGray
import com.example.smartmeal.ui.theme.BorderGray
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.TextBlack
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.foundation.BorderStroke

@Composable
fun StatisticsScreen() {
    val context = LocalContext.current
    val preferences = remember { SetupPreferences(context) }
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
        viewModel.refresh()
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgLightGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp)
            ) {
                SmartMealText(
                    text = "Статистика",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(vertical = 12.dp)
            ) {
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                        thickness = 1.dp,
                        color = BgLightGray
                    )
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

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                pageSpacing = 16.dp,
                verticalAlignment = Alignment.Top
            ) { page ->
                val stats = uiState.dailyStats.getOrNull(page)
                if (stats != null) {
                    val contentKey = remember(stats.meals) { stats.meals.joinToString { "${it.id}-${it.recipe}" } }
                    key(contentKey) {
                        DailyStatsContent(
                            stats = stats,
                            targetCalories = uiState.targetCalories,
                            targetProteins = uiState.targetProteins,
                            targetFats = uiState.targetFats,
                            targetCarbs = uiState.targetCarbs
                        )
                    }
                }
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
            fontWeight = FontWeight.Bold,
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
    targetCalories: Double,
    targetProteins: Double,
    targetFats: Double,
    targetCarbs: Double
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            DailyNutritionCard(
                stats = stats,
                targetCalories = targetCalories,
                targetProteins = targetProteins,
                targetFats = targetFats,
                targetCarbs = targetCarbs
            )
        }

        item {
            SmartMealText(
                text = "ПРИЁМЫ ПИЩИ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = PrimaryGreen,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        items(stats.meals, key = { it.id }) { meal ->
            MealNutritionRow(meal = meal)
        }
    }
}

@Composable
fun DailyNutritionCard(
    stats: DailyStats,
    targetCalories: Double,
    targetProteins: Double,
    targetFats: Double,
    targetCarbs: Double
) {
    val progressP = (stats.totalProteins * 4 / targetCalories).toFloat()
    val progressF = (stats.totalFats * 9 / targetCalories).toFloat()
    val progressC = (stats.totalCarbs * 4 / targetCalories).toFloat()
    
    val animatedP by animateFloatAsState(targetValue = progressP, animationSpec = tween(1500), label = "P")
    val animatedF by animateFloatAsState(targetValue = progressF, animationSpec = tween(1500), label = "F")
    val animatedC by animateFloatAsState(targetValue = progressC, animationSpec = tween(1500), label = "C")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF8F8F8)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 36.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 20.dp.toPx()
                    val gap = 2.5f
                    
                    // Яркие неоновые градиенты
                    val brushP = Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF00C853)))
                    val brushF = Brush.linearGradient(listOf(Color(0xFFFFD600), Color(0xFFFFAB00)))
                    val brushC = Brush.linearGradient(listOf(Color(0xFF00B0FF), Color(0xFF0091EA)))
                    
                    // Фоновый трек (недобор) с мягким градиентом
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color(0xFFF0F0F0), Color(0xFFFAFAFA), Color(0xFFF0F0F0))),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    val sweepP = (animatedP * 360f).coerceIn(0f, 360f)
                    val sweepF = (animatedF * 360f).coerceIn(0f, 360f - sweepP)
                    val sweepC = (animatedC * 360f).coerceIn(0f, 360f - sweepP - sweepF)

                    // Отрисовка сегментов со свечением
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SmartMealText(
                        text = stats.totalCalories.toInt().toString(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextBlack,
                        letterSpacing = (-1.5).sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmartMealText(
                            text = "из ",
                            fontSize = 20.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        SmartMealText(
                            text = "${targetCalories.toInt()}",
                            fontSize = 22.sp,
                            color = TextBlack.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    SmartMealText(
                        text = "ккал сегодня",
                        fontSize = 13.sp,
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
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

@Composable
fun MacroNutrientItem(label: String, value: Double, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        SmartMealText(
            text = "${value.toInt()}г",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 19.sp,
            color = TextBlack
        )
        SmartMealText(
            text = label,
            fontSize = 13.sp,
            color = Color.Gray,
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
fun MealNutritionRow(meal: MenuItemDto) {
    val mealTypeTitle = when(meal.meal_type) {
        "breakfast" -> "Завтрак"
        "lunch" -> "Обед"
        "dinner" -> "Ужин"
        else -> meal.meal_type.replaceFirstChar { it.titlecase() }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                SmartMealText(
                    text = "${meal.per_serving_calories.toInt()} ккал • 1 порция",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
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
