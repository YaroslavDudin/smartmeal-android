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

import androidx.lifecycle.viewModelScope
import com.example.smartmeal.data.api.RetrofitClient
import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.menu_generator.data.api.GeneratorApi
import com.example.smartmeal.feature.menu_generator.data.models.AutoGenerateRequest
import com.example.smartmeal.feature.menu_generator.data.models.GeneratedMenuDto
import com.example.smartmeal.feature.menu_generator.data.models.GeneratedMenuItemDto
import kotlinx.coroutines.launch
import java.time.LocalDate


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

        if (uiState.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!uiState.hasMenu) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("У вас еще нет меню на эту неделю")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.generateMenu() }) {
                        Text("Сгенерировать меню")
                    }
                }
            }
        } else {
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
        }

        if (uiState.error != null) {
            Text(
                text = uiState.error ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
    val isLoading: Boolean = false,
    val hasMenu: Boolean = false,
    val error: String? = null,
    val selectedDay: String = "Вт",
    val selectedDate: String = "Вторник - 3 марта 2026 г",
    val mealSections: List<MealSection> = emptyList(),
    val fullMenu: GeneratedMenuDto? = null
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val menuApi = RetrofitClient.createService(MenuApi::class.java)
    private val generatorApi = RetrofitClient.createService(GeneratorApi::class.java)

    private val dayToOffset = mapOf(
        "Вт" to 0, "Ср" to 1, "Чт" to 2, "Пт" to 3, "Сб" to 4, "Вс" to 5, "Пн" to 6
    )

    init {
        loadCurrentMenu()
    }

    private fun loadCurrentMenu() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = menuApi.getMenus()
                if (response.isSuccessful) {
                    val menus = response.body()
                    if (!menus.isNullOrEmpty()) {
                        // Для MVP берем самое последнее меню
                        val lastMenuId = menus.last().id
                        fetchMenuDetails(lastMenuId)
                    } else {
                        _uiState.update { it.copy(isLoading = false, hasMenu = false) }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Ошибка загрузки: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun fetchMenuDetails(menuId: Int) {
        // Поскольку getMenu(id) возвращает MenuDto без items, нам нужно либо 
        // использовать getMenuItems() и фильтровать, либо бэкенд должен возвращать с items.
        // Наш бэкенд MenuViewSet.get_queryset использует prefetch_related('items__recipe'),
        // так что getMenu(id) ДОЛЖЕН возвращать элементы, если MenuDto их содержит.
        // Но в Kotlin MenuDto не имеет items. Однако GeneratedMenuDto имеет.
        // Используем хак для MVP: запрашиваем через GeneratorApi (если бы там был GET) 
        // или просто используем getMenuItems().
        
        try {
            val response = menuApi.getMenuItems() // Получаем все элементы
            if (response.isSuccessful) {
                val allItems = response.body() ?: emptyList()
                val menuItems = allItems.filter { it.menu == menuId }
                
                // Преобразуем в GeneratedMenuDto для удобства хранения
                val mockFullMenu = GeneratedMenuDto(
                    id = menuId,
                    period = "week",
                    start_date = "2026-03-03",
                    created_at = "",
                    items = menuItems.map {
                        GeneratedMenuItemDto(
                            id = it.id,
                            recipe = it.recipe,
                            recipe_title = "Рецепт ${it.recipe}", // На бэкенде есть prefetch, но в DTO нет названия
                            day_offset = it.day_offset,
                            meal_type = it.meal_type,
                            actual_date = it.actual_date
                        )
                    }
                )
                
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        hasMenu = true,
                        fullMenu = mockFullMenu
                    )
                }
                updateMealSectionsForSelectedDay()
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    fun generateMenu() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val request = AutoGenerateRequest(
                    period = "week",
                    start_date = "2026-03-03"
                )
                val response = generatorApi.autoGenerate(request)
                if (response.isSuccessful) {
                    val generatedMenu = response.body()
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            hasMenu = true,
                            fullMenu = generatedMenu
                        )
                    }
                    updateMealSectionsForSelectedDay()
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Ошибка генерации: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectDay(day: String) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedDay = day,
                selectedDate = getDateForDay(day)
            )
        }
        updateMealSectionsForSelectedDay()
    }

    private fun updateMealSectionsForSelectedDay() {
        val currentState = _uiState.value
        val offset = dayToOffset[currentState.selectedDay] ?: 0
        val itemsForDay = currentState.fullMenu?.items?.filter { it.day_offset == offset } ?: emptyList()

        val mealSections = itemsForDay.map { item ->
            val title = when(item.meal_type) {
                "breakfast" -> "Завтрак"
                "lunch" -> "Обед"
                "dinner" -> "Ужин"
                else -> item.meal_type.replaceFirstChar { it.uppercase() }
            }
            MealSection(
                id = item.meal_type,
                title = title,
                meal = MealItem(
                    id = item.id.toString(),
                    title = item.recipe_title,
                    cookTime = "20-30 мин", // Заглушка
                    imageRes = R.drawable.food,
                    isFavorite = false
                )
            )
        }.sortedBy { section ->
            // Сортировка по времени приема пищи
            when(section.id) {
                "breakfast" -> 1
                "lunch" -> 2
                "dinner" -> 3
                else -> 4
            }
        }

        _uiState.update { it.copy(mealSections = mealSections) }
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
        // Здесь должен быть вызов API для замены блюда
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
}
