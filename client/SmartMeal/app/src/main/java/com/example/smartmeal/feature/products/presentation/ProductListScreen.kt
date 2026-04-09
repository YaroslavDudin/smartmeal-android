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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smartmeal.feature.home.presentation.CustomPlan
import com.example.smartmeal.feature.home.presentation.MyPlanSection
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.selectors.DateSelector
import com.example.smartmeal.ui.components.selectors.buildDateSelectorItems
import com.example.smartmeal.ui.components.selectors.formatSelectedDateLabel
import com.example.smartmeal.ui.theme.BgLightGray
import com.example.smartmeal.ui.theme.BorderGray
import com.example.smartmeal.ui.theme.LightGreenBg
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.TextBlack
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.clip

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
    onOrderModalConsumed: () -> Unit = {}
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
    val listState = rememberLazyListState()
    val hasSingleAvailableDate = availableDates.size == 1
    
    val contentState: ProductContentState = when {
        isLoading -> ProductContentState.Loading
        hasNoAvailableDays -> ProductContentState.Expired
        !errorMessage.isNullOrBlank() -> ProductContentState.Error
        aggregatedProducts.isEmpty() -> ProductContentState.Empty
        else -> ProductContentState.List
    }

    var showOrderModal by remember { mutableStateOf(openOrderModal) }
    androidx.compose.runtime.LaunchedEffect(openOrderModal) {
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

    Box(modifier = modifier.fillMaxSize().background(BgLightGray)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Элегантная шапка ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp)
            ) {
                SmartMealText(
                    text = "Продукты",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.align(Alignment.CenterStart).testTag("title")
                )
            }

            // --- Зона Контекста (Даты + План) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(vertical = 12.dp)
            ) {
                if (availableDates.isNotEmpty()) {
                    if (hasSingleAvailableDate) {
                        SmartMealText(
                            text = formatSelectedDateLabel(availableDates.first()),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryGreen,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    } else {
                        if (monthYearLabel.isNotBlank()) {
                            SmartMealText(
                                text = monthYearLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp)
                            )
                        }
                        DateSelector(
                            items = dateSelectorItems,
                            selectedStartId = selectedStartDateKey,
                            selectedEndId = selectedEndDateKey,
                            onItemClick = onDateSelected
                        )
                    }
                }

                if (customPlan != null) {
                    if (availableDates.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                            thickness = 1.dp,
                            color = BgLightGray
                        )
                    }
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Контент списка ---
            AnimatedContent<ProductContentState>(
                targetState = contentState,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.985f)) togetherWith
                        (fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.985f))
                },
                modifier = Modifier.weight(1f),
                label = "ProductContentState"
            ) { state ->
                when (state) {
                    ProductContentState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryGreen)
                        }
                    }
                    ProductContentState.Error -> {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            SmartMealText(text = errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        }
                    }
                    ProductContentState.Empty -> {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            SmartMealText(text = "Список продуктов пуст", color = Color.Gray)
                        }
                    }
                    ProductContentState.Expired -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            SmartMealText(text = "Доступные дни закончились", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onReselectPlan) { SmartMealText("Выбрать заново") }
                        }
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

        // --- Панель Действий (Sticky Bottom Bar) ---
        if (contentState == ProductContentState.List) {
            val checkedCount = aggregatedProducts.count { it.checked }
            
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Выбрать всё
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onCheckAll(allVisibleProductIds, !allVisibleChecked) }
                    ) {
                        Checkbox(
                            checked = allVisibleChecked,
                            onCheckedChange = { onCheckAll(allVisibleProductIds, it) },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                        )
                        SmartMealText(
                            text = "Выбрать всё",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextBlack
                        )
                    }

                    // Кнопка Заказать
                    androidx.compose.material3.Button(
                        onClick = { showOrderModal = true },
                        enabled = checkedCount > 0,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SmartMealText(
                                text = "Заказать",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (checkedCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    SmartMealText(
                                        text = checkedCount.toString(),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 12.sp,
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

    val regularCategories = remember(aggregatedProducts) {
        aggregatedProducts
            .filterNot { it.checked }
            .groupBy { it.categoryName }
            .toList()
            .sortedBy { (categoryName, _) ->
                categoryOrder.indexOf(categoryName).let { if (it == -1) Int.MAX_VALUE else it }
            }
    }

    val purchasedCategories = remember(aggregatedProducts) {
        aggregatedProducts
            .filter { it.checked }
            .groupBy { it.categoryName }
            .toList()
            .sortedBy { (categoryName, _) ->
                categoryOrder.indexOf(categoryName).let { if (it == -1) Int.MAX_VALUE else it }
            }
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
                color = if (title == "Покупки") Color.Gray else PrimaryGreen, 
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
    return products.groupBy { Triple(it.name.trim().lowercase(Locale("ru")), it.categoryName, it.checked) }.values.map { grouped ->
        val first = grouped.first()

        val isPiece = first.amount.contains("шт")

        val totalValue = grouped.sumOf { parseWeightToGrams(it.amount) }

        val finalAmountString = if (isPiece) {
            val formatted = if (totalValue % 1.0 == 0.0) totalValue.toInt().toString() else totalValue.toString()
            "$formatted шт"
        } else {
            formatWeightDisplay(totalValue)
        }

        first.copy(
            id = "${first.name}_${first.categoryName}_${first.checked}",
            amount = finalAmountString,
            sourceIds = grouped.flatMap { it.sourceIds }.toSet()
        )
    }
}

@Composable
private fun ProductRowItem(product: ProductUiModel, onProductChecked: (Collection<String>, Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize()
            .clickable { onProductChecked(product.sourceIds, !product.checked) },
        shape = RoundedCornerShape(16.dp),
        color = if (product.checked) Color(0xFFF4F9F4) else Color.White,
        border = if (product.checked)
            androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen)
        else
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
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
            SmartMealText(text = product.icon, modifier = Modifier.padding(horizontal = 8.dp), fontSize = 20.sp)
            SmartMealText(
                text = product.name,
                fontSize = 16.sp,
                color = if (product.checked) PrimaryGreen else TextBlack,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            SmartMealText(
                text = product.amount,
                fontSize = 15.sp,
                color = if (product.checked) PrimaryGreen else Color.Gray,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

private fun formatMonthYearForSelector(date: Date): String = SimpleDateFormat("LLLL yyyy", Locale("ru")).format(date).replaceFirstChar { it.titlecase(Locale("ru")) }

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
                            .clip(RoundedCornerShape(16.dp))
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
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SmartMealText(
                            text = store.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextBlack,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PdfPreviewDialog(
    storeName: String,
    products: List<ProductUiModel>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { SmartMealText("Список для $storeName (PDF Preview)") },
        text = {
            LazyColumn {
                items(products, key = { it.id }) { product ->
                    SmartMealText(
                        text = "• ${product.name} - ${product.amount}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                SmartMealText("Скачать", color = PrimaryGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                SmartMealText("Закрыть", color = Color.Gray)
            }
        }
    )
}