package com.example.smartmeal.feature.products.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartmeal.feature.home.presentation.CustomPlan
import com.example.smartmeal.feature.home.presentation.MyPlanSection
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.selectors.DateSelector
import com.example.smartmeal.ui.components.selectors.buildDateSelectorId
import com.example.smartmeal.ui.components.selectors.buildDateSelectorItems
import com.example.smartmeal.ui.components.selectors.formatSelectedDateLabel
import com.example.smartmeal.ui.theme.LightGreenBg
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.TextBlack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Переносим модель данных сюда, так как она используется в UI
data class ProductUiModel(
    val id: String,
    val name: String,
    val amount: String,
    val category: String,
    val icon: String,
    val categoryName: String,
    val categoryIcon: String,
    val checked: Boolean = false,
    val dayOffsets: Set<Int> = emptySet(),
    val actualDates: Set<String> = emptySet(),
    val sourceIds: Set<String> = setOf(id)
)

@Composable
fun ProductListScreen(
    products: List<ProductUiModel>,
    selectedDate: Date?,
    selectedStartDateKey: String?,
    selectedEndDateKey: String?,
    dateRangeText: String,
    onDateSelected: (String) -> Unit,
    onProductChecked: (Collection<String>, Boolean) -> Unit,
    onCheckAll: (Collection<String>, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    customPlan: CustomPlan? = null
) {
    val availableDates = remember(products, customPlan) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val rawDateStrings = products
            .flatMap { it.actualDates }
            .distinct()
            .sorted()
            
        val filteredStrings = if (customPlan != null) {
            val startStr = formatter.format(customPlan.startDate)
            val endStr = formatter.format(customPlan.endDate)
            rawDateStrings.filter { it in startStr..endStr }
        } else {
            rawDateStrings
        }
        
        filteredStrings.mapNotNull { parseApiDate(it) }
    }
    
    val dateSelectorItems = remember(availableDates) { buildDateSelectorItems(availableDates) }
    
    val filteredProducts = remember(products, selectedStartDateKey, selectedEndDateKey) {
        filterProductsByDateRange(products, selectedStartDateKey, selectedEndDateKey)
    }
    
    val aggregatedProducts = remember(filteredProducts) {
        aggregateProductsForDisplay(filteredProducts)
    }
    
    val monthYearLabel = remember(selectedDate, availableDates, selectedStartDateKey, selectedEndDateKey) {
        val startDate = selectedStartDateKey?.let { parseApiDate(it) }
        val endDate = selectedEndDateKey?.let { parseApiDate(it) }
        when {
            startDate != null && endDate != null -> formatMonthYearRangeForSelector(startDate, endDate)
            startDate != null -> formatMonthYearForSelector(startDate)
            else -> {
                val anchorDate = selectedDate ?: availableDates.firstOrNull()
                anchorDate?.let { formatMonthYearForSelector(it) }.orEmpty()
            }
        }
    }
    
    val allVisibleProductIds = remember(filteredProducts) {
        filteredProducts.flatMap { it.sourceIds }.distinct()
    }
    
    val allVisibleChecked = filteredProducts.isNotEmpty() && filteredProducts.all { it.checked }
    val listState = rememberLazyListState()
    val hasSingleAvailableDate = availableDates.size == 1
    
    val contentState: ProductContentState = when {
        isLoading -> ProductContentState.Loading
        !errorMessage.isNullOrBlank() -> ProductContentState.Error
        aggregatedProducts.isEmpty() -> ProductContentState.Empty
        else -> ProductContentState.List
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 4.dp)
    ) {
        SmartMealText(
            text = "Список продуктов",
            style = MaterialTheme.typography.titleLarge,
            color = TextBlack,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 12.dp)
                .padding(top = 16.dp, bottom = 8.dp)
                .testTag("title")
        )

        if (monthYearLabel.isNotBlank() && !hasSingleAvailableDate) {
            SmartMealText(
                text = monthYearLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
                    .testTag("products_month_year")
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("products_date_selector")
        ) {
            if (hasSingleAvailableDate) {
                SmartMealText(
                    text = formatSelectedDateLabel(availableDates.first()),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextBlack,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 8.dp, bottom = 14.dp)
                        .testTag("products_selected_date_summary")
                )
            } else {
                DateSelector(
                    items = dateSelectorItems,
                    selectedStartId = selectedStartDateKey,
                    selectedEndId = selectedEndDateKey,
                    onItemClick = onDateSelected
                )
            }
        }

        if (customPlan != null) {
            val apiFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            MyPlanSection(
                customPlan = customPlan,
                selectedDate = selectedStartDateKey?.let { parseApiDate(it) },
                isRangeSelection = true, 
                onDateSelectedFromPlan = { /* Не используется */ },
                onRangeSelected = { start, end ->
                    onDateSelected(apiFormatter.format(start))
                    onDateSelected(apiFormatter.format(end))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp)
                .testTag("checkAllRow"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            SmartMealText(
                text = "Выбрать всё",
                style = MaterialTheme.typography.bodyMedium,
                color = TextBlack,
                modifier = Modifier.padding(end = 8.dp)
            )
            Checkbox(
                checked = allVisibleChecked,
                onCheckedChange = { onCheckAll(allVisibleProductIds, !allVisibleChecked) },
                modifier = Modifier.testTag("checkAllButton")
            )
        }

        AnimatedContent<ProductContentState>(
            targetState = contentState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.985f)) togetherWith
                    (fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.985f))
            },
            label = "ProductContentState"
        ) { state ->
            when (state) {
                ProductContentState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryGreen)
                    }
                }
                ProductContentState.Error -> {
                    SmartMealText(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp)
                    )
                }
                ProductContentState.Empty -> {
                    SmartMealText(
                        text = "Список продуктов пуст",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp)
                    )
                }
                ProductContentState.List -> {
                    ProductCategoryList(
                        aggregatedProducts = aggregatedProducts,
                        listState = listState,
                        onProductChecked = onProductChecked
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCategoryList(
    aggregatedProducts: List<ProductUiModel>,
    listState: LazyListState,
    onProductChecked: (Collection<String>, Boolean) -> Unit
) {
    val categoryOrder = listOf(
        "Фрукты и ягоды", "Овощи и фрукты", "Овощи, зелень и грибы",
        "Мясо и мясная продукция", "Рыба и морепродукты", "Мясо и рыба",
        "Молочные продукты и яйца", "Молочные продукты", "Бобовые",
        "Консервированные продукты", "Пасты", "Бакалея и молочные продукты",
        "Мука и мучные изделия", "Приправы и специи", "Специи", "Соусы",
        "Орехи и семена", "Добавки для приготовления блюд", "Масла",
        "Напитки", "Зерновые", "Сладости", "Разное", "Покупки"
    )

    val regularCategories: List<Pair<String, List<ProductUiModel>>> = aggregatedProducts
        .filterNot { it.checked }
        .groupBy { it.categoryName }
        .toList()
        .sortedBy { (categoryName, _) ->
            categoryOrder.indexOf(categoryName).let { if (it == -1) Int.MAX_VALUE else it }
        }

    val purchasedCategories: List<Pair<String, List<ProductUiModel>>> = aggregatedProducts
        .filter { it.checked }
        .groupBy { it.categoryName }
        .toList()
        .sortedBy { (categoryName, _) ->
            categoryOrder.indexOf(categoryName).let { if (it == -1) Int.MAX_VALUE else it }
        }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        regularCategories.forEachIndexed { index, entry ->
            val categoryName = entry.first
            val productsInCategory = entry.second
            item {
                ProductCategoryHeader(
                    title = categoryName,
                    icon = productsInCategory.firstOrNull()?.categoryIcon.orEmpty(),
                    modifier = Modifier.padding(top = if (index == 0) 6.dp else 14.dp, bottom = 6.dp),
                    testTag = "category-$categoryName"
                )
            }
            items(items = productsInCategory.sortedBy { it.name.lowercase(Locale("ru")) }, key = { it.id }) { product ->
                ProductRowItem(product = product, onProductChecked = onProductChecked)
            }
        }

        if (purchasedCategories.isNotEmpty()) {
            item {
                ProductCategoryHeader(
                    title = "Покупки",
                    icon = "🛒",
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                    testTag = "category-Покупки"
                )
            }
            purchasedCategories.forEach { entry ->
                val categoryName = entry.first
                val productsInCategory = entry.second
                item {
                    ProductCategoryHeader(
                        title = categoryName,
                        icon = productsInCategory.firstOrNull()?.categoryIcon.orEmpty(),
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 8.dp),
                        testTag = "purchased-category-$categoryName",
                        textStyle = MaterialTheme.typography.titleSmall
                    )
                }
                items(items = productsInCategory.sortedBy { it.name.lowercase(Locale("ru")) }, key = { it.id }) { product ->
                    ProductRowItem(product = product, onProductChecked = onProductChecked)
                }
            }
        }
    }
}

@Composable
private fun ProductCategoryHeader(
    title: String,
    icon: String,
    modifier: Modifier = Modifier,
    testTag: String,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmartMealText(text = title, style = textStyle, color = TextBlack, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag(testTag))
            if (icon.isNotBlank()) {
                SmartMealText(text = icon, fontSize = textStyle.fontSize, modifier = Modifier.padding(start = 6.dp))
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

private sealed interface ProductContentState {
    data object Loading : ProductContentState
    data object Empty : ProductContentState
    data object Error : ProductContentState
    data object List : ProductContentState
}

internal fun filterProductsByDateRange(products: List<ProductUiModel>, startDateKey: String?, endDateKey: String?): List<ProductUiModel> {
    val startKey = startDateKey ?: return products
    val endKey = endDateKey ?: startKey
    val lowerBound = minOf(startKey, endKey)
    val upperBound = maxOf(startKey, endKey)
    return products.filter { product -> product.actualDates.any { it in lowerBound..upperBound } }
}

internal fun aggregateProductsForDisplay(products: List<ProductUiModel>): List<ProductUiModel> {
    return products.groupBy { Triple(it.name.trim().lowercase(Locale("ru")), it.categoryName, it.checked) }.values.map { grouped ->
        val first = grouped.first()
        first.copy(
            id = "${first.name}_${first.categoryName}_${first.checked}",
            amount = formatWeightDisplay(grouped.sumOf { parseWeightToGrams(it.amount) }),
            sourceIds = grouped.flatMap { it.sourceIds }.toSet()
        )
    }
}

@Composable
private fun ProductRowItem(product: ProductUiModel, onProductChecked: (Collection<String>, Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(if (product.checked) LightGreenBg.copy(alpha = 0.48f) else Color.Transparent, RoundedCornerShape(14.dp)).padding(vertical = 5.dp).animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = product.checked, onCheckedChange = { onProductChecked(product.sourceIds, !product.checked) })
        SmartMealText(text = product.icon, modifier = Modifier.padding(horizontal = 6.dp))
        SmartMealText(text = product.name, style = MaterialTheme.typography.bodyLarge, color = if (product.checked) PrimaryGreen else TextBlack, modifier = Modifier.weight(1f))
        SmartMealText(text = product.amount, style = MaterialTheme.typography.bodyLarge, color = PrimaryGreen, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun formatMonthYearForSelector(date: Date): String = SimpleDateFormat("LLLL yyyy", Locale("ru")).format(date).replaceFirstChar { it.titlecase(Locale("ru")) }

internal fun formatMonthYearRangeForSelector(startDate: Date, endDate: Date): String {
    val startMonth = SimpleDateFormat("LLLL", Locale("ru")).format(startDate).replaceFirstChar { it.titlecase(Locale("ru")) }
    val endMonth = SimpleDateFormat("LLLL", Locale("ru")).format(endDate).replaceFirstChar { it.titlecase(Locale("ru")) }
    val year = SimpleDateFormat("yyyy", Locale.US).format(startDate)
    return if (startMonth == endMonth) "$startMonth $year" else "$startMonth - $endMonth $year"
}
