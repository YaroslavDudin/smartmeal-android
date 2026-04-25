package com.example.smartmeal.ui.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.smartmeal.ui.components.feedback.shimmerEffect
import coil.request.ImageRequest
import com.example.smartmeal.R
import com.example.smartmeal.ui.components.buttons.CircleIconButton
import com.example.smartmeal.ui.components.buttons.CircleIconType

import com.example.smartmeal.ui.components.SmartMealText

import com.example.smartmeal.utils.softBottomShadow

import androidx.compose.material3.IconButton

import androidx.compose.foundation.border
import com.example.smartmeal.ui.theme.PrimaryGreen

@Composable
fun MealCard(
    modifier: Modifier = Modifier,
    title: String,
    cookTime: String,
    imageRes: Int = R.drawable.food,
    imageUrl: String? = null,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    isActive: Boolean = false,
    isInMenu: Boolean = false,
    onPlusClick: (() -> Unit)? = null,
    cardTag: String? = null,
    titleTag: String? = null,
    favoriteTag: String? = null,
    plusTag: String? = null
) {
    val resolvedCardTag = cardTag ?: "meal_card"
    val resolvedFavoriteTag = favoriteTag ?: "favorite_button"
    val resolvedPlusTag = plusTag ?: "plus_button"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .softBottomShadow(shape = RoundedCornerShape(16.dp))
            .testTag(resolvedCardTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl ?: imageRes)
                    .crossfade(500)
                    .build(),
                contentDescription = title,
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                },
                error = {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.food),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f)
            ) {
                SmartMealText(
                    text = title,
                    modifier = if (titleTag != null) Modifier.testTag(titleTag) else Modifier,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "cook_time",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SmartMealText(
                        text = cookTime,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                if (onPlusClick != null && isFavorite) {
                    IconButton(
                        onClick = onPlusClick,
                        modifier = Modifier.size(36.dp).testTag(resolvedPlusTag)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isInMenu) R.drawable.ic_plus_active else R.drawable.ic_plus_inactive
                            ),
                            contentDescription = "Add to menu",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    }
                }

                CircleIconButton(
                    iconType = CircleIconType.FAVORITE,
                    isSelected = isFavorite,
                    onClick = onFavoriteClick,
                    backgroundColor = Color.Transparent,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag(resolvedFavoriteTag)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MealCardPreview() {
    MealCard(
        title = "Завтрак \nОвсянка с ягодами",
        cookTime = "15 мин",
        imageRes = R.drawable.food,
        isFavorite = true,
        onFavoriteClick = {}
    )
}