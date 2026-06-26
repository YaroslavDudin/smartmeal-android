package com.example.smartmeal.feature.economics.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartmeal.feature.economics.data.local.EconomicsPreferences
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.theme.SmartMealBackground
import com.example.smartmeal.ui.theme.SmartMealCardBorder
import com.example.smartmeal.ui.theme.SmartMealGreen
import com.example.smartmeal.ui.theme.SmartMealOrange
import com.example.smartmeal.ui.theme.SmartMealSurfaceSoft
import com.example.smartmeal.ui.theme.SmartMealTextMuted
import com.example.smartmeal.ui.theme.SmartMealTextSecondary
import com.example.smartmeal.ui.theme.SmartMealTomato
import com.example.smartmeal.ui.theme.SmartMealTomatoDark
import com.example.smartmeal.ui.theme.SmartMealTomatoSoft
import com.example.smartmeal.ui.theme.TextBlack
import kotlinx.coroutines.delay

// ─── Design tokens ─────────────────────────────────────────────────────────────

private val EcoSurface        = Color.White
private val EcoBorder         = SmartMealCardBorder
private val EcoMuted          = SmartMealTextMuted
private val EcoSecondary      = SmartMealTextSecondary
private val EcoAccent         = SmartMealTomato
private val EcoAccentDark     = SmartMealTomatoDark
private val EcoAccentSoft     = SmartMealTomatoSoft
private val EcoSuccess        = SmartMealGreen
private val EcoWarning        = SmartMealOrange
private val EcoBackground     = SmartMealBackground

private val BarGradientStart  = SmartMealTomato
private val BarGradientEnd    = SmartMealOrange

// ─── Public entry point ────────────────────────────────────────────────────────

/**
 * Экран «Экономика» — бюджет на день + статистика расходов.
 *
 * TODO: Подключить реальный API (Яндекс.Лавка, Сбермаркет, Wildberries и др.)
 *       Текущие данные — шаблонные mock-значения на основе дневного бюджета.
 */
@Composable
fun EconomicsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs   = remember { EconomicsPreferences(context) }
    val vm: EconomicsViewModel = viewModel(
        factory = EconomicsViewModelFactory(prefs)
    )
    val state by vm.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBackground),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 12.dp, bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Шапка ──────────────────────────────────────────────────────────────
        item {
            EcoTopBar(onBack = onBack)
        }

        // ── Бюджет на день ─────────────────────────────────────────────────────
        item {
            BudgetSetupCard(
                budgetInput  = state.budgetInput,
                isSaved      = state.isBudgetSaved,
                onInputChange = vm::onBudgetInputChange,
                onSave       = vm::saveBudget
            )
        }

        // ── Сегодня ────────────────────────────────────────────────────────────
        item {
            TodaySummaryCard(
                spent  = state.todaySpent,
                budget = state.dailyBudget
            )
        }

        // ── Выбор периода ──────────────────────────────────────────────────────
        item {
            EcoPeriodSelector(
                selected  = state.period,
                onSelect  = vm::selectPeriod
            )
        }

        // ── Гистограмма ────────────────────────────────────────────────────────
        item {
            SpendingBarChartCard(
                bars         = state.bars,
                dailyBudget  = state.dailyBudget,
                period       = state.period
            )
        }

        // ── Категории расходов ─────────────────────────────────────────────────
        item {
            CategoryBreakdownCard(categories = state.categories)
        }
    }
}

// ─── Top bar ──────────────────────────────────────────────────────────────────

@Composable
private fun EcoTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = TextBlack
            )
        }

        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = null,
            tint = EcoAccent,
            modifier = Modifier.size(22.dp)
        )

        SmartMealText(
            text = "Экономика",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextBlack
        )
    }
}

// ─── Budget Setup Card ────────────────────────────────────────────────────────

@Composable
private fun BudgetSetupCard(
    budgetInput:   String,
    isSaved:       Boolean,
    onInputChange: (String) -> Unit,
    onSave:        () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current

    // Показываем «галочку-успеха» на 2 секунды после сохранения
    var showSuccess by remember { mutableStateOf(false) }
    LaunchedEffect(isSaved) {
        if (isSaved) {
            showSuccess = true
            delay(2000)
            showSuccess = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = EcoSurface,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, EcoBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Заголовок
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EcoAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    SmartMealText(text = "₽", fontSize = 16.sp, color = EcoAccent, fontWeight = FontWeight.Bold)
                }
                SmartMealText(
                    text = "Бюджет на день",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack
                )
            }

            // Поле ввода + кнопка
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { v -> onInputChange(v.filter { it.isDigit() || it == '.' }) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = {
                        SmartMealText("500", color = EcoMuted)
                    },
                    prefix = {
                        SmartMealText("₽ ", color = EcoAccent, fontWeight = FontWeight.SemiBold)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction    = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onSave()
                            keyboard?.hide()
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = EcoAccent,
                        unfocusedBorderColor = EcoBorder,
                        focusedLabelColor    = EcoAccent,
                        cursorColor          = EcoAccent
                    )
                )

                // Кнопка «Сохранить» / «Успех»
                AnimatedVisibility(
                    visible = !showSuccess,
                    enter = fadeIn() + scaleIn(),
                    exit  = fadeOut() + scaleOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = EcoAccent),
                                onClick = {
                                    onSave()
                                    keyboard?.hide()
                                }
                            ),
                        shape = RoundedCornerShape(14.dp),
                        color = EcoAccent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Сохранить",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showSuccess,
                    enter = fadeIn() + scaleIn(initialScale = 0.7f),
                    exit  = fadeOut() + scaleOut()
                ) {
                    Box(
                        modifier = Modifier.size(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Сохранено",
                            tint = EcoSuccess,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            SmartMealText(
                text = "💡 В будущем подключим Яндекс.Лавку и Сбермаркет для автоматического учёта",
                fontSize = 11.sp,
                color = EcoMuted,
                lineHeight = 15.sp
            )
        }
    }
}

// ─── Today Summary Card ────────────────────────────────────────────────────────

@Composable
private fun TodaySummaryCard(
    spent:  Float,
    budget: Float
) {
    val ratio     = if (budget > 0f) (spent / budget).coerceIn(0f, 1f) else 0f
    val overBudget = spent > budget
    val barColor  = if (overBudget) EcoAccent else EcoSuccess

    // Анимированный прогресс
    val animRatio by animateFloatAsState(
        targetValue    = ratio,
        animationSpec  = tween(800, easing = FastOutSlowInEasing),
        label          = "todayProgress"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = EcoSurface,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, EcoBorder)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color.White, EcoAccentSoft.copy(alpha = 0.35f))
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SmartMealText(
                            text = "Сегодня",
                            fontSize = 13.sp,
                            color = EcoSecondary
                        )
                        SmartMealText(
                            text = "₽ ${spent.toInt()}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (overBudget) EcoAccent else TextBlack
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SmartMealText(
                            text = "Бюджет",
                            fontSize = 13.sp,
                            color = EcoSecondary
                        )
                        SmartMealText(
                            text = "₽ ${budget.toInt()}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EcoMuted
                        )
                    }
                }

                // Прогресс-бар
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { animRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = barColor,
                        trackColor = EcoBorder
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val remaining = budget - spent
                        SmartMealText(
                            text = if (overBudget) "Перерасход ₽ ${(-remaining).toInt()}" else "Остаток ₽ ${remaining.toInt()}",
                            fontSize = 11.sp,
                            color = if (overBudget) EcoAccent else EcoSuccess
                        )
                        SmartMealText(
                            text = "${(ratio * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = EcoMuted
                        )
                    }
                }
            }
        }
    }
}

// ─── Period Selector ───────────────────────────────────────────────────────────

@Composable
private fun EcoPeriodSelector(
    selected: EconomicsPeriod,
    onSelect:  (EconomicsPeriod) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = EcoSurface,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, EcoBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EconomicsPeriod.entries.forEach { period ->
                val isActive = period == selected
                val bgColor by animateColorAsState(
                    targetValue   = if (isActive) EcoAccent else Color.Transparent,
                    animationSpec = tween(250),
                    label         = "periodBg_${period.name}"
                )
                val textColor by animateColorAsState(
                    targetValue   = if (isActive) Color.White else EcoSecondary,
                    animationSpec = tween(250),
                    label         = "periodText_${period.name}"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(bgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = EcoAccent),
                            onClick = { onSelect(period) }
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SmartMealText(
                        text       = period.label,
                        fontSize   = 13.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color      = textColor
                    )
                }
            }
        }
    }
}

// ─── Spending Bar Chart Card ───────────────────────────────────────────────────

/**
 * Кастомная гистограмма расходов, нарисованная через Canvas.
 * Бар окрашен gradient Tomato→Orange. Бюджет-лимит — пунктирная линия.
 */
@Composable
private fun SpendingBarChartCard(
    bars:        List<SpendingBar>,
    dailyBudget: Float,
    period:      EconomicsPeriod
) {
    // Для периода WEEK/MONTH лимит = dailyBudget * daysInBar, для YEAR = dailyBudget * 30
    val limitPerBar = when (period) {
        EconomicsPeriod.WEEK  -> dailyBudget
        EconomicsPeriod.MONTH -> dailyBudget * 7f
        EconomicsPeriod.YEAR  -> dailyBudget * 30f
    }

    val maxAmount = (bars.maxOfOrNull { it.amount } ?: 1f).coerceAtLeast(limitPerBar)

    // Анимация роста баров при появлении
    var animate by remember { mutableStateOf(false) }
    LaunchedEffect(bars) {
        animate = false
        delay(80)
        animate = true
    }
    val animProgress by animateFloatAsState(
        targetValue   = if (animate) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label         = "barChartProgress"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = EcoSurface,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, EcoBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EcoAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = EcoAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    SmartMealText(
                        text = "Расходы",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextBlack
                    )
                    SmartMealText(
                        text = "TODO: данные из Яндекс / Сбер API",
                        fontSize = 10.sp,
                        color = EcoMuted
                    )
                }
            }

            // График
            if (bars.isNotEmpty()) {
                val barColor1 = BarGradientStart
                val barColor2 = BarGradientEnd
                val limitColor = EcoAccent.copy(alpha = 0.45f)
                val trackColor = EcoBorder.copy(alpha = 0.5f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    drawBarChart(
                        bars        = bars,
                        maxAmount   = maxAmount,
                        limitAmount = limitPerBar,
                        animProg    = animProgress,
                        barColor1   = barColor1,
                        barColor2   = barColor2,
                        limitColor  = limitColor,
                        trackColor  = trackColor
                    )
                }

                // Подписи по оси X
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    bars.forEach { bar ->
                        SmartMealText(
                            text     = bar.label,
                            fontSize = 9.sp,
                            color    = EcoMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Легенда
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LegendDot(color = EcoAccent,  label = "Расходы")
                    LegendDot(color = EcoAccent.copy(alpha = 0.45f), label = "Лимит")
                }
            }
        }
    }
}

/** Рисует бар-чарт через DrawScope (вызывается внутри Canvas). */
private fun DrawScope.drawBarChart(
    bars:        List<SpendingBar>,
    maxAmount:   Float,
    limitAmount: Float,
    animProg:    Float,
    barColor1:   Color,
    barColor2:   Color,
    limitColor:  Color,
    trackColor:  Color
) {
    val totalWidth  = size.width
    val totalHeight = size.height
    val barCount    = bars.size
    if (barCount == 0) return

    val gap         = totalWidth * 0.012f
    val barWidth    = (totalWidth - gap * (barCount + 1)) / barCount
    val trackRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

    // Пунктирная линия лимита
    val limitY = totalHeight * (1f - (limitAmount / maxAmount).coerceIn(0f, 1f))
    val dashLen = 12f
    val gapLen  = 8f
    var x = 0f
    while (x < totalWidth) {
        drawLine(
            color       = limitColor,
            start       = Offset(x, limitY),
            end         = Offset((x + dashLen).coerceAtMost(totalWidth), limitY),
            strokeWidth = 2f,
            cap         = StrokeCap.Round
        )
        x += dashLen + gapLen
    }

    // Бары
    bars.forEachIndexed { i, bar ->
        val left      = gap + i * (barWidth + gap)
        val barRatio  = ((bar.amount / maxAmount).coerceIn(0f, 1f)) * animProg
        val barHeight = (totalHeight * barRatio).coerceAtLeast(4f)
        val top       = totalHeight - barHeight

        // Трек (серый фон)
        drawRoundRect(
            color        = trackColor,
            topLeft      = Offset(left, 0f),
            size         = Size(barWidth, totalHeight),
            cornerRadius = trackRadius
        )

        // Цветной бар
        drawRoundRect(
            brush        = Brush.linearGradient(
                colors = listOf(barColor1, barColor2),
                start  = Offset(left, top),
                end    = Offset(left, totalHeight)
            ),
            topLeft      = Offset(left, top),
            size         = Size(barWidth, barHeight),
            cornerRadius = trackRadius
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        SmartMealText(text = label, fontSize = 10.sp, color = EcoMuted)
    }
}

// ─── Category Breakdown Card ───────────────────────────────────────────────────

@Composable
private fun CategoryBreakdownCard(categories: List<SpendingCategory>) {
    if (categories.isEmpty()) return

    val totalSpent = categories.sumOf { it.amount.toDouble() }.toFloat()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = EcoSurface,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, EcoBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Заголовок
            SmartMealText(
                text = "Статьи расходов",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextBlack
            )

            // Мини-пай индикатор (горизонтальная полоска-сегменты)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
            ) {
                categories.forEach { cat ->
                    val segColor = Color(cat.color)
                    val animW by animateFloatAsState(
                        targetValue   = cat.percent / 100f,
                        animationSpec = tween(600, easing = FastOutSlowInEasing),
                        label         = "catSeg_${cat.name}"
                    )
                    Box(
                        modifier = Modifier
                            .weight(animW.coerceAtLeast(0.001f))
                            .fillMaxSize()
                            .background(segColor)
                    )
                }
            }

            // Список категорий
            categories.forEach { cat ->
                val catColor = Color(cat.color)
                val animPercent by animateFloatAsState(
                    targetValue   = cat.percent / 100f,
                    animationSpec = tween(700, easing = FastOutSlowInEasing),
                    label         = "catBar_${cat.name}"
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            SmartMealText(
                                text = cat.emoji,
                                fontSize = 16.sp
                            )
                            SmartMealText(
                                text = cat.name,
                                fontSize = 13.sp,
                                color = TextBlack,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmartMealText(
                                text = "₽ ${cat.amount.toInt()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextBlack
                            )
                            SmartMealText(
                                text = "${cat.percent.toInt()}%",
                                fontSize = 11.sp,
                                color = EcoMuted
                            )
                        }
                    }

                    // Полоска категории
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(EcoBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animPercent)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(50))
                                .background(catColor)
                        )
                    }
                }
            }

            // Итого
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmartMealText(
                    text = "Итого за период",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoSecondary
                )
                SmartMealText(
                    text = "₽ ${totalSpent.toInt()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
            }

            SmartMealText(
                text = "⚠ Mock-данные. Подключите API чеков для реальной аналитики.",
                fontSize = 10.sp,
                color = EcoMuted,
                lineHeight = 14.sp
            )
        }
    }
}
