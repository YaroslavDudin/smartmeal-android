package com.example.smartmeal.ui.components.cards

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.SmartMealBackground
import com.example.smartmeal.ui.theme.SmartMealCardBorder
import com.example.smartmeal.ui.theme.SmartMealSurfaceSoft
import com.example.smartmeal.ui.theme.SmartMealTextMuted

private val NavigationContainer = Color.White
private val NavigationBorder = SmartMealCardBorder
private val NavigationMuted = SmartMealTextMuted

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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 7.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, NavigationBorder.copy(alpha = 0.82f))
    ) {
        NavigationBar(
            modifier = Modifier.height(if (isLandscape) 56.dp else 72.dp),
            containerColor = NavigationContainer,
            tonalElevation = 0.dp
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedItem == index

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onItemSelected(index) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (isSelected) PrimaryGreen else NavigationMuted,
                            modifier = Modifier.size(if (isLandscape) 20.dp else 21.dp)
                        )
                    },
                    label = if (isLandscape) {
                        null
                    } else {
                        {
                            SmartMealText(
                                text = item.title,
                                color = if (isSelected) PrimaryGreen else NavigationMuted,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    },
                    alwaysShowLabel = !isLandscape,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGreen,
                        selectedTextColor = PrimaryGreen,
                        unselectedIconColor = NavigationMuted,
                        unselectedTextColor = NavigationMuted,
                        indicatorColor = SmartMealSurfaceSoft
                    )
                )
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector
)

@Preview(showBackground = true)
@Composable
private fun BottomNavigationBarPreview() {
    BottomNavigationBar(selectedItem = 1)
}
