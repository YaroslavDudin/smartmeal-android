package com.example.smartmeal.feature.products.presentation

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import com.example.smartmeal.feature.home.presentation.CustomPlan
import com.example.smartmeal.feature.home.presentation.MyPlanSection
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.feedback.ProductListSkeleton
import com.example.smartmeal.ui.components.selectors.DateSelector
import com.example.smartmeal.ui.components.selectors.buildDateSelectorItems
import com.example.smartmeal.ui.components.selectors.formatSelectedDateLabel
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.TextBlack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val categoryOrder = listOf(
    "Фрукты и ягоды", "Овощи и фрукты", "Овощи, зелень и грибы",
    "Мясо и мясная продукция", "Рыба и морепродукты", "Мясо и рыба",
    "Молочные продукты и яйца", "Молочные продукты", "Бобовые",
    "Консервированные продукты", "Пасты", "Бакалея и молочные продукты",
    "Мука и мучные изделия", "Приправы и специи", "Специи", "Соусы",
    "Орехи и семена", "Добавки для приготовления блюд", "Масла",
    "Напитки", "Зерновые", "Сладости", "Разное", "Покупки"
)

private val ProductHeroStart = Color(0xFFFFFFFF)
private val ProductHeroEnd = Color(0xFFF0F0F0)
private val ProductSoftSurface = Color(0xFFF8F8F8)
private val ProductBorder = Color(0xFFE0E0E0)
private val ProductMutedText = Color(0xFF757575)

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
    viewModel: ProductListViewModel,
    products: List<ProductUiModel>,
    selectedDate: Date?,
    selectedStartDateKey: String?,
    selectedEndDateKey: String?,
    dateRangeText: String,
    onDateSelected: (String) -> Unit,
    onProductChecked: (Collection<String>, Boolean) -> Unit,
    onCheckAll: (Collection<String>, Boolean) -> Unit,
    onReselectPlan: () -> Unit,
    modifier: Modifier = Modifier,
    hasNoAvailableDays: Boolean = false,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    customPlan: CustomPlan? = null,
    openOrderModal: Boolean = false,
    scrollToCart: Boolean = false,
    onOrderModalConsumed: () -> Unit = {},
    onScrollToCartConsumed: () -> Unit = {}
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
            .filter { !it.before(normalizeProductDate(Date())) }
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
    val checkedCount = aggregatedProducts.count { it.checked }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isSmallScreen = configuration.screenHeightDp < 640 || configuration.screenWidthDp < 360

    val itemsBeforePurchased = remember(aggregatedProducts, isLandscape) {
        val regularCategoriesList = aggregatedProducts
            .filterNot { it.checked }
            .groupBy { it.categoryName }
            .toList()
            .sortedBy { (categoryName, _) ->
                categoryOrder.indexOf(categoryName).let { if (it == -1) Int.MAX_VALUE else it }
            }

        var count = 3
        regularCategoriesList.forEach { (_, productsInCategory) ->
            count += 1
            count += if (isLandscape) (productsInCategory.size + 1) / 2 else productsInCategory.size
        }
        count
    }

    LaunchedEffect(scrollToCart, aggregatedProducts) {
        if (scrollToCart) {
            if (aggregatedProducts.any { it.checked }) {
                kotlinx.coroutines.delay(100)
                listState.animateScrollToItem(index = itemsBeforePurchased, scrollOffset = -100)
            }
            onScrollToCartConsumed()
        }
    }

    val hasSingleAvailableDate = availableDates.size == 1
    val contentState: ProductContentState = when {
        isLoading -> ProductContentState.Loading
        hasNoAvailableDays -> ProductContentState.Expired
        !errorMessage.isNullOrBlank() -> ProductContentState.Error
        aggregatedProducts.isEmpty() -> ProductContentState.Empty
        else -> ProductContentState.List
    }

    var showOrderModal by remember { mutableStateOf(openOrderModal) }
    LaunchedEffect(openOrderModal) {
        if (openOrderModal) {
            showOrderModal = true
            onOrderModalConsumed()
        }
    }

    if (showOrderModal) {
        OrderModalBottomSheet(
            viewModel = viewModel,
            onDismiss = { showOrderModal = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (isLandscape) 88.dp else 116.dp)
        ) {
            item {
                ProductHeroSection(
                    modifier = Modifier.padding(
                        start = if (isSmallScreen) 16.dp else 24.dp,
                        end = if (isSmallScreen) 16.dp else 24.dp,
                        top = if (isSmallScreen) 8.dp else 12.dp,
                        bottom = if (isSmallScreen) 8.dp else 14.dp
                    ),
                    dateRangeText = dateRangeText,
                    checkedCount = checkedCount,
                    testTag = "title"
                )
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isSmallScreen) 10.dp else 16.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, ProductBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        if (availableDates.isNotEmpty()) {
                            RangeContextRow(
                                dateRangeText = dateRangeText,
                                monthYearLabel = monthYearLabel
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (hasSingleAvailableDate) {
                                SmartMealText(
                                    text = formatSelectedDateLabel(availableDates.first()),
                                    fontSize = if (isSmallScreen) 14.sp else 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            } else {
                                DateSelector(
                                    items = dateSelectorItems,
                                    selectedStartId = selectedStartDateKey,
                                    selectedEndId = selectedEndDateKey,
                                    onItemClick = onDateSelected,
                                    isSmallScreen = isSmallScreen
                                )
                            }
                        }

                        if (customPlan != null && !isLoading) {
                            if (availableDates.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = ProductBorder)
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            val apiFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            SmartMealText(
                                text = "Период плана",
                                style = MaterialTheme.typography.labelLarge,
                                color = ProductMutedText,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            MyPlanSection(
                                customPlan = customPlan,
                                selectedDate = selectedStartDateKey?.let { parseApiDate(it) },
                                isRangeSelection = true,
                                onDateSelectedFromPlan = { },
                                onRangeSelected = { start, end ->
                                    onDateSelected(apiFormatter.format(start))
                                    onDateSelected(apiFormatter.format(end))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_my_plan")
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 16.dp)) }

            when (contentState) {
                ProductContentState.Loading -> item { ProductListSkeleton() }
                ProductContentState.Error -> item {
                    ProductMessageCard(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                ProductContentState.Empty -> item {
                    ProductMessageCard(
                        text = "Список продуктов пока пуст",
                        color = ProductMutedText
                    )
                }
                ProductContentState.Expired -> item {
                    ProductExpiredStateCard(onReselectPlan = onReselectPlan)
                }
                ProductContentState.List -> {
                    productCategoryItems(
                        aggregatedProducts = aggregatedProducts,
                        onProductChecked = onProductChecked,
                        isLandscape = isLandscape
                    )
                }
            }
        }

        if (contentState == ProductContentState.List) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = if (isLandscape) 52.dp else 18.dp)
                    .padding(bottom = if (isLandscape) 10.dp else 14.dp)
                    .navigationBarsPadding()
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, ProductBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = if (isLandscape) 8.dp else 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .testTag("product_select_all")
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) { onCheckAll(allVisibleProductIds, !allVisibleChecked) }
                        ) {
                            Checkbox(
                                checked = allVisibleChecked,
                                onCheckedChange = { onCheckAll(allVisibleProductIds, it) },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen),
                                modifier = Modifier.scale(0.8f)
                            )
                            SmartMealText(
                                text = "Все",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextBlack
                            )
                        }

                        if (checkedCount > 0) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        val targetIndex = itemsBeforePurchased
                                        val currentIndex = listState.firstVisibleItemIndex
                                        if (kotlin.math.abs(targetIndex - currentIndex) > 25) {
                                            listState.scrollToItem(
                                                if (targetIndex > currentIndex) targetIndex - 10 else targetIndex + 10
                                            )
                                        }
                                        listState.animateScrollToItem(index = targetIndex, scrollOffset = -100)
                                    }
                                },
                                modifier = Modifier.height(if (isLandscape) 38.dp else 44.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = PrimaryGreen
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SmartMealText(
                                        text = "Корзина",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { showOrderModal = true },
                            enabled = checkedCount > 0,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(if (isLandscape) 38.dp else 44.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SmartMealText(
                                    text = "Заказать",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (checkedCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        SmartMealText(
                                            text = checkedCount.toString(),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.productCategoryItems(
    aggregatedProducts: List<ProductUiModel>,
    onProductChecked: (Collection<String>, Boolean) -> Unit,
    isLandscape: Boolean
) {
    val regularCategories = aggregatedProducts
        .filterNot { it.checked }
        .groupBy { it.categoryName }
        .toList()
        .sortedBy { (categoryName, _) ->
            categoryOrder.indexOf(categoryName).let { if (it == -1) Int.MAX_VALUE else it }
        }

    val purchasedCategories = aggregatedProducts
        .filter { it.checked }
        .groupBy { it.categoryName }
        .toList()
        .sortedBy { (categoryName, _) ->
            categoryOrder.indexOf(categoryName).let { if (it == -1) Int.MAX_VALUE else it }
        }

    regularCategories.forEachIndexed { index, entry ->
        val categoryName = entry.first
        val productsInCategory = entry.second
        item {
            ProductCategoryHeader(
                title = categoryName,
                icon = productsInCategory.firstOrNull()?.categoryIcon.orEmpty(),
                modifier = Modifier.padding(top = if (index == 0) 6.dp else 14.dp, bottom = 6.dp, start = if (isLandscape) 24.dp else 8.dp),
                testTag = "category-$categoryName"
            )
        }

        val items = productsInCategory.sortedBy { it.name.lowercase(Locale("ru")) }
        if (isLandscape) {
            items.chunked(2).forEach { rowItems ->
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        rowItems.forEach { product ->
                            Box(modifier = Modifier.weight(1f)) {
                                ProductRowItem(product = product, onProductChecked = onProductChecked)
                            }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            items(items = items, key = { it.id }) { product ->
                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                    ProductRowItem(product = product, onProductChecked = onProductChecked)
                }
            }
        }
    }

    if (purchasedCategories.isNotEmpty()) {
        item {
            ProductCategoryHeader(
                title = "Покупки",
                icon = "🛒",
                modifier = Modifier.padding(top = 18.dp, bottom = 6.dp, start = if (isLandscape) 24.dp else 8.dp),
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
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = if (isLandscape) 32.dp else 16.dp),
                    testTag = "purchased-category-$categoryName",
                    textStyle = MaterialTheme.typography.titleSmall
                )
            }

            val items = productsInCategory.sortedBy { it.name.lowercase(Locale("ru")) }
            if (isLandscape) {
                items.chunked(2).forEach { rowItems ->
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            rowItems.forEach { product ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ProductRowItem(product = product, onProductChecked = onProductChecked)
                                }
                                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                items(items = items, key = { it.id }) { product ->
                    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                        ProductRowItem(product = product, onProductChecked = onProductChecked)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductHeroSection(
    modifier: Modifier = Modifier,
    dateRangeText: String,
    checkedCount: Int,
    testTag: String
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(ProductHeroStart, ProductHeroEnd)))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SmartMealText(
                    text = "Продукты",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.testTag(testTag)
                )
                SmartMealText(
                    text = "Соберите покупки по выбранному периоду и отмечайте уже купленное без лишних переходов.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProductMutedText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProductInfoChip(
                        icon = Icons.Default.DateRange,
                        text = dateRangeText.ifBlank { "Выберите диапазон" }
                    )
                    ProductInfoChip(
                        icon = Icons.Default.CheckCircle,
                        text = if (checkedCount > 0) "$checkedCount выбрано" else "Покупки не отмечены"
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductInfoChip(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(16.dp)
            )
            SmartMealText(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = TextBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RangeContextRow(
    dateRangeText: String,
    monthYearLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SmartMealText(
                text = "Текущий диапазон",
                style = MaterialTheme.typography.labelLarge,
                color = ProductMutedText,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            SmartMealText(
                text = dateRangeText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextBlack
            )
        }
        if (monthYearLabel.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ProductSoftSurface
            ) {
                SmartMealText(
                    text = monthYearLabel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = ProductMutedText,
                    fontWeight = FontWeight.Medium
                )
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
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
) {
    Column(modifier = modifier.padding(start = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmartMealText(
                text = title.uppercase(),
                style = textStyle,
                color = if (title == "Покупки") ProductMutedText else PrimaryGreen,
                modifier = Modifier.testTag(testTag)
            )
            if (icon.isNotBlank()) {
                SmartMealText(text = icon, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

private sealed interface ProductContentState {
    data object Loading : ProductContentState
    data object Empty : ProductContentState
    data object Error : ProductContentState
    data object Expired : ProductContentState
    data object List : ProductContentState
}

private fun normalizeProductDate(date: Date): Date {
    return java.util.Calendar.getInstance().apply {
        time = date
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.time
}

internal fun filterProductsByDateRange(
    products: List<ProductUiModel>,
    startDateKey: String?,
    endDateKey: String?
): List<ProductUiModel> {
    val startKey = startDateKey ?: return products
    val endKey = endDateKey ?: startKey
    val lowerBound = minOf(startKey, endKey)
    val upperBound = maxOf(startKey, endKey)
    return products.filter { product -> product.actualDates.any { it in lowerBound..upperBound } }
}

internal fun aggregateProductsForDisplay(products: List<ProductUiModel>): List<ProductUiModel> {
    return products.groupBy {
        Triple(it.name.trim().lowercase(Locale("ru")), it.categoryName, it.checked)
    }.values.map { grouped ->
        val first = grouped.first()
        val isPiece = first.amount.contains("шт")
        val totalValue = grouped.sumOf { parseWeightToGrams(it.amount) }

        val finalAmountString = if (isPiece) {
            val formatted = if (totalValue % 1.0 == 0.0) totalValue.toInt().toString() else totalValue.toString()
            "$formatted шт"
        } else {
            formatWeightDisplay(totalValue, first.name)
        }

        first.copy(
            id = "${first.name}_${first.categoryName}_${first.checked}",
            amount = finalAmountString,
            sourceIds = grouped.flatMap { it.sourceIds }.toSet()
        )
    }
}

@Composable
private fun ProductRowItem(
    product: ProductUiModel,
    onProductChecked: (Collection<String>, Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize()
            .clickable { onProductChecked(product.sourceIds, !product.checked) },
        shape = RoundedCornerShape(18.dp),
        color = if (product.checked) Color(0xFFF2FAF1) else Color.White,
        border = if (product.checked) {
            BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.65f))
        } else {
            BorderStroke(1.dp, ProductBorder.copy(alpha = 0.7f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = product.checked,
                onCheckedChange = { onProductChecked(product.sourceIds, it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryGreen,
                    uncheckedColor = Color(0xFFE0E0E0)
                )
            )
            SmartMealText(
                text = product.icon,
                modifier = Modifier.padding(horizontal = 8.dp),
                fontSize = 20.sp
            )
            SmartMealText(
                text = product.name,
                fontSize = 16.sp,
                color = if (product.checked) PrimaryGreen else TextBlack,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            SmartMealText(
                text = product.amount,
                fontSize = 14.sp,
                color = if (product.checked) PrimaryGreen else ProductMutedText,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

private fun formatMonthYearForSelector(date: Date): String {
    return SimpleDateFormat("LLLL yyyy", Locale("ru"))
        .format(date)
        .replaceFirstChar { it.titlecase(Locale("ru")) }
}

internal fun formatMonthYearRangeForSelector(startDate: Date, endDate: Date): String {
    val startMonth = SimpleDateFormat("LLLL", Locale("ru")).format(startDate).replaceFirstChar { it.titlecase(Locale("ru")) }
    val endMonth = SimpleDateFormat("LLLL", Locale("ru")).format(endDate).replaceFirstChar { it.titlecase(Locale("ru")) }
    val startYear = SimpleDateFormat("yyyy", Locale.US).format(startDate)
    val endYear = SimpleDateFormat("yyyy", Locale.US).format(endDate)
    return when {
        startMonth == endMonth && startYear == endYear -> "$startMonth $startYear"
        startYear == endYear -> "$startMonth - $endMonth $startYear"
        else -> "$startMonth $startYear - $endMonth $endYear"
    }
}

@Composable
private fun ProductMessageCard(
    text: String,
    color: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ProductBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            SmartMealText(text = text, color = color, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ProductExpiredStateCard(
    onReselectPlan: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("products_expired_state"),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ProductBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SmartMealText(
                text = "Доступные дни закончились",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            SmartMealText(
                text = "Выберите новый период питания, чтобы снова собрать актуальный список продуктов.",
                style = MaterialTheme.typography.bodyMedium,
                color = ProductMutedText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onReselectPlan,
                modifier = Modifier.testTag("products_reselect_plan_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                SmartMealText("Выбрать заново", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderModalBottomSheet(
    viewModel: ProductListViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val shareText = { text: String, storeName: String ->
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Мой список покупок ($storeName):\n\n$text")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Заказать продукты"))
    }

    data class StoreItem(val name: String, val iconRes: Int)
    val stores = remember {
        listOf(
            StoreItem("Яндекс Лавка", com.example.smartmeal.R.drawable.yandex_lavka_icon_logo),
            StoreItem("Самокат", com.example.smartmeal.R.drawable.samokat_sign_logo),
            StoreItem("Яндекс Маркет", com.example.smartmeal.R.drawable.yandex_market_sign_logo)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SmartMealText(
                text = "Где заказать продукты?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                stores.forEach { store ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.exportCheckedProducts(
                                    onSuccess = { txtContent ->
                                        shareText(txtContent, store.name)
                                        onDismiss()
                                    },
                                    onError = { errorMsg ->
                                        android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = store.iconRes),
                            contentDescription = store.name,
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White, RoundedCornerShape(18.dp))
                                .padding(6.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SmartMealText(
                            text = store.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextBlack,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
