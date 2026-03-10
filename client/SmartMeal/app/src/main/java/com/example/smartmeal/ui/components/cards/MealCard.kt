package com.example.smartmeal.ui.components.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartmeal.R
import com.example.smartmeal.ui.components.buttons.CircleIconButton
import com.example.smartmeal.ui.components.buttons.CircleIconType

@Composable
fun MealCard(
    modifier: Modifier = Modifier,
    title: String,
    cookTime: String,
    imageRes: Int,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .testTag("meal_card"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = painterResource(imageRes),
                contentDescription = title,
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "cook_time"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(cookTime)
                }
            }

            CircleIconButton(
                iconType = CircleIconType.FAVORITE,
                isSelected = isFavorite,
                onClick = onFavoriteClick,
                backgroundColor = Color.LightGray,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(40.dp)
                    .testTag("favorite_button")
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
fun MealCardPreview() {
    MealCard(
        title = "Овсянка",
        cookTime = "15 мин",
        imageRes = R.drawable.food,
        isFavorite = true,
        onFavoriteClick = {}
    )
}