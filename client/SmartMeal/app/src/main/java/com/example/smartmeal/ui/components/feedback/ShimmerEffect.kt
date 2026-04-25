package com.example.smartmeal.ui.components.feedback

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Продвинутый Modifier для эффекта мерцания.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "shimmer_offset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFEBEBEB),
                Color(0xFFF5F5F5),
                Color(0xFFEBEBEB),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

/**
 * Скелетон карточки блюда.
 */
@Composable
fun MealCardSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(100.dp).fillMaxHeight().shimmerEffect())
            Column(modifier = Modifier.padding(12.dp).weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth(0.8f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).shimmerEffect())
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.width(50.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
            }
            Box(modifier = Modifier.padding(end = 16.dp).size(32.dp).clip(CircleShape).shimmerEffect())
        }
    }
}

/**
 * Скелетон одного продукта.
 */
@Composable
fun ProductItemSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Чекбокс
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.width(16.dp))
        // Название продукта
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.width(16.dp))
        // Вес/Количество
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
    }
}

/**
 * Скелетон кнопки "Мой план".
 */
@Composable
fun MyPlanButtonSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .shimmerEffect()
    )
}

/**
 * Скелетон всего домашнего экрана.
 */
@Composable
fun HomeScreenSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Добавляем скелетон кнопки "Мой план" в начало, если он нужен
        MyPlanButtonSkeleton()
        
        repeat(4) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(100.dp).height(20.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                MealCardSkeleton()
            }
        }
    }
}

/**
 * Скелетон всего экрана продуктов.
 */
@Composable
fun ProductListSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Заголовок страницы (Меню)
        Box(modifier = Modifier.align(Alignment.CenterHorizontally).width(120.dp).height(32.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        Spacer(modifier = Modifier.height(24.dp))
        
        // Скелетон кнопки "Мой план" (если он нужен, добавим для консистентности)
        MyPlanButtonSkeleton()
        Spacer(modifier = Modifier.height(16.dp))

        // Селектор дат
        Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
        Spacer(modifier = Modifier.height(24.dp))

        repeat(3) {
            // Заголовок категории
            Box(modifier = Modifier.width(150.dp).height(24.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            Spacer(modifier = Modifier.height(12.dp))
            // Список продуктов в категории
            repeat(3) {
                ProductItemSkeleton()
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Скелетон для экрана статистики.
 */
@Composable
fun StatisticsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Заголовок
        Box(modifier = Modifier.width(180.dp).height(28.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        
        // Скелетон кнопки "Мой план"
        MyPlanButtonSkeleton()

        // Большая карточка графика
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)).shimmerEffect())
        
        // Сетка карточек с деталями
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
            Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
        }
        
        // Еще одна секция
        Box(modifier = Modifier.width(140.dp).height(24.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
    }
}

