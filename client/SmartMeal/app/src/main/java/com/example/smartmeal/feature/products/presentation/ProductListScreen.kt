package com.example.smartmeal.feature.products.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.smartmeal.ui.components.selectors.DaySelector
import com.example.smartmeal.ui.theme.LightGreenBg
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.TextBlack
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ProductListScreen(
    products: List<ProductUiModel>,
    selectedDate: Date?,
    onDaySelected: (String) -> Unit,
    onProductChecked: (String, Boolean) -> Unit,
    onCheckAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayFormatter = remember { SimpleDateFormat("EEEE - d MMMM yyyy 'г.'", Locale("ru")) }

    val selectedDateDisplay = selectedDate?.let { displayFormatter.format(it) } ?: ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp)
    ) {
        // Заголовок
        Text(
            text = "Список продуктов",
            style = MaterialTheme.typography.titleLarge,
            color = TextBlack,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
                .testTag("title")
        )

        // DaySelector
        DaySelector(
            selectedDay = selectedDate?.let { getDayOfWeek(it) } ?: "Пн",
            onDaySelected = onDaySelected
        )

        // Текст выбранной даты
        Text(
            text = selectedDateDisplay,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
                .testTag("selectedDateText")
        )

        // Кнопка "Выбрать всё"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("checkAllRow"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Выбрать всё",
                style = MaterialTheme.typography.bodyMedium,
                color = TextBlack,
                modifier = Modifier.padding(end = 8.dp)
            )
            Checkbox(
                checked = products.all { it.checked },
                onCheckedChange = { onCheckAll() },
                modifier = Modifier.testTag("checkAllButton")
            )
        }

        // Фильтруем продукты по выбранной дате
        val filteredProducts = if (selectedDate != null) {
            val dateStr = dateFormatter.format(selectedDate)
            products.filter { it.actualDates.contains(dateStr) }
        } else {
            products
        }

        val categoryOrder = listOf(
            "Овощи и фрукты",
            "Мясо и рыба",
            "Молочные продукты",
            "Бакалея и молочные продукты",
            "Специи",
            "Масла",
            "Напитки",
            "Зерновые",
            "Сладости",
            "Разное",
            "Покупки"
        )

        val categories = filteredProducts.groupBy { product ->
            if (product.checked) "Покупки" else product.categoryName
        }

        val sortedCategories = categories.entries
            .filter { it.key != "Покупки" }
            .sortedBy { (categoryName, _) ->
                categoryOrder.indexOf(categoryName).let { if (it == -1) Int.MAX_VALUE else it }
            }
            .let { if (categories.containsKey("Покупки")) it + (categories.entries.find { it.key == "Покупки" }!!) else it }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            sortedCategories.forEachIndexed { idx, entry ->
                val (categoryName, productsInCategory) = entry
                item {
                    if (idx > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            thickness = 2.dp,
                            color = LightGreenBg
                        )
                    }
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextBlack,
                                modifier = Modifier.testTag("category-$categoryName")
                            )
                            Text(
                                text = "",
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                items(
                    items = if (categoryName == "Покупки") {
                        productsInCategory.sortedBy { it.name }
                    } else {
                        productsInCategory
                    },
                    key = { it.id }
                ) { product ->
                    ProductRowItem(
                        product = product,
                        categoryName = categoryName,
                        onProductChecked = onProductChecked
                    )
                }
            }
        }
    }
}



private fun getDayOfWeek(date: Date): String {
    val calendar = Calendar.getInstance().apply { time = date }
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Пн"
        Calendar.TUESDAY -> "Вт"
        Calendar.WEDNESDAY -> "Ср"
        Calendar.THURSDAY -> "Чт"
        Calendar.FRIDAY -> "Пт"
        Calendar.SATURDAY -> "Сб"
        Calendar.SUNDAY -> "Вс"
        else -> "Пн"
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
            .padding(vertical = 4.dp)
            .testTag("product-${product.name}"),

        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (product.checked) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (product.checked) PrimaryGreen else Color.Gray,
            modifier = Modifier
                .size(22.dp)
                .clickable {
                    onProductChecked(product.id, !product.checked)

                }
                .testTag("checkbox-${product.id}")
        )

        Text(
            text = product.icon,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        Text(
            text = product.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (product.checked) PrimaryGreen else TextBlack,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = product.amount,
            style = MaterialTheme.typography.bodyLarge,
            color = if (product.checked) PrimaryGreen else TextBlack,
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
    val actualDates: Set<String> = emptySet() // новое
)