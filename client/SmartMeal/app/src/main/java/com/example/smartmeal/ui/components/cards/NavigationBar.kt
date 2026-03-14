package com.example.smartmeal.ui.components.cards

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val SoftGreen = Color(0xFF66BB6A)
@Composable
fun BottomNavigationBar(
    selectedItem: Int = 0,
    onItemSelected: (Int) -> Unit = {}
) {
    val items = listOf(
        NavigationItem("Меню", Icons.Default.MenuBook),
        NavigationItem("Продукты", Icons.Default.ShoppingCart),
        NavigationItem("Статистика", Icons.Default.BarChart),
        NavigationItem("Профиль", Icons.Default.Person)
    )

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.White
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selectedItem == index

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.title,
                        tint = if (isSelected) SoftGreen else Color.Gray
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        color = if (isSelected) SoftGreen else Color.Gray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SoftGreen,
                    selectedTextColor = SoftGreen,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector
)

@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
    BottomNavigationBar(selectedItem = 3)
}