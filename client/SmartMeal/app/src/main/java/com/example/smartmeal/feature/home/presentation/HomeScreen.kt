package com.example.smartmeal.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartmeal.R
import com.example.smartmeal.data.api.RetrofitClient
import com.example.smartmeal.feature.home.data.MenuRepository
import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.data.menu.MenuDto
import com.example.smartmeal.feature.menu_generator.data.api.GeneratorApi
import com.example.smartmeal.feature.menu_generator.data.models.AutoGenerateRequest
import com.example.smartmeal.ui.components.buttons.CircleIconButton
import com.example.smartmeal.ui.components.buttons.CircleIconType
import com.example.smartmeal.ui.components.cards.BottomNavigationBar
import com.example.smartmeal.ui.components.cards.MealCard
import com.example.smartmeal.ui.components.selectors.DaySelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreenContent(
        modifier = modifier,
        uiState = uiState,
        onDaySelected = { day -> viewModel.selectDay(day) },
        onGenerateMenu = { viewModel.generateMenu() },
        onReplaceMeal = { id -> viewModel.replaceMeal(id) },
        onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
        onLogout = onLogout
    )
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    onDaySelected: (String) -> Unit,
    onGenerateMenu: () -> Unit,
    onReplaceMeal: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onLogout: () -> Unit
) {
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
                .padding(top = 16.dp, bottom = 8.dp)
                .testTag("home_title"),
            color = MaterialTheme.colorScheme.onBackground
        )

        Box(modifier = Modifier.testTag("home_day_selector")) {
            DaySelector(
                selectedDay = uiState.selectedDay,
                onDaySelected = onDaySelected
            )
        }

        Text(
            text = uiState.selectedDateDisplay,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .testTag("home_date"),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.testTag("home_loading"))
            }
        } else if (!uiState.hasMenu) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.testTag("home_empty_state")
                ) {
                    Text("У вас еще нет меню на эту неделю")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onGenerateMenu,
                        modifier = Modifier.testTag("home_generate_button")
                    ) {
                        Text("Сгенерировать меню")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_meal_list")
            ) {
                items(uiState.mealSections) { section ->
                    MealSection(
                        sectionId = section.id,
                        title = section.title,
                        meal = section.meal,
                        onReplaceClick = { onReplaceMeal(section.id) },
                        onFavoriteClick = { onToggleFavorite(section.meal.id) },
                        modifier = Modifier.testTag("home_section_${section.id}")
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("home_logout_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(text = "Выйти из аккаунта")
        }

        BottomNavigationBar(
            selectedItem = 0,
            onItemSelected = {}
        )
    }
}

@Composable
fun MealSection(
    sectionId: String,
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
                modifier = Modifier
                    .size(36.dp)
                    .testTag("home_replace_${sectionId}")
            )
        }

        MealCard(
            title = meal.title,
            cookTime = meal.cookTime,
            imageRes = meal.imageRes,
            isFavorite = meal.isFavorite,
            onFavoriteClick = onFavoriteClick,
            cardTag = "home_meal_card_${meal.id}",
            titleTag = "home_meal_title_${meal.id}",
            favoriteTag = "home_favorite_${meal.id}"
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
    val selectedDay: String = "",
    val selectedDateDisplay: String = "",
    val mealSections: List<MealSection> = emptyList(),
    val currentMenu: MenuDto? = null
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val menuRepository = MenuRepository(RetrofitClient.createService(MenuApi::class.java))
    private val generatorApi = RetrofitClient.createService(GeneratorApi::class.java)

    private val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    private val displayDateFormatter = SimpleDateFormat("EEEE - d MMMM yyyy 'г.'", Locale("ru"))
    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        // Устанавливаем текущий день недели как выбранный по умолчанию
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayIndex = when(dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        _uiState.update { it.copy(selectedDay = dayNames[dayIndex]) }
        loadCurrentMenu()
    }

    private fun loadCurrentMenu() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val menu = menuRepository.getLatestMenu()
                if (menu != null) {
                    _uiState.update { it.copy(
                        isLoading = false,
                        hasMenu = true,
                        currentMenu = menu
                    ) }
                    updateMealSections()
                } else {
                    _uiState.update { it.copy(isLoading = false, hasMenu = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Ошибка: ${e.localizedMessage}") }
            }
        }
    }

    fun generateMenu() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val todayStr = apiDateFormatter.format(Date())
                val request = AutoGenerateRequest(
                    period = "week",
                    start_date = todayStr
                )
                val response = generatorApi.autoGenerate(request)
                if (response.isSuccessful) {
                    loadCurrentMenu()
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Ошибка генерации") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun selectDay(day: String) {
        _uiState.update { it.copy(selectedDay = day) }
        updateMealSections()
    }

    private fun updateMealSections() {
        val state = _uiState.value
        val menu = state.currentMenu ?: return

        try {
            val startDate = apiDateFormatter.parse(menu.start_date) ?: Date()
            val calendar = Calendar.getInstance()
            calendar.time = startDate

            // Определяем смещение выбранного дня относительно понедельника
            val selectedDayOfWeekIndex = dayNames.indexOf(state.selectedDay)

            // Определяем смещение начала меню относительно понедельника
            val startCalendar = Calendar.getInstance()
            startCalendar.time = startDate
            val startDayOfWeek = startCalendar.get(Calendar.DAY_OF_WEEK)
            val startDayIndex = when(startDayOfWeek) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }

            var offset = selectedDayOfWeekIndex - startDayIndex
            if (offset < 0) offset += 7

            val itemsForDay = menu.items?.filter { it.day_offset == offset } ?: emptyList()

            val displayCalendar = Calendar.getInstance()
            displayCalendar.time = startDate
            displayCalendar.add(Calendar.DAY_OF_YEAR, offset)
            val displayDate = displayDateFormatter.format(displayCalendar.time)

            val mealSections = itemsForDay.map { item ->
                val title = when(item.meal_type) {
                    "breakfast" -> "Завтрак"
                    "lunch" -> "Обед"
                    "dinner" -> "Ужин"
                    else -> item.meal_type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
                MealSection(
                    id = item.meal_type,
                    title = title,
                    meal = MealItem(
                        id = item.id.toString(),
                        title = item.recipe_title,
                        cookTime = "25-30 мин",
                        imageRes = R.drawable.food,
                        isFavorite = false
                    )
                )
            }.sortedBy { section ->
                when(section.id) {
                    "breakfast" -> 1
                    "lunch" -> 2
                    "dinner" -> 3
                    else -> 4
                }
            }

            _uiState.update { it.copy(
                mealSections = mealSections,
                selectedDateDisplay = displayDate
            ) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Ошибка обработки даты: ${e.localizedMessage}") }
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

    fun replaceMeal(id: String) {
        _uiState.update { currentState ->
            val updatedSections = currentState.mealSections.map { section ->
                if (section.id == id) {
                    val currentTitle = section.meal.title
                    val nextTitle = if (currentTitle.endsWith(" (замена)")) {
                        currentTitle
                    } else {
                        "$currentTitle (замена)"
                    }
                    section.copy(meal = section.meal.copy(title = nextTitle))
                } else {
                    section
                }
            }
            currentState.copy(mealSections = updatedSections)
        }
    }
}
