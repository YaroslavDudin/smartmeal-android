package com.example.smartmeal.feature.products.presentation



import androidx.compose.material3.Scaffold
import com.example.smartmeal.ui.components.cards.BottomNavigationBar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.LightGreenBg
import com.example.smartmeal.ui.theme.TextBlack

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.smartmeal.ui.components.chips_filters.FilterChip

@Composable
fun ProductListScreen(
    products: List<ProductUiModel>,
    selectedStartDayIndex: Int?,
    selectedEndDayIndex: Int?,
    onDayRangeSelected: (Int?, Int?) -> Unit,
    onProductChecked: (String, Boolean) -> Unit,
    dateRangeText: String
) {
    Scaffold(
        modifier = Modifier.background(Color.White),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Список продуктов",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
        },
        bottomBar = {
            Box(modifier = Modifier.background(Color.White)) {
                BottomNavigationBar(selectedItem = 1)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 16.dp)
                .padding(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding()
                )
        ) {
            DayRangeSelector(
                selectedStartIndex = selectedStartDayIndex,
                selectedEndIndex = selectedEndDayIndex,
                onRangeSelected = onDayRangeSelected
            )

            Text(
                text = dateRangeText,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            val categories = products.groupBy { it.categoryName }
            categories.entries.forEachIndexed { idx, entry ->
                val (categoryName, productsInCategory) = entry
                if (idx > 0) HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    thickness = 2.dp,
                    color = LightGreenBg
                )
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack,
                        modifier = Modifier.testTag("category-$categoryName")
                    )
                    Text(
                        text = productsInCategory.firstOrNull()?.categoryIcon.orEmpty(),
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                    Spacer(modifier = Modifier.height(4.dp))
                    productsInCategory.forEach { product ->
                        ProductRowItem(product, categoryName, onProductChecked)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRowItem(
    product: ProductUiModel,
    categoryName: String,
    onProductChecked: (String, Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (product.checked) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Checked",
                tint = PrimaryGreen,
                modifier = Modifier
                    .size(22.dp)
                    .testTag("checked-${product.name}")
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.RadioButtonUnchecked,
                contentDescription = "Unchecked",
                tint = Color.Gray,
                modifier = Modifier
                    .size(22.dp)
                    .testTag("unchecked-${product.name}")
            )
        }
        Text(
            text = product.icon,
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .testTag("icon-${product.name}")
        )
        val isShopping = categoryName == "Покупки"
        Text(
            text = product.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (product.checked) PrimaryGreen else TextBlack,
            textDecoration = if (product.checked && !isShopping) TextDecoration.LineThrough else null,
            modifier = Modifier
                .weight(1f)
                .testTag("product-${product.name}")
        )
        Text(
            text = product.amount,
            style = MaterialTheme.typography.bodyLarge,
            color = if (product.checked) PrimaryGreen else TextBlack,
            textDecoration = if (product.checked && !isShopping) TextDecoration.LineThrough else null,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun DayRangeSelector(
    selectedStartIndex: Int?,
    selectedEndIndex: Int?,
    onRangeSelected: (Int?, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEachIndexed { index, day ->
            val isSelected = index == selectedStartIndex || index == selectedEndIndex
            FilterChip(
                label = day,
                isSelected = isSelected,
                onClick = {
                    when {
                        selectedStartIndex == null -> onRangeSelected(index, null)
                        selectedEndIndex == null -> {
                            if (index < selectedStartIndex) {
                                onRangeSelected(index, selectedStartIndex)
                            } else {
                                onRangeSelected(selectedStartIndex, index)
                            }
                        }
                        else -> onRangeSelected(index, null)
                    }
                },
                modifier = Modifier.testTag("day_chip_$index")
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
fun ProductListScreenPreview() {
    val products = listOf(
        ProductUiModel(
            name = "Микс салатов",
            amount = "200 г",
            category = "vegetable",
            icon = "🥗",
            categoryName = "Овощи и фрукты",
            categoryIcon = "🥬",
            checked = false
        ),
        ProductUiModel(
            name = "Яблоко",
            amount = "150 г",
            category = "vegetable",
            icon = "🍏",
            categoryName = "Овощи и фрукты",
            categoryIcon = "🥬",
            checked = false
        ),
        ProductUiModel(
            name = "Банан",
            amount = "120 г",
            category = "vegetable",
            icon = "🍌",
            categoryName = "Овощи и фрукты",
            categoryIcon = "🥬",
            checked = false
        ),
        ProductUiModel(
            name = "Апельсин",
            amount = "130 г",
            category = "vegetable",
            icon = "🍊",
            categoryName = "Овощи и фрукты",
            categoryIcon = "🥬",
            checked = false
        ),
        ProductUiModel(
            name = "Киви",
            amount = "90 г",
            category = "vegetable",
            icon = "🥝",
            categoryName = "Овощи и фрукты",
            categoryIcon = "🥬",
            checked = false
        ),
        ProductUiModel(
            name = "Куриное филе",
            amount = "300 г",
            category = "meat",
            icon = "🍗",
            categoryName = "Мясо и рыба",
            categoryIcon = "🐟",
            checked = false
        ),
        ProductUiModel(
            name = "Гречка",
            amount = "50 г",
            category = "shopping",
            icon = "🛒",
            categoryName = "Покупки",
            categoryIcon = "🛒",
            checked = true
        )
    )
    ProductListScreen(
        products = products,
        selectedStartDayIndex = 0,
        selectedEndDayIndex = 1,
        onDayRangeSelected = { _, _ -> },
        onProductChecked = { _, _ -> },
        dateRangeText = "Пн-Вт: 2-3 марта 2026 г."
    )
}
data class ProductUiModel(
    val name: String,
    val amount: String,
    val category: String,
    val icon: String,
    val categoryName: String,
    val categoryIcon: String,
    val checked: Boolean = false
)
