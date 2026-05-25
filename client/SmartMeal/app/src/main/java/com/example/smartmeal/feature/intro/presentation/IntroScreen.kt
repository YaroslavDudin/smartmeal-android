package com.example.smartmeal.feature.intro.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.theme.PrimaryGreen
import kotlinx.coroutines.launch

private val IntroBackground = Color(0xFFFAFAFA)
private val IntroAccent = Color(0xFFEF5A40)
private val IntroAccentDark = Color(0xFFC74732)
private val IntroAccentSoft = Color(0xFFFFE8E1)
private val IntroText = Color(0xFF202124)
private val IntroBody = Color(0xFF555555)
private val IntroMuted = Color(0xFF777777)
private val IntroCardBorder = Color(0xFFE9E9E9)
private val IntroChip = Color(0xFFF8F8F8)

private data class IntroPage(
    val title: String,
    val body: String,
    val mockup: IntroMockup
)

private enum class IntroMockup {
    MENU,
    PRODUCTS,
    CALORIES,
    SETTINGS
}

private val IntroPages = listOf(
    IntroPage(
        title = "План питания на каждый день",
        body = "SmartMeal собирает меню по датам, показывает завтрак, обед и ужин, а нужное блюдо можно быстро заменить.",
        mockup = IntroMockup.MENU
    ),
    IntroPage(
        title = "Продукты сразу в список",
        body = "Ингредиенты из выбранного периода собираются в покупки. Отмечайте купленное и отправляйте список в заказ.",
        mockup = IntroMockup.PRODUCTS
    ),
    IntroPage(
        title = "Калории и БЖУ под контролем",
        body = "Следите за дневной калорийностью, белками, жирами и углеводами без ручных подсчетов.",
        mockup = IntroMockup.CALORIES
    ),
    IntroPage(
        title = "Подробные настройки под вас",
        body = "Укажите цель по калориям, аллергии, избранные блюда и время готовки, чтобы план стал персональным.",
        mockup = IntroMockup.SETTINGS
    )
)

@Composable
fun IntroScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { IntroPages.size })
    val scope = rememberCoroutineScope()
    val currentPage by remember {
        derivedStateOf { IntroPages[pagerState.currentPage] }
    }
    val buttonColor by animateColorAsState(
        targetValue = if (pagerState.currentPage == IntroPages.lastIndex) IntroAccentDark else IntroAccent,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "introButtonColor"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(IntroBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val compactHeight = maxHeight < 700.dp
        val horizontalPadding = if (maxWidth < 360.dp) 18.dp else 24.dp
        val heroHeight = if (compactHeight) 386.dp else 464.dp
        val circleSize = if (compactHeight) 276.dp else 332.dp
        val phoneHeight = if (compactHeight) 284.dp else 350.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compactHeight) 48.dp else 62.dp)
            ) {
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    SmartMealText(
                        text = "Пропустить",
                        color = IntroMuted,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) { page ->
                IntroPageContent(
                    page = IntroPages[page],
                    circleSize = circleSize,
                    phoneHeight = phoneHeight,
                    compactHeight = compactHeight
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IntroPagerIndicator(
                pageCount = IntroPages.size,
                selectedPage = pagerState.currentPage,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    if (pagerState.currentPage == IntroPages.lastIndex) {
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
            ) {
                SmartMealText(
                    text = if (pagerState.currentPage == IntroPages.lastIndex) "Начать" else "Дальше",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun IntroPageContent(
    page: IntroPage,
    circleSize: Dp,
    phoneHeight: Dp,
    compactHeight: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compactHeight) 296.dp else 360.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(top = if (compactHeight) 14.dp else 22.dp)
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(IntroAccentSoft)
            )

            PhonePreview(
                mockup = page.mockup,
                modifier = Modifier
                    .height(phoneHeight)
                    .aspectRatio(0.58f)
                    .padding(top = if (compactHeight) 58.dp else 74.dp)
            )
        }

        Spacer(modifier = Modifier.height(if (compactHeight) 12.dp else 26.dp))

        SmartMealText(
            text = page.title,
            color = IntroText,
            fontSize = if (compactHeight) 24.sp else 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = if (compactHeight) 30.sp else 34.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        SmartMealText(
            text = page.body,
            color = IntroBody,
            fontSize = if (compactHeight) 17.sp else 19.sp,
            lineHeight = if (compactHeight) 25.sp else 29.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PhonePreview(
    mockup: IntroMockup,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        color = Color.Black,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 5.dp, end = 5.dp, top = 17.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(IntroBackground)
        ) {
            PhoneStatusBar()
            when (mockup) {
                IntroMockup.MENU -> MenuMockup()
                IntroMockup.PRODUCTS -> ProductsMockup()
                IntroMockup.CALORIES -> CaloriesMockup()
                IntroMockup.SETTINGS -> SettingsMockup()
            }
        }
    }
}

@Composable
private fun PhoneStatusBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(Color.White)
    ) {
        SmartMealText(
            text = "10:46",
            color = IntroText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
        )
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(4.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.75f)))
            Box(Modifier.size(width = 13.dp, height = 7.dp).clip(RoundedCornerShape(2.dp)).background(Color.Black.copy(alpha = 0.75f)))
        }
    }
}

@Composable
private fun MenuMockup() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 8.dp, end = 8.dp, top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HeroCard(
            title = "Меню",
            subtitle = "Четверг, 23 июля",
            icon = Icons.Default.ShoppingCart,
            chips = listOf("План активен", "3 блюда")
        )
        DateStrip(selected = "Чт\n23")
        MealCardMock("Завтрак", "Пшенная каша на кокосовом молоке", "25 мин")
        MealCardMock("Обед", "Пряный булгур с куриными бедрами", "45 мин")
        MealCardMock("Ужин", "Плов с говядиной и морковью", "60 мин", showButton = true)
        BottomNavMock(selected = 0)
    }
}

@Composable
private fun ProductsMockup() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 8.dp, end = 8.dp, top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HeroCard(
            title = "Продукты",
            subtitle = "Соберите покупки по выбранному периоду",
            icon = Icons.Default.ShoppingCart,
            chips = listOf("23 июл.", "Покупки не отмечены")
        )
        DateStrip(selected = "Чт\n23")
        SectionTitle("Фрукты и ягоды")
        ProductRow("Банан", "1.2 кг")
        ProductRow("Лимон", "212 г")
        ProductRow("Яблоко", "1.2 кг")
        SectionTitle("Овощи и зелень")
        OrderBarMock()
        BottomNavMock(selected = 1)
    }
}

@Composable
private fun CaloriesMockup() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 8.dp, end = 8.dp, top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HeroCard(
            title = "Статистика",
            subtitle = "Четверг, 23 июля",
            icon = Icons.Default.BarChart,
            chips = listOf("Аналитика", "Калории")
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, IntroCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CalorieRing(
                    Modifier
                        .size(110.dp)
                        .aspectRatio(1f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MacroStat("89г", "Белки", Color(0xFF12C95B))
                    MacroStat("94г", "Жиры", Color(0xFFFFB300))
                    MacroStat("226г", "Углеводы", Color(0xFF17A8F5))
                }
            }
        }
        MealTinyAnalytics()
        BottomNavMock(selected = 2)
    }
}

@Composable
private fun SettingsMockup() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 8.dp, end = 8.dp, top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsTop("Целевая калорийность")
        ToggleCard("Планировать по калориям", "Активно", selected = true)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, IntroCardBorder)
        ) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SmartMealText("Общая цель: 2000 ккал", color = IntroAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                SliderMock()
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CalorieMealBox("Завтрак", "600")
                    CalorieMealBox("Обед", "800")
                    CalorieMealBox("Ужин", "600")
                }
            }
        }
        ToggleCard("Аллергии и время готовки", "Рыба, морепродукты. Ужин: до часа", selected = true)
        BottomNavMock(selected = 3)
    }
}

@Composable
private fun HeroCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    chips: List<String>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, IntroCardBorder)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    SmartMealText(title, color = IntroText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    SmartMealText(subtitle, color = IntroMuted, fontSize = 10.sp)
                }
                Surface(shape = CircleShape, color = IntroAccent, modifier = Modifier.size(30.dp)) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(7.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { chip ->
                    SmallChip(chip)
                }
            }
        }
    }
}

@Composable
private fun DateStrip(selected: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, IntroCardBorder)
    ) {
        Column(Modifier.padding(vertical = 9.dp)) {
            SmartMealText(
                "Июль 2026",
                color = IntroMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(selected, "Пт\n24", "Сб\n25").forEachIndexed { index, label ->
                    DateCell(label = label, selected = index == 0)
                }
            }
        }
    }
}

@Composable
private fun DateCell(label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) IntroAccent else Color(0xFFFFF8F5))
            .border(1.dp, if (selected) IntroAccent else IntroAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        SmartMealText(
            label,
            color = if (selected) Color.White else IntroText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun MealCardMock(
    label: String,
    title: String,
    time: String,
    showButton: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SmartMealText(label, color = IntroText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, IntroCardBorder)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FoodThumb(Modifier.size(width = 58.dp, height = 50.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                ) {
                    SmartMealText(title, color = IntroText, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    SmartMealText(time, color = IntroText, fontSize = 9.sp)
                }
                if (showButton) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = IntroAccent,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        SmartMealText("Список", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                    }
                } else {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFBDBDBD), modifier = Modifier.padding(end = 9.dp).size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ProductRow(name: String, amount: String) {
    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(14.dp).border(1.dp, Color(0xFFDADADA), RoundedCornerShape(2.dp)))
            SmartMealText(name, color = IntroText, fontSize = 11.sp, modifier = Modifier.padding(start = 18.dp).weight(1f))
            SmartMealText(amount, color = IntroMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun OrderBarMock() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, IntroCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(13.dp).border(1.dp, Color(0xFFAAAAAA), RoundedCornerShape(2.dp)))
            SmartMealText("Все", color = IntroText, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
            Surface(shape = RoundedCornerShape(10.dp), color = IntroChip) {
                SmartMealText("Заказать", color = IntroMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp))
            }
        }
    }
}

@Composable
private fun CalorieRing(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Force a perfectly circular drawing area based on the smaller dimension
        val diameter = size.minDimension
        val strokeWidth = 5.dp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        
        // Dynamic inset to keep it airy but well-contained
        val inset = strokeWidth / 2f + 4.dp.toPx()
        val innerDiameter = diameter - inset * 2f
        val arcSize = Size(innerDiameter, innerDiameter)
        
        // Perfectly centered offset for the arcs
        val topLeft = Offset(
            (size.width - innerDiameter) / 2f,
            (size.height - innerDiameter) / 2f
        )
        val center = Offset(size.width / 2f, size.height / 2f)
        
        // 1. Soft background track (Perfect Circle)
        drawArc(
            color = Color(0xFFF0F0F0),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        
        // 2. Progress Arcs
        drawArc(
            color = Color(0xFF00C853), // Vibrant Green
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = Color(0xFFFFAB00), // Vibrant Amber
            startAngle = 5f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = Color(0xFF00B0FF), // Vibrant Blue
            startAngle = 120f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(32, 33, 36)
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 20.sp.toPx()
                isAntiAlias = true
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            }
            
            // Draw 2112 (Slightly smaller, centered)
            drawText("2112", center.x, center.y + 4.dp.toPx(), paint)
            
            // Draw "ккал"
            paint.apply {
                color = android.graphics.Color.rgb(150, 150, 150)
                textSize = 9.sp.toPx()
                letterSpacing = 0.05f
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            }
            drawText("ккал", center.x, center.y + 18.dp.toPx(), paint)
        }
    }
}

@Composable
private fun MacroStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SmartMealText(value, color = IntroText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        SmartMealText(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Box(Modifier.size(width = 42.dp, height = 4.dp).clip(RoundedCornerShape(6.dp)).background(color))
    }
}

@Composable
private fun MealTinyAnalytics() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, IntroCardBorder)
    ) {
        Column(Modifier.padding(12.dp)) {
            SmartMealText("Приемы пищи", color = IntroAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            SmartMealText("Завтрак", color = IntroAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            SmartMealText("Пшенная каша на кокосовом молоке", color = IntroText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            SmartMealText("639 ккал · Б: 11г · Ж: 31г · У: 78г", color = IntroMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun SettingsTop(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmartMealText("‹", color = IntroText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        SmartMealText(title, color = IntroText, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(18.dp))
    }
}

@Composable
private fun ToggleCard(title: String, subtitle: String, selected: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) IntroAccent.copy(alpha = 0.65f) else IntroCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                SmartMealText(title, color = if (selected) IntroAccent else IntroText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                SmartMealText(subtitle, color = IntroMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Tune,
                contentDescription = null,
                tint = if (selected) IntroAccent else Color(0xFFD0D0D0),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun SliderMock() {
    Box(Modifier.fillMaxWidth().height(35.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE0E0E0))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.42f)
                .height(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(IntroAccent)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(2.dp)
                .height(32.dp)
                .background(IntroAccent)
        )
    }
}

@Composable
private fun RowScope.CalorieMealBox(label: String, value: String) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(54.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, IntroCardBorder)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            SmartMealText(label, color = IntroMuted, fontSize = 8.sp)
            SmartMealText(value, color = IntroText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            SmartMealText("ккал", color = Color(0xFFBBBBBB), fontSize = 7.sp)
        }
    }
}

@Composable
private fun FoodThumb(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))) {
        drawRect(Color(0xFFFFEFE6))
        drawCircle(Color(0xFFE8D9C6), radius = size.minDimension * 0.36f, center = Offset(size.width * 0.52f, size.height * 0.52f))
        drawCircle(Color(0xFFE85D3F), radius = size.minDimension * 0.10f, center = Offset(size.width * 0.44f, size.height * 0.42f))
        drawCircle(Color(0xFF8BA560), radius = size.minDimension * 0.08f, center = Offset(size.width * 0.58f, size.height * 0.47f))
        drawCircle(Color(0xFFF2C078), radius = size.minDimension * 0.08f, center = Offset(size.width * 0.50f, size.height * 0.62f))
    }
}

@Composable
private fun SectionTitle(text: String) {
    SmartMealText(text, color = IntroAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SmallChip(text: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = IntroChip) {
        SmartMealText(text, color = IntroText, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
    }
}

@Composable
private fun BottomNavMock(selected: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(Color.White),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(Icons.Default.RestaurantMenu, "Меню", selected == 0)
        BottomNavItem(Icons.Default.ShoppingCart, "Продукты", selected == 1)
        BottomNavItem(Icons.Default.BarChart, "Статистика", selected == 2)
        BottomNavItem(Icons.Default.Person, "Профиль", selected == 3)
    }
}

@Composable
private fun BottomNavItem(icon: ImageVector, label: String, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = if (selected) IntroAccent else IntroMuted, modifier = Modifier.size(13.dp))
        SmartMealText(label, color = if (selected) IntroAccent else IntroMuted, fontSize = 7.sp, maxLines = 1)
    }
}

@Composable
private fun IntroPagerIndicator(
    pageCount: Int,
    selectedPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(width = if (index == selectedPage) 22.dp else 8.dp, height = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (index == selectedPage) PrimaryGreen else Color(0xFFE0E0E0))
            )
        }
    }
}
