package com.example.smartmeal.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*

import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import com.example.smartmeal.ui.components.cards.MealCard

import com.example.smartmeal.ui.components.selectors.DaySelector
import com.example.smartmeal.ui.components.cards.BottomNavigationBar
import com.example.smartmeal.ui.theme.SmartMealTheme


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Alignment

import androidx.lifecycle.viewmodel.compose.viewModel



import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.smartmeal.R
import com.example.smartmeal.ui.components.buttons.CircleIconButton
import com.example.smartmeal.ui.components.buttons.CircleIconType



@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onLogout: () -> Unit
    ) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = "Меню",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        DaySelector(
            selectedDay = uiState.selectedDay,
            onDaySelected = { day -> viewModel.selectDay(day) }
        )

        Text(
            text = uiState.selectedDate,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

            LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(uiState.mealSections) { section ->
                MealSection(
                    title = section.title,
                    meal = section.meal,
                    onReplaceClick = { viewModel.replaceMeal(section.id) },
                    onFavoriteClick = { viewModel.toggleFavorite(section.meal.id) }
                )
            }
            
        }
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(text = "Выйти из аккаунта")
        }

        // Нижняя навигация
        BottomNavigationBar(
            selectedItem = 0,
            onItemSelected = {}
        )
    }
}

@Composable
fun MealSection(
    title: String,
    meal: MealItem,
    onReplaceClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Text(text = title)
            Spacer(modifier = Modifier.width(8.dp))
            CircleIconButton(
                iconType = CircleIconType.REPLACE,
                onClick = onReplaceClick,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        MealCard(
            title = meal.title,
            cookTime = meal.cookTime,
            imageRes = meal.imageRes,
            isFavorite = meal.isFavorite,
            onFavoriteClick = onFavoriteClick
        )
    }
}

data class MealItem(
    val id: String,
    val title: String,
    val cookTime: String,
    val imageRes: Int,
    val isFavorite: Boolean = false
)

data class MealSection(
    val id: String,
    val title: String,
    val meal: MealItem
)

data class HomeUiState(
    val selectedDay: String = "Вт",
    val selectedDate: String = "Вторник - 3 марта 2026 г",
    val mealSections: List<MealSection> = listOf(
        MealSection(
            id = "breakfast",
            title = "Завтрак",
            meal = MealItem(
                id = "breakfast_meal",
                title = "Овсянка с ягодами",
                cookTime = "15 мин",
                imageRes = R.drawable.food,
                isFavorite = true
            )
        ),
        MealSection(
            id = "lunch",
            title = "Обед",
            meal = MealItem(
                id = "lunch_meal",
                title = "Куриный суп с лапшой",
                cookTime = "25 мин",
                imageRes = R.drawable.food,
                isFavorite = false
            )
        ),
        MealSection(
            id = "dinner",
            title = "Ужин",
            meal = MealItem(
                id = "dinner_meal",
                title = "Лосось на гриле",
                cookTime = "30 мин",
                imageRes = R.drawable.food,
                isFavorite = false
            )
        )
    )
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun selectDay(day: String) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedDay = day,
                selectedDate = getDateForDay(day)
            )
        }
    }

    fun toggleFavorite(mealId: String) {
        _uiState.update { currentState ->
            val updatedSections = currentState.mealSections.map { section ->
                if (section.meal.id == mealId) {
                    section.copy(
                        meal = section.meal.copy(isFavorite = !section.meal.isFavorite)
                    )
                } else section
            }
            currentState.copy(mealSections = updatedSections)
        }
    }

    fun replaceMeal(sectionId: String) {
        _uiState.update { currentState ->
            val updatedSections = currentState.mealSections.map { section ->
                if (section.id == sectionId) {
                    section.copy(
                        meal = section.meal.copy(
                            title = getRandomMealForSection(sectionId),
                        )
                    )
                } else section
            }
            currentState.copy(mealSections = updatedSections)
        }
    }

    private fun getDateForDay(day: String): String {
        return when(day) {
            "Вт" -> "Вторник - 3 марта 2026 г"
            "Ср" -> "Среда - 4 марта 2026 г"
            "Чт" -> "Четверг - 5 марта 2026 г"
            "Пт" -> "Пятница - 6 марта 2026 г"
            "Сб" -> "Суббота - 7 марта 2026 г"
            "Вс" -> "Воскресенье - 8 марта 2026 г"
            "Пн" -> "Понедельник - 9 марта 2026 г"
            else -> "Вторник - 3 марта 2026 г"
        }
    }

    private fun getRandomMealForSection(sectionId: String): String {
        val breakfastMeals = listOf("Овсянка с ягодами", "Яичница с беконом", "Сырники со сметаной")
        val lunchMeals = listOf("Куриный суп", "Борщ", "Паста Карбонара")
        val dinnerMeals = listOf("Лосось на гриле", "Стейк с овощами", "Курица с рисом")

        return when(sectionId) {
            "breakfast" -> breakfastMeals.random()
            "lunch" -> lunchMeals.random()
            "dinner" -> dinnerMeals.random()
            else -> "Блюдо"
        }
    }
}
