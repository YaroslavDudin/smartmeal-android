package com.example.smartmeal.ui.components.cards

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText

val SoftGreen = Color(0xFF66BB6A)
@Composable
fun BottomNavigationBar(
    selectedItem: Int = 0,
    onItemSelected: (Int) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val items = listOf(
        NavigationItem("Меню", Icons.AutoMirrored.Filled.MenuBook),
        NavigationItem("Продукты", Icons.Default.ShoppingCart),
        NavigationItem("Статистика", Icons.Default.BarChart),
        NavigationItem("Профиль", Icons.Default.Person)
    )

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isLandscape) Modifier.height(56.dp) else Modifier),
        containerColor = Color.White,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
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
                        tint = if (isSelected) SoftGreen else Color.Gray,
                        modifier = if (isLandscape) Modifier.size(20.dp) else Modifier.size(24.dp)
                    )
                },
                label = if (isLandscape) null else {
                    {
                        SmartMealText(
                            text = item.title,
                            color = if (isSelected) SoftGreen else Color.Gray
                        )
                    }
                },
                alwaysShowLabel = !isLandscape,
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
