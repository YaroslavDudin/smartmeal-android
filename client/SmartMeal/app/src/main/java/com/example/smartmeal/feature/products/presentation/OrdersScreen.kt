package com.example.smartmeal.feature.products.presentation

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmeal.data.api.RetrofitClient
import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.theme.BgLightGray
import kotlinx.coroutines.launch
import com.example.smartmeal.R
import androidx.compose.foundation.Image

private val YellowDivider = Color(0xFFD4B800)
private val ButtonYellow = Color(0xFFFEEDAA)

@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    onGoToProducts: () -> Unit,
    startDate: String? = null,
    endDate: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val menuApi = remember { RetrofitClient.createService(MenuApi::class.java) }
    val preferences = remember { com.example.smartmeal.data.local.SetupPreferences(context) }
    var isExporting by remember { mutableStateOf(false) }

    val exportTxtAndShare = {
        if (!isExporting) {
            isExporting = true
            coroutineScope.launch {
                try {
                    // Собираем все переопределенные порции из настроек
                    val itemServings = mutableMapOf<String, Int>()
                    val menuItemsResponse = menuApi.getMenuItems()
                    if (menuItemsResponse.isSuccessful) {
                        menuItemsResponse.body()?.forEach { item ->
                            val servings = preferences.getMenuItemServings(item.id)
                            if (servings > 0) {
                                itemServings[item.id.toString()] = servings
                            }
                        }
                    }

                    menuApi.recalculateCart(
                        com.example.smartmeal.feature.home.data.api.RecalculateCartRequest(
                            start_date = startDate,
                            end_date = endDate,
                            item_servings = itemServings,
                            global_servings = preferences.getPortionSize()
                        )
                    )
                    
                    val response = menuApi.exportCart(
                        all = true, 
                        request = com.example.smartmeal.feature.home.data.api.ExportCartRequest(emptyList())
                    )
                    if (response.isSuccessful) {
                        val txtContent = response.body()?.string() ?: ""
                        
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, txtContent)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Список продуктов")
                        context.startActivity(shareIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isExporting = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLightGray)
    ) {
        // ── Шапка (Стиль как в DietScreen) ──────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clickable { onBack() },
                tint = Color.Black
            )
            SmartMealText(
                text = "Заказы",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Основной контент (Центрируемый и занимающий всё свободное место)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            SmartMealText(
                text = "Заказы продуктов",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── Иконки сервисов ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                ServiceIconPlaceholder(
                    name = "Яндекс\nДоставка",
                    iconRes = R.drawable.yandex_lavka_icon_logo,
                    onClick = exportTxtAndShare
                )
                ServiceIconPlaceholder(
                    name = "Яндекс\nМаркет",
                    iconRes = R.drawable.yandex_market_sign_logo,
                    onClick = exportTxtAndShare
                )
                ServiceIconPlaceholder(
                    name = "Самокат",
                    iconRes = R.drawable.samokat_sign_logo,
                    onClick = exportTxtAndShare
                )
            }

            if (isExporting) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }

            Spacer(modifier = Modifier.height(64.dp))

            // ── Желтая полоска (HorizontalDivider как в DietScreen) ──
            HorizontalDivider(
                color = YellowDivider,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        // ── Кнопка перехода к продуктам (Цвет #FEEDAA, всегда снизу) ──
        Button(
            onClick = onGoToProducts,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonYellow,
                contentColor = Color.Black
            )
        ) {
            SmartMealText(
                text = "Список продуктов",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ServiceIconPlaceholder(
    name: String,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = name,
                modifier = Modifier.size(64.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        SmartMealText(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}
