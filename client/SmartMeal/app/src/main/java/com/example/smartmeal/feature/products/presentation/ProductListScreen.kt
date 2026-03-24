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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.selectors.DaySelector
import com.example.smartmeal.ui.theme.LightGreenBg
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.TextBlack
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProductListScreen(
    products: List<ProductUiModel>,
    selectedDate: Date?,
    selectedStartDayIndex: Int?,
    selectedEndDayIndex: Int?,
    dateRangeText: String,
    onDaySelected: (String) -> Unit,
    onProductChecked: (Collection<String>, Boolean) -> Unit,
    onCheckAll: (Collection<String>, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val filteredProducts = remember(products, selectedStartDayIndex, selectedEndDayIndex) {
        filterProductsByDayRange(products, selectedStartDayIndex, selectedEndDayIndex)
    }
    val aggregatedProducts = remember(filteredProducts) {
        aggregateProductsForDisplay(filteredProducts)
    }
    val selectedDateDisplay = remember(filteredProducts, selectedDate) {
        buildSelectedDateDisplay(filteredProducts, selectedDate)
    }
    val allVisibleProductIds = remember(filteredProducts) {
        filteredProducts.flatMap { it.sourceIds }.distinct()
    }
    val allVisibleChecked = filteredProducts.isNotEmpty() && filteredProducts.all { it.checked }
    val listState = rememberLazyListState()
    val contentState = when {
        isLoading -> ProductContentState.Loading
        !errorMessage.isNullOrBlank() -> ProductContentState.Error
        aggregatedProducts.isEmpty() -> ProductContentState.Empty
        else -> ProductContentState.List
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp)
    ) {
        SmartMealText(
            text = "Список продуктов",
            style = MaterialTheme.typography.titleLarge,
            color = TextBlack,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
                .testTag("title")
        )

        DaySelector(
            selectedRange = selectedDayRange(selectedStartDayIndex, selectedEndDayIndex),
            onDaySelected = onDaySelected
        )

        SmartMealText(
            text = if (selectedDateDisplay.isNotBlank()) selectedDateDisplay else dateRangeText,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
                .testTag("selectedDateText")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
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

        AnimatedContent(
            targetState = contentState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.985f)) togetherWith
                    (fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.985f))
            },
            label = "ProductContentState"
        ) { state ->
            when (state) {
                ProductContentState.Loading -> {
                    SmartMealText(
                        text = "Собираем список продуктов...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextBlack,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 32.dp)
                            .testTag("products_loading")
                    )
                }

                ProductContentState.Error -> {
                    SmartMealText(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 32.dp)
                            .testTag("products_error")
                    )
                }

                ProductContentState.Empty -> {
                    SmartMealText(
                        text = "Список продуктов пуст",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextBlack,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 32.dp)
                            .testTag("emptyProducts")
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
        "Фрукты и ягоды",
        "Овощи и фрукты",
        "Овощи, зелень и грибы",
        "Мясо и мясная продукция",
        "Рыба и морепродукты",
        "Мясо и рыба",
        "Молочные продукты и яйца",
        "Молочные продукты",
        "Бобовые",
        "Бакалея и молочные продукты",
        "Мука и мучные изделия",
        "Приправы и специи",
        "Специи",
        "Соусы",
        "Орехи и семена",
        "Добавки для приготовления блюд",
        "Масла",
        "Напитки",
        "Зерновые",
        "Сладости",
        "Разное",
        "Покупки"
    )

    val categories = aggregatedProducts.groupBy { product ->
        if (product.checked) "Покупки" else product.categoryName
    }

    val sortedCategories = categories.entries
        .filter { it.key != "Покупки" }
        .sortedBy { (categoryName, _) ->
            categoryOrder.indexOf(categoryName).let { if (it == -1) Int.MAX_VALUE else it }
        }
        .let { ordered ->
            val shopping = categories.entries.find { it.key == "Покупки" }
            if (shopping != null) ordered + shopping else ordered
        }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        sortedCategories.forEachIndexed { index, entry ->
            val (categoryName, productsInCategory) = entry
            val categoryIcon = if (categoryName == "Покупки") {
                "🛒"
            } else {
                productsInCategory.firstOrNull()?.categoryIcon.orEmpty()
            }
            item {
                Column(
                    modifier = Modifier
                        .padding(top = if (index == 0) 6.dp else 14.dp, bottom = 6.dp)
                        .animateContentSize(animationSpec = tween(durationMillis = 220))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmartMealText(
                            text = categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextBlack,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("category-$categoryName")
                        )
                        SmartMealText(
                            text = categoryIcon,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
            items(
                items = productsInCategory.sortedBy { it.name.lowercase(Locale("ru")) },
                key = { it.id }
            ) { product ->
                ProductRowItem(
                    product = product,
                    onProductChecked = onProductChecked
                )
            }
        }
    }
}

private sealed interface ProductContentState {
    data object Loading : ProductContentState
    data object Empty : ProductContentState
    data object Error : ProductContentState
    data object List : ProductContentState
}

internal fun selectedDayRange(startIndex: Int?, endIndex: Int?): IntRange? {
    val start = startIndex ?: return null
    val end = endIndex ?: start
    return start..end
}

internal fun filterProductsByDayRange(
    products: List<ProductUiModel>,
    startIndex: Int?,
    endIndex: Int?
): List<ProductUiModel> {
    val range = selectedDayRange(startIndex, endIndex) ?: return products
    val hasDayBoundProducts = products.any { it.dayOffsets.isNotEmpty() }
    if (!hasDayBoundProducts) return products

    return products.filter { product ->
        product.dayOffsets.any { it in range }
    }
}

internal fun aggregateProductsForDisplay(products: List<ProductUiModel>): List<ProductUiModel> {
    return products
        .groupBy { product ->
            val displayCategory = if (product.checked) "Покупки" else product.categoryName
            Triple(product.name.trim().lowercase(Locale("ru")), displayCategory, product.checked)
        }
        .values
        .map { groupedProducts ->
            val first = groupedProducts.first()
            val displayCategory = if (first.checked) "Покупки" else first.categoryName
            first.copy(
                id = buildDisplayProductId(first.name, displayCategory, first.checked),
                amount = formatWeightDisplay(groupedProducts.sumOf { parseWeightToGrams(it.amount) }),
                categoryName = displayCategory,
                category = displayCategory,
                checked = first.checked,
                dayOffsets = groupedProducts.flatMap { it.dayOffsets }.toSet(),
                actualDates = groupedProducts.flatMap { it.actualDates }.toSet(),
                sourceIds = groupedProducts.flatMap { it.sourceIds }.toSet()
            )
        }
}

internal fun buildSelectedDateDisplay(products: List<ProductUiModel>, fallbackDate: Date?): String {
    val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val fullFormatter = SimpleDateFormat("EEEE - d MMMM yyyy 'г.'", Locale("ru"))
    val shortFormatter = SimpleDateFormat("d MMMM yyyy 'г.'", Locale("ru"))

    val dates = products
        .flatMap { it.actualDates }
        .distinct()
        .sorted()
        .mapNotNull { inputFormatter.parse(it) }

    if (dates.isEmpty()) {
        return fallbackDate?.let(fullFormatter::format).orEmpty()
    }

    val first = dates.first()
    val last = dates.last()
    return if (first == last) {
        fullFormatter.format(first)
    } else {
        "${shortFormatter.format(first)} - ${shortFormatter.format(last)}"
    }
}

internal fun buildDisplayProductId(name: String, categoryName: String, checked: Boolean): String {
    return "${name.trim()}_${categoryName.trim()}_$checked"
}

@Composable
private fun ProductRowItem(
    product: ProductUiModel,
    onProductChecked: (Collection<String>, Boolean) -> Unit
) {
    val rowShape: Shape = RoundedCornerShape(14.dp)
    val titleColor by animateColorAsState(
        targetValue = if (product.checked) PrimaryGreen.copy(alpha = 0.94f) else TextBlack,
        animationSpec = tween(durationMillis = 260),
        label = "productTitleColor"
    )
    val amountColor by animateColorAsState(
        targetValue = PrimaryGreen,
        animationSpec = tween(durationMillis = 220),
        label = "productAmountColor"
    )
    val rowHighlight by animateColorAsState(
        targetValue = if (product.checked) LightGreenBg.copy(alpha = 0.48f) else Color.Transparent,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "productRowHighlight"
    )
    val iconTint by animateColorAsState(
        targetValue = if (product.checked) PrimaryGreen else LightGreenBg.copy(alpha = 0.95f),
        animationSpec = tween(durationMillis = 240),
        label = "productIconTint"
    )
    val checkboxScale by animateFloatAsState(
        targetValue = if (product.checked) 1.18f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "productCheckboxScale"
    )
    val rowScale by animateFloatAsState(
        targetValue = if (product.checked) 1.015f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "productRowScale"
    )
    val rowOffsetX by animateFloatAsState(
        targetValue = if (product.checked) 6f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "productRowOffset"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
                translationX = rowOffsetX
            }
            .background(color = rowHighlight, shape = rowShape)
            .padding(vertical = 5.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 220))
            .testTag("product-${product.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clickable {
                    onProductChecked(product.sourceIds, !product.checked)
                }
                .testTag("checkbox-${product.id}")
        ) {
            AnimatedContent(
                targetState = product.checked,
                transitionSpec = {
                    (scaleIn(
                        initialScale = 0.72f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f)
                    ) + fadeIn(animationSpec = tween(180))) togetherWith
                        (scaleOut(
                            targetScale = 0.72f,
                            animationSpec = tween(180)
                        ) + fadeOut(animationSpec = tween(160)))
                },
                label = "productCheckbox"
            ) { isChecked ->
                Icon(
                    imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            scaleX = checkboxScale
                            scaleY = checkboxScale
                        }
                        .size(22.dp)
                )
            }
        }

        SmartMealText(
            text = product.icon,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        SmartMealText(
            text = product.name,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor,
            modifier = Modifier.weight(1f)
        )

        SmartMealText(
            text = product.amount,
            style = MaterialTheme.typography.bodyLarge,
            color = amountColor,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

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
