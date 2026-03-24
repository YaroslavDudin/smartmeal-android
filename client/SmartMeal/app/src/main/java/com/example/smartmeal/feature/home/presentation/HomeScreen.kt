package com.example.smartmeal.feature.home.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartmeal.data.api.RetrofitClient
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.home.data.MenuRepository
import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.data.menu.MenuDto
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import com.example.smartmeal.feature.menu_generator.data.api.GeneratorApi
import com.example.smartmeal.feature.menu_generator.data.models.AutoGenerateRequest
import com.example.smartmeal.feature.products.presentation.ProductListViewModel
import com.example.smartmeal.feature.products.presentation.ProductListScreen
import com.example.smartmeal.ui.components.SmartMealText
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onLogout: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onRecipeClick: (Int) -> Unit,
) {

    val menuApi = remember { RetrofitClient.createService(MenuApi::class.java) }

    val productListViewModel: ProductListViewModel = viewModel(
        factory = remember { ProductListViewModelFactory(menuApi) }
    )

    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val setupPreferences = remember { SetupPreferences(context) }
    val planType = setupPreferences.getPlanType()
    val planRange = setupPreferences.getCustomPlanRange()
    val customPlan = if (planType == SetupPreferences.PLAN_TYPE_CUSTOM) {
        planRange?.let { (start, end) -> CustomPlan(Date(start), Date(end)) }
    } else {
        null
    }

    LaunchedEffect(customPlan?.startDate?.time, customPlan?.endDate?.time, uiState.selectedDateFromPlan) {
        if (customPlan == null) {
            viewModel.clearPlanSelection()
            return@LaunchedEffect
        }

        val selected = uiState.selectedDate
        val outOfRange = selected == null ||
            selected.before(customPlan.startDate) ||
            selected.after(customPlan.endDate)

        if (!uiState.selectedDateFromPlan || outOfRange) {
            viewModel.selectDate(customPlan.startDate, customPlan)
        }
    }

    val isLoadingProducts = productListViewModel.isLoading

    var selectedNavItem by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.allMenuItems) {
        uiState.allMenuItems.takeIf { it.isNotEmpty() }?.let {
            productListViewModel.generateProductsFromMenuItems(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onCartUpdated = {
            viewModel.uiState.value.allMenuItems.takeIf { it.isNotEmpty() }?.let { menuItems ->
                productListViewModel.generateProductsFromMenuItems(menuItems)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedNavItem,
                onItemSelected = { selectedNavItem = it }
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {

            when (selectedNavItem) {

                // ---------------- ГЛАВНАЯ ----------------
                0 -> HomeContent(
                    uiState = uiState,
                    onDaySelected = { viewModel.selectDay(it, customPlan) },
                    onGenerateMenu = {
                        val storedPlanType = setupPreferences.getPlanType()
                        val range = setupPreferences.getCustomPlanRange()?.let { (start, end) ->
                            Date(start) to Date(end)
                        }
                        viewModel.generateMenu(storedPlanType, range)
                    },
                    onReplaceMeal = { viewModel.replaceMeal(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onRecipeClick = onRecipeClick,
                    onDateSelectedFromPlan = { viewModel.selectDate(it, customPlan) },
                    customPlan = customPlan
                )

                // ---------------- ПРОДУКТЫ ----------------
                1 -> {
                    if (isLoadingProducts) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        ProductListScreen(
                            products = productListViewModel.products,
                            selectedDate = uiState.selectedDate,
                            onDaySelected = { day -> viewModel.selectDay(day, customPlan) },
                            onProductChecked = { productId, checked -> productListViewModel.onProductChecked(productId, checked) },
                            onCheckAll = { productListViewModel.toggleCheckAllProducts() }
                        )
                    }
                }

                // ---------------- СТАТИСТИКА ----------------
                2 -> StatisticsScreen()

                // ---------------- ПРОФИЛЬ ----------------
                3 -> ProfileScreen(
                    onLogout = onLogout,
                    onLogoutSuccess = onLogoutSuccess
                )
            }
        }
    }
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onDaySelected: (String) -> Unit,
    onGenerateMenu: () -> Unit,
    onReplaceMeal: (String) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onRecipeClick: (Int) -> Unit,
    onDateSelectedFromPlan: (Date) -> Unit,
    customPlan: CustomPlan?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
    ) {
        SmartMealText(
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

        SmartMealText(
            text = uiState.selectedDateDisplay,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .testTag("home_date"),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (customPlan != null) {
            MyPlanSection(
                customPlan = customPlan,
                onDateSelectedFromPlan = onDateSelectedFromPlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("home_my_plan")
            )
        }

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
                    SmartMealText("У вас еще нет меню на эту неделю")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onGenerateMenu,
                        modifier = Modifier.testTag("home_generate_button")
                    ) {
                        SmartMealText("Сгенерировать меню")
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
                items(uiState.mealSections, key = { it.id }) { section ->
                    MealSection(
                        sectionId = section.id,
                        title = section.title,
                        meal = section.meal,
                        onReplaceClick = { onReplaceMeal(section.id) },
                        onFavoriteClick = { onToggleFavorite(section.meal.id) },
                        onRecipeClick = onRecipeClick
                    )

                }
            }
        }

        if (uiState.error != null) {
            SmartMealText(
                text = uiState.error ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun MealSection(
    sectionId: String,
    title: String,
    meal: MenuItemDto,
    onReplaceClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onRecipeClick: (Int) -> Unit,
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
            SmartMealText(text = title)
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

        AnimatedContent(
            targetState = meal,
            transitionSpec = {
                (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.95f)) togetherWith
                        (fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 0.95f))
            },
            label = "MealReplacementAnimation"
        ) { item ->
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable { onRecipeClick(item.recipe) }
            ) {
                MealCard(
                    title = item.recipe_title,
                    cookTime = "${item.cook_time} мин",
                    imageUrl = item.image_url,
                    isFavorite = false,
                    onFavoriteClick = onFavoriteClick
                )
            }
        }
    }
}

@Composable
fun StatisticsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SmartMealText("Статистика – в разработке")
    }
}

@Composable
fun ProfileScreen(onLogout: () -> Unit, onLogoutSuccess: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SmartMealText("Профиль – в разработке")
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onLogout()
                    onLogoutSuccess()
                }
            ) {
                SmartMealText("Выйти из аккаунта")
            }
        }
    }
}

// Data-классы
data class MealSection(
    val id: String,
    val title: String,
    val meal: MenuItemDto
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val hasMenu: Boolean = false,
    val error: String? = null,
    val selectedDay: String = "",
    val selectedDate: Date? = null,
    val selectedDateFromPlan: Boolean = false,
    val selectedDateDisplay: String = "",
    val mealSections: List<MealSection> = emptyList(),
    val currentMenu: MenuDto? = null,
    val allMenuItems: List<MenuItemDto> = emptyList(),
    val customPlan: CustomPlan? = null
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val menuRepository = MenuRepository(RetrofitClient.createService(MenuApi::class.java))
    private val generatorApi = RetrofitClient.createService(GeneratorApi::class.java)
    private val menuApi: MenuApi = RetrofitClient.createService(MenuApi::class.java)
    var onCartUpdated: (() -> Unit)? = null
    private val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    private val displayDateFormatter = SimpleDateFormat("EEEE - d MMMM yyyy 'г.'", Locale("ru"))
    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
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
            refreshMenu()
        }
    }

    private suspend fun refreshMenu(): MenuDto? {
        _uiState.update { it.copy(isLoading = true, error = null) }
        return try {
            val menu = menuRepository.getLatestMenu()
            val allItemsResponse = menuApi.getMenuItems()
            val allItems = if (allItemsResponse.isSuccessful) allItemsResponse.body() ?: emptyList() else emptyList()
            if (menu != null) {
                val startDate = apiDateFormatter.parse(menu.start_date) ?: Date()
                val maxOffset = menu.items?.maxOfOrNull { it.day_offset } ?: 0
                val endDate = startDate.let {
                    val cal = Calendar.getInstance().apply { time = it }
                    cal.add(Calendar.DAY_OF_YEAR, maxOffset)
                    cal.time
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasMenu = true,
                        currentMenu = menu,
                        customPlan = CustomPlan(startDate, endDate),
                        allMenuItems = allItems
                    )
                }
                updateMealSections()
            } else {
                _uiState.update { it.copy(isLoading = false, hasMenu = false, allMenuItems = allItems) }
            }
            menu
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "Ошибка: ${e.localizedMessage}") }
            null
        }
    }

    fun generateMenu(
        planType: String?,
        customRange: Pair<Date, Date>?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val type = planType ?: SetupPreferences.PLAN_TYPE_WEEKLY
                when (type) {
                    SetupPreferences.PLAN_TYPE_CUSTOM -> {
                        val range = customRange
                        if (range != null) {
                            val cal = Calendar.getInstance().apply { time = range.first }
                            val end = range.second
                            while (!cal.time.after(end)) {
                                val dayStr = apiDateFormatter.format(cal.time)
                                val response = generatorApi.autoGenerate(
                                    AutoGenerateRequest(period = "day", start_date = dayStr)
                                )
                                if (!response.isSuccessful) {
                                    _uiState.update { it.copy(error = "Ошибка генерации меню на $dayStr") }
                                }
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }
                    }
                    SetupPreferences.PLAN_TYPE_DAILY -> {
                        val todayStr = apiDateFormatter.format(Date())
                        val response = generatorApi.autoGenerate(
                            AutoGenerateRequest(period = "day", start_date = todayStr)
                        )
                        if (!response.isSuccessful) {
                            _uiState.update { it.copy(error = "Ошибка генерации") }
                        }
                    }
                    else -> {
                        val todayStr = apiDateFormatter.format(Date())
                        val response = generatorApi.autoGenerate(
                            AutoGenerateRequest(period = "week", start_date = todayStr)
                        )
                        if (!response.isSuccessful) {
                            _uiState.update { it.copy(error = "Ошибка генерации") }
                        }
                    }
                }

                val menu = refreshMenu()
                if (menu != null && !menu.items.isNullOrEmpty()) {
                    for (item in menu.items) {
                        val recipeResponse = menuApi.getRecipe(item.recipe)
                        if (!recipeResponse.isSuccessful) {
                            println("Ошибка загрузки рецепта ${item.recipe}: ${recipeResponse.code()}")
                        }
                    }
                    onCartUpdated?.invoke()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectDay(day: String, customPlan: CustomPlan?) {
        val state = _uiState.value
        val selectedDate = if (state.selectedDateFromPlan && state.selectedDate != null) {
            val calendar = Calendar.getInstance().apply { time = state.selectedDate }
            val baseIndex = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
            val targetIndex = dayNames.indexOf(day).coerceAtLeast(0)
            val diff = targetIndex - baseIndex
            calendar.add(Calendar.DAY_OF_YEAR, diff)
            val candidate = normalizeDate(calendar.time)
            if (customPlan != null) {
                val start = normalizeDate(customPlan.startDate)
                val end = normalizeDate(customPlan.endDate)
                if (candidate.before(start) || candidate.after(end)) {
                    return
                }
            }
            candidate
        } else {
            val menu = state.currentMenu
            menu?.let { dateForSelectedDay(it, day) }
        }
        _uiState.update {
            it.copy(
                selectedDay = day,
                selectedDate = selectedDate,
                selectedDateFromPlan = it.selectedDateFromPlan && selectedDate != null
            )
        }
        updateMealSections()
    }

    private fun updateMealSections() {
        val state = _uiState.value
        val menu = state.currentMenu ?: return

        try {
            val resolvedDate = state.selectedDate ?: dateForSelectedDay(menu, state.selectedDay)

            val itemsForDay = if (resolvedDate != null) {
                val selectedDateStr = apiDateFormatter.format(resolvedDate)
                if (state.selectedDateFromPlan) {
                    state.allMenuItems.filter { it.actual_date == selectedDateStr }
                } else {
                    menu.items?.filter { it.actual_date == selectedDateStr } ?: emptyList()
                }
            } else {
                emptyList()
            }

            val displayDate = resolvedDate?.let { displayDateFormatter.format(it) } ?: ""

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
                    meal = item
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
                selectedDateDisplay = displayDate,
                selectedDate = resolvedDate
            ) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Ошибка обработки даты: ${e.localizedMessage}") }
        }
    }

    fun toggleFavorite(mealId: Int) {
        // Логика избранного
    }

    fun selectDate(date: Date, customPlan: CustomPlan?) {
        val normalized = normalizeDate(date)
        if (customPlan != null) {
            val start = normalizeDate(customPlan.startDate)
            val end = normalizeDate(customPlan.endDate)
            if (normalized.before(start) || normalized.after(end)) {
                return
            }
        }
        val calendar = Calendar.getInstance()
        calendar.time = normalized

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

        val dayName = dayNames[dayIndex]
        _uiState.update { it.copy(selectedDay = dayName, selectedDate = normalized, selectedDateFromPlan = true) }
        updateMealSections()
    }

    fun clearPlanSelection() {
        _uiState.update { it.copy(selectedDateFromPlan = false, selectedDate = null) }
        updateMealSections()
    }

    private fun normalizeDate(date: Date): Date {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.time
    }

    private fun dateForSelectedDay(menu: MenuDto, selectedDay: String): Date? {
        val startDate = apiDateFormatter.parse(menu.start_date) ?: return null
        val startCalendar = Calendar.getInstance().apply { time = startDate }
        val startDayIndex = when(startCalendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        val selectedDayOfWeekIndex = dayNames.indexOf(selectedDay)
        if (selectedDayOfWeekIndex < 0) return null
        var offset = selectedDayOfWeekIndex - startDayIndex
        if (offset < 0) offset += 7
        val calendar = Calendar.getInstance().apply { time = startDate }
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        return calendar.time
    }

    fun replaceMeal(mealType: String) {
        val state = _uiState.value
        val menu = state.currentMenu ?: return

        val selectedDayOfWeekIndex = dayNames.indexOf(state.selectedDay)
        val startDate = try { apiDateFormatter.parse(menu.start_date) } catch (e: Exception) { null } ?: Date()
        val startCalendar = Calendar.getInstance().apply { time = startDate }
        val startDayIndex = when(startCalendar.get(Calendar.DAY_OF_WEEK)) {
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

        val menuItem = menu.items?.find { it.day_offset == offset && it.meal_type == mealType } ?: return

        viewModelScope.launch {
            try {
                val updatedItem = menuRepository.replaceMenuItem(menuItem.id)
                if (updatedItem != null) {
                    refreshMenu() // перезагрузит currentMenu и allMenuItems
                    onCartUpdated?.invoke()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }


    }

}

private class ProductListViewModelFactory(
    private val menuApi: MenuApi
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductListViewModel(menuApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
