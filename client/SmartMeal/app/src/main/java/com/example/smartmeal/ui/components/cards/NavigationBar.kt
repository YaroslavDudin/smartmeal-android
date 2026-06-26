package com.example.smartmeal.ui.components.cards

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.theme.SmartMealCardBorder
import com.example.smartmeal.ui.theme.SmartMealTextMuted
import com.example.smartmeal.ui.theme.SmartMealTomato
import com.example.smartmeal.ui.theme.SmartMealTomatoSoft

// ─── Design tokens ────────────────────────────────────────────────────────────

private val NavSurface     = Color(0xFFFFFFFF)
private val NavBorder      = SmartMealCardBorder
private val NavMuted       = SmartMealTextMuted
private val NavActiveColor = SmartMealTomato
private val NavIndicatorBg = SmartMealTomatoSoft

// ─── Data ─────────────────────────────────────────────────────────────────────

data class NavigationItem(
    val title: String,
    val icon: ImageVector
)

// ─── Public composable ────────────────────────────────────────────────────────

/**
 * Premium floating bottom navigation bar.
 *
 * - Floating pill shape with a subtle shadow + hairline border
 * - Soft pill indicator behind the active icon (no ripple bleed outside pill)
 * - Spring-physics icon scale on tap
 * - Tap area == pill indicator area (icon + label zone), not the full cell height
 * - Landscape-aware: hides labels, reduces height
 */
@Composable
fun BottomNavigationBar(
    selectedItem: Int = 0,
    onItemSelected: (Int) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val items = listOf(
        NavigationItem("Меню",       Icons.AutoMirrored.Filled.MenuBook),
        NavigationItem("Продукты",   Icons.Default.ShoppingCart),
        NavigationItem("Статистика", Icons.Default.BarChart),
        NavigationItem("Профиль",    Icons.Default.Person)
    )

    val barHeight: Dp    = if (isLandscape) 56.dp else 74.dp
    val pillWidth: Dp    = if (isLandscape) 40.dp  else 52.dp
    val pillHeight: Dp   = if (isLandscape) 32.dp  else 40.dp
    val iconSize: Dp     = if (isLandscape) 20.dp  else 22.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = NavSurface,
        // Subtle shadow — visible but not overpowering
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = NavBorder.copy(alpha = 0.65f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedItem == index

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.14f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "navIconScale_$index"
                )

                val iconColor = if (isSelected) NavActiveColor else NavMuted
                val interactionSource = remember { MutableInteractionSource() }

                // Each item occupies equal weight in the row.
                // The TOUCH TARGET is the pill-shaped Box — same size as the visual
                // indicator — so tap area == visual feedback area.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(barHeight),
                    contentAlignment = Alignment.Center
                ) {
                    // Pill: acts as both the visual indicator and the bounded tap zone.
                    Column(
                        modifier = Modifier
                            .size(width = pillWidth, height = pillHeight)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) NavIndicatorBg else Color.Transparent)
                            .clickable(
                                interactionSource = interactionSource,
                                // Ripple is bounded to the pill shape
                                indication = ripple(bounded = true, color = NavActiveColor),
                                onClick = { onItemSelected(index) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = iconColor,
                            modifier = Modifier
                                .size(iconSize)
                                .scale(iconScale)
                        )

                        if (!isLandscape) {
                            SmartMealText(
                                text = item.title,
                                color = iconColor,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFFCF8F6)
@Composable
private fun BottomNavigationBarPreview() {
    BottomNavigationBar(selectedItem = 0)
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF8F6)
@Composable
private fun BottomNavigationBarPreview2() {
    BottomNavigationBar(selectedItem = 2)
}
