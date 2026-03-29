package com.example.smartmeal.feature.home.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
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
import com.example.smartmeal.feature.statistics.presentation.StatisticsScreen
import com.example.smartmeal.feature.profile.presentation.ProfileScreen
import com.example.smartmeal.feature.profile.presentation.ProfileViewModel
import com.example.smartmeal.feature.setup.data.api.SetupApi
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.buttons.CircleIconButton
import com.example.smartmeal.ui.components.buttons.CircleIconType
import com.example.smartmeal.ui.components.cards.BottomNavigationBar
import com.example.smartmeal.ui.components.cards.MealCard
import com.example.smartmeal.ui.components.selectors.DateSelector
import com.example.smartmeal.ui.components.selectors.buildDateSelectorId
import com.example.smartmeal.ui.components.selectors.buildDateSelectorItems
import com.example.smartmeal.ui.components.selectors.formatSelectedDateLabel
import com.example.smartmeal.feature.home.presentation.MyPlanSection
import com.example.smartmeal.ui.theme.PrimaryGreen
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
    onLogout: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onRecipeClick: (Int, Int?) -> Unit,
) {
    val menuApi = remember { RetrofitClient.createService(MenuApi::class.java) }
    val setupApi = remember { RetrofitClient.createService(SetupApi::class.java) }
    val context = LocalContext.current
    val setupPreferences = remember { SetupPreferences(context) }

    val viewModel: HomeViewModel = viewModel(
        factory = remember { HomeViewModelFactory(setupPreferences) }
    )
    
    val productListViewModel: ProductListViewModel = viewModel(
        factory = remember { ProductListViewModelFactory(menuApi, setupPreferences) }
    )

    // Профиль теперь использует настоящий ViewModel Александра
    val profileViewModel: ProfileViewModel = viewModel(
        factory = remember {
            ProfileViewModelFactory(
                api = setupApi,
                preferences = setupPreferences,
                onProfileUpdated = { viewModel.reloadMenu() }
            )
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val planType = setupPreferences.getPlanType()
    val planRange = setupPreferences.getCustomPlanRange()
    val selectedPlanDateMillis = setupPreferences.getSelectedPlanDate()
    
    // ТВОЯ ЛОГИКА ПЛАНОВ
    val customPlan = remember(planType, planRange, selectedPlanDateMillis) {
        when (planType) {
            SetupPreferences.PLAN_TYPE_CUSTOM -> {
                planRange?.let { (start, end) -> CustomPlan(Date(start), Date(end)) }
            }
            SetupPreferences.PLAN_TYPE_WEEKLY -> {
                selectedPlanDateMillis?.let { start ->
                    val end = Calendar.getInstance().apply {
                        time = Date(start)
                        add(Calendar.DATE, 6)
                    }.timeInMillis
                    CustomPlan(Date(start), Date(end))
                }
            }
            SetupPreferences.PLAN_TYPE_DAILY -> {
                selectedPlanDateMillis?.let { start ->
                    CustomPlan(Date(start), Date(start))
                }
            }
            else -> null
        }
    }

    LaunchedEffect(customPlan) {
        if (customPlan != null) {
            val selected = uiState.selectedDate
            val outOfRange = selected == null ||
                selected.before(normalizeDateStatic(customPlan.startDate)) ||
                selected.after(normalizeDateStatic(customPlan.endDate))

            if (!uiState.selectedDateFromPlan || outOfRange) {
                viewModel.selectDate(customPlan.startDate, customPlan)
            }
        }
    }

    var selectedNavItem by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedNavItem, uiState.allMenuItems) {
        if (selectedNavItem == 1) {
            if (uiState.allMenuItems.isNotEmpty()) {
                productListViewModel.generateProductsFromMenuItems(uiState.allMenuItems)
            } else {
                productListViewModel.clearProducts()
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
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedNavItem) {
                // ── ГЛАВНАЯ (ТВОЯ ЛОГИКА) ───────────────────────────────────
                0 -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            onClick = { viewModel.dismissError() },
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        )
                ) {
                    HomeContent(
                        uiState = uiState,
                        onDateSelected = { viewModel.selectDate(it, customPlan) },
                        onGenerateMenu = {
                            val storedPlanType = setupPreferences.getPlanType()
                            val range = setupPreferences.getCustomPlanRange()
                            val selDateMillis = setupPreferences.getSelectedPlanDate()
                            
                            var finalStartDate: Date? = null
                            var customDays: Int? = null
                            
                            when (storedPlanType) {
                                SetupPreferences.PLAN_TYPE_CUSTOM -> {
                                    if (range != null) {
                                        finalStartDate = Date(range.first)
                                        val diff = range.second - range.first
                                        customDays = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
                                    }
                                }
                                SetupPreferences.PLAN_TYPE_WEEKLY, SetupPreferences.PLAN_TYPE_DAILY -> {
                                    if (selDateMillis != null) {
                                        finalStartDate = Date(selDateMillis)
                                    }
                                }
                            }
                            
                            val startDateToPass = finalStartDate ?: Date()
                            viewModel.generateMenu(storedPlanType, startDateToPass, customDays)
                        },
                        onReplaceMeal = { viewModel.replaceMeal(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onRecipeClick = onRecipeClick,
                        onDateSelectedFromPlan = { viewModel.selectDate(it, customPlan) },
                        customPlan = customPlan
                    )
                }

                // ── ПРОДУКТЫ ───────────────────────────────────────────────
                1 -> {
                    ProductListScreen(
                        products = productListViewModel.products,
                        selectedDate = uiState.selectedDate,
                        selectedStartDateKey = productListViewModel.selectedStartDateKey,
                        selectedEndDateKey = productListViewModel.selectedEndDateKey,
                        dateRangeText = productListViewModel.dateRangeText,
                        onDateSelected = { dateKey -> productListViewModel.selectDateRange(dateKey) },
                        onProductChecked = { productIds, checked ->
                            productListViewModel.onProductChecked(productIds, checked)
                        },
                        onCheckAll = { productIds, checked ->
                            productListViewModel.toggleCheckAllProducts(productIds, checked)
                        },
                        isLoading = productListViewModel.isLoading,
                        errorMessage = productListViewModel.errorMessage,
                        customPlan = customPlan
                    )
                }

                // ── СТАТИСТИКА ─────────────────────────────────────────────
                2 -> StatisticsScreen()

                // ── ПРОФИЛЬ (АЛЕКСАНДР) ────────────────────────────────────
                3 -> {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onLogout = onLogout,
                        onLogoutSuccess = onLogoutSuccess,
                        onGoToProducts = { selectedNavItem = 1 },
                        onRecipeClick = { recipeId -> onRecipeClick(recipeId, null) }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onDateSelected: (Date) -> Unit,
    onGenerateMenu: () -> Unit,
    onReplaceMeal: (String) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onRecipeClick: (Int, Int?) -> Unit,
    onDateSelectedFromPlan: (Date) -> Unit,
    customPlan: CustomPlan?
) {
    val availableDates = remember(uiState.currentMenu?.items, uiState.allMenuItems, customPlan) {
        val sourceItems = if (customPlan != null) uiState.allMenuItems
                         else uiState.currentMenu?.items ?: emptyList()
        buildAvailableDates(sourceItems, customPlan)
    }

    val dateSelectorItems = remember(availableDates) { buildDateSelectorItems(availableDates) }
    val selectedDateId = uiState.selectedDate?.let { buildDateSelectorId(it) }
    val hasSingleAvailableDate = availableDates.size == 1

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
        SmartMealText(
            text = "Меню",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp).testTag("home_title"),
            color = MaterialTheme.colorScheme.onBackground
        )

        val monthYearLabel = availableDates.firstOrNull()?.let {
            formatMonthYearForSelector(uiState.selectedDate ?: it)
        }.orEmpty()

        if (monthYearLabel.isNotBlank() && !hasSingleAvailableDate) {
            SmartMealText(
                text = monthYearLabel,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("home_month_year"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Box(modifier = Modifier.testTag("home_day_selector")) {
            if (hasSingleAvailableDate) {
                SmartMealText(
                    text = formatSelectedDateLabel(availableDates.first()),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp)
                        .testTag("home_selected_date_summary"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                DateSelector(
                    items = dateSelectorItems,
                    selectedStartId = selectedDateId,
                    onItemClick = { dateId ->
                        availableDates.firstOrNull { buildDateSelectorId(it) == dateId }?.let(onDateSelected)
                    }
                )
            }
        }

        val isMultiDayPlan = customPlan != null && customPlan.startDate.time != customPlan.endDate.time
        if (isMultiDayPlan) {
            MyPlanSection(
                customPlan = customPlan,
                selectedDate = uiState.selectedDate,
                onDateSelectedFromPlan = onDateSelectedFromPlan,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("home_my_plan")
            )
        }

        if (uiState.isLoading && !uiState.hasMenu) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.testTag("home_loading"))
            }
        } else if (!uiState.hasMenu) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.testTag("home_empty_state")
                ) {
                    SmartMealText("У вас еще нет меню на эту неделю")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onGenerateMenu,
                        modifier = Modifier.testTag("home_generate_button"),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp)
                    ) {
                        SmartMealText("Сгенерировать меню")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                modifier = Modifier.weight(1f).testTag("home_meal_list")
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
    onRecipeClick: (Int, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
            SmartMealText(text = title)
            Spacer(modifier = Modifier.width(8.dp))
            CircleIconButton(
                iconType = CircleIconType.REPLACE,
                onClick = onReplaceClick,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp).testTag("home_replace_${sectionId}")
            )
        }

        AnimatedContent<MenuItemDto>(
            targetState = meal,
            transitionSpec = {
                (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.95f)) togetherWith
                        (fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 0.95f))
            },
            label = "MealReplacementAnimation"
        ) { item ->
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .clickable { onRecipeClick(item.recipe, item.id) }
            ) {
                MealCard(
                    title = item.recipe_title,
                    cookTime = "${item.cook_time} мин",
                    imageUrl = item.image_url,
                    isFavorite = item.is_favorite,
                    onFavoriteClick = onFavoriteClick
                )
            }
        }
    }
}

data class MealSection(val id: String, val title: String, val meal: MenuItemDto)

data class HomeUiState(
    val isLoading: Boolean = false,
    val hasMenu: Boolean = false,
    val error: String? = null,
    val selectedDay: String = "",
    val selectedDate: Date? = null,
    val selectedDateFromPlan: Boolean = false,
    val mealSections: List<MealSection> = emptyList(),
    val currentMenu: MenuDto? = null,
    val allMenuItems: List<MenuItemDto> = emptyList(),
    val customPlan: CustomPlan? = null
)

class HomeViewModel(private val preferences: SetupPreferences) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val menuRepository = MenuRepository(RetrofitClient.createService(MenuApi::class.java))
    private val generatorApi = RetrofitClient.createService(GeneratorApi::class.java)
    private val menuApi: MenuApi = RetrofitClient.createService(MenuApi::class.java)
    private val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init { loadCurrentMenu() }

    private fun loadCurrentMenu() {
        viewModelScope.launch { refreshMenu() }
    }

    fun reloadMenu() {
        viewModelScope.launch { refreshMenu() }
    }

    private suspend fun refreshMenu(): MenuDto? {
        _uiState.update { it.copy(isLoading = true, error = null) }
        return try {
            val allItemsResponse = menuApi.getMenuItems()
            val allItems = if (allItemsResponse.isSuccessful) allItemsResponse.body() ?: emptyList() else emptyList()
            
            val planType = preferences.getPlanType()
            val planRange = preferences.getCustomPlanRange()
            val selectedPlanDateMillis = preferences.getSelectedPlanDate()
            
            val (planStart, planEnd) = when (planType) {
                SetupPreferences.PLAN_TYPE_CUSTOM -> {
                    if (planRange != null) Date(planRange.first) to Date(planRange.second)
                    else null to null
                }
                SetupPreferences.PLAN_TYPE_WEEKLY -> {
                    if (selectedPlanDateMillis != null) {
                        val start = Date(selectedPlanDateMillis)
                        val end = Calendar.getInstance().apply { 
                            time = start
                            add(Calendar.DATE, 6)
                        }.time
                        start to end
                    } else null to null
                }
                SetupPreferences.PLAN_TYPE_DAILY -> {
                    if (selectedPlanDateMillis != null) Date(selectedPlanDateMillis) to Date(selectedPlanDateMillis)
                    else null to null
                }
                else -> null to null
            }

            val latestMenu = menuRepository.getLatestMenu()
            var hasMenuForPlan = false
            var currentMenuToDisplay: MenuDto? = null
            
            if (latestMenu != null && planStart != null && planEnd != null) {
                val menuStart = apiDateFormatter.parse(latestMenu.start_date) ?: Date(0)
                if (!menuStart.before(normalizeDate(planStart)) && !menuStart.after(normalizeDate(planEnd))) {
                    hasMenuForPlan = true
                    currentMenuToDisplay = latestMenu
                }
            } else if (latestMenu != null && planType == null) {
                hasMenuForPlan = true
                currentMenuToDisplay = latestMenu
            }

            if (hasMenuForPlan && currentMenuToDisplay != null) {
                val menuItems = currentMenuToDisplay.items ?: emptyList()
                val menuStart = apiDateFormatter.parse(currentMenuToDisplay.start_date) ?: Date()
                val maxOffset = menuItems.maxOfOrNull { it.day_offset } ?: 0
                val menuEnd = Calendar.getInstance().apply { 
                    time = menuStart
                    add(Calendar.DATE, maxOffset)
                }.time

                _uiState.update {
                    val today = normalizeDate(Date())
                    val availableDates = buildAvailableDates(menuItems, CustomPlan(menuStart, menuEnd))
                    val resolvedSelectedDate = if (availableDates.any { it.time == today.time }) today else availableDates.firstOrNull()

                    it.copy(
                        isLoading = false,
                        hasMenu = true,
                        currentMenu = currentMenuToDisplay,
                        customPlan = CustomPlan(menuStart, menuEnd),
                        allMenuItems = allItems,
                        selectedDate = resolvedSelectedDate,
                        selectedDay = resolvedSelectedDate?.let { d -> resolveDayNameForDate(d) }.orEmpty(),
                        selectedDateFromPlan = false
                    )
                }
                updateMealSections()
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        hasMenu = false, 
                        allMenuItems = allItems,
                        selectedDate = planStart?.let { d -> normalizeDate(d) },
                        selectedDay = planStart?.let { d -> resolveDayNameForDate(d) }.orEmpty(),
                        customPlan = if (planStart != null && planEnd != null) CustomPlan(planStart, planEnd) else null
                    ) 
                }
            }
            currentMenuToDisplay
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "Ошибка: ${e.localizedMessage}") }
            null
        }
    }

    fun generateMenu(planType: String?, selectedPlanDate: Date?, customDays: Int? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val type = planType ?: SetupPreferences.PLAN_TYPE_WEEKLY
                val periodStr = when {
                    customDays != null -> "custom"
                    type == SetupPreferences.PLAN_TYPE_DAILY -> "day"
                    else -> "week"
                }
                val startDateStr = resolveGenerationStartDateString(
                    formatter = apiDateFormatter, selectedPlanDate = selectedPlanDate
                )
                val response = generatorApi.autoGenerate(
                    AutoGenerateRequest(period = periodStr, start_date = startDateStr, days = customDays)
                )
                if (response.isSuccessful) {
                    refreshMenu()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = try {
                        val json = org.json.JSONObject(errorBody ?: "{}")
                        json.optString("detail", "Ошибка генерации")
                    } catch (e: Exception) { "Ошибка сервера: ${response.code()}" }
                    _uiState.update { it.copy(error = message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun updateMealSections() {
        val state = _uiState.value
        val menu = state.currentMenu ?: return
        try {
            val resolvedDate = state.selectedDate ?: buildAvailableDates(menu.items ?: emptyList(), state.customPlan).firstOrNull()
            val itemsForDay = if (resolvedDate != null) {
                val selectedDateStr = apiDateFormatter.format(resolvedDate)
                if (state.selectedDateFromPlan) state.allMenuItems.filter { it.actual_date == selectedDateStr }
                else menu.items?.filter { it.actual_date == selectedDateStr } ?: emptyList()
            } else emptyList()

            val mealSections = itemsForDay.map { item ->
                val title = when (item.meal_type) {
                    "breakfast" -> "Завтрак"; "lunch" -> "Обед"; "dinner" -> "Ужин"
                    else -> item.meal_type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
                MealSection(id = item.meal_type, title = title, meal = item)
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
                selectedDate = resolvedDate,
                selectedDay = resolvedDate?.let { d -> resolveDayNameForDate(d) }.orEmpty()
            ) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Ошибка обработки даты: ${e.localizedMessage}") }
        }
    }

    fun toggleFavorite(mealId: Int) {
        val state = _uiState.value
        val menuItem = state.currentMenu?.items?.find { it.id == mealId }
            ?: state.allMenuItems.find { it.id == mealId }
            ?: return

        val recipeId = menuItem.recipe

        viewModelScope.launch {
            try {
                val response = menuApi.toggleFavorite(com.example.smartmeal.feature.home.data.api.ToggleFavoriteRequest(recipeId))
                if (response.isSuccessful) {
                    val isFavorite = response.body()?.is_favorite ?: false
                    
                    // Обновляем состояние локально
                    _uiState.update { currentState ->
                        val updatedCurrentMenu = currentState.currentMenu?.copy(
                            items = currentState.currentMenu.items?.map { 
                                if (it.recipe == recipeId) it.copy(is_favorite = isFavorite) else it 
                            }
                        )
                        val updatedAllMenuItems = currentState.allMenuItems.map { 
                            if (it.recipe == recipeId) it.copy(is_favorite = isFavorite) else it 
                        }
                        currentState.copy(
                            currentMenu = updatedCurrentMenu,
                            allMenuItems = updatedAllMenuItems
                        )
                    }
                    updateMealSections()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка при изменении избранного: ${e.localizedMessage}") }
            }
        }
    }

    fun selectDate(date: Date, customPlan: CustomPlan?) {
        val normalized = normalizeDate(date)
        if (customPlan != null) {
            val start = normalizeDate(customPlan.startDate)
            val end = normalizeDate(customPlan.endDate)
            if (normalized.before(start) || normalized.after(end)) return
        }
        val calendar = Calendar.getInstance().apply { time = normalized }
        val dayIndex = when(calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6; else -> 0
        }
        _uiState.update { it.copy(selectedDay = dayNames[dayIndex], selectedDate = normalized, selectedDateFromPlan = customPlan != null) }
        updateMealSections()
    }

    fun clearPlanSelection() {
        _uiState.update { it.copy(selectedDateFromPlan = false) }
        updateMealSections()
    }

    private fun normalizeDate(date: Date): Date {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.time
    }

    fun replaceMeal(mealType: String) {
        val state = _uiState.value
        val menu = state.currentMenu ?: return
        val selectedDate = state.selectedDate ?: buildAvailableDates(menu.items ?: emptyList(), state.customPlan).firstOrNull() ?: return
        val selectedDateStr = apiDateFormatter.format(selectedDate)
        val menuItem = if (state.selectedDateFromPlan) {
            state.allMenuItems.find { it.actual_date == selectedDateStr && it.meal_type == mealType }
        } else {
            menu.items?.find { it.actual_date == selectedDateStr && it.meal_type == mealType }
        } ?: return

        viewModelScope.launch {
            try {
                val updatedItem = menuRepository.replaceMenuItem(menuItem.id)
                if (updatedItem != null) {
                    preferences.clearMenuItemServings(updatedItem.id)
                    _uiState.update { currentState -> mergeUpdatedMenuItemIntoState(currentState, updatedItem) }
                    updateMealSections()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun dismissError() { _uiState.update { it.copy(error = null) } }
}

internal fun buildAvailableDates(
    menuItems: List<MenuItemDto>,
    customPlan: CustomPlan?
): List<Date> {
    if (customPlan != null) {
        val dates = mutableListOf<Date>()
        val cal = Calendar.getInstance().apply { time = normalizeDateStatic(customPlan.startDate) }
        val end = normalizeDateStatic(customPlan.endDate)

        var safetyCount = 0
        while (!cal.time.after(end) && safetyCount < 31) {
            dates.add(cal.time)
            cal.add(Calendar.DATE, 1)
            safetyCount++
        }
        if (dates.isNotEmpty()) return dates
    }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return menuItems
        .mapNotNull { item -> try { formatter.parse(item.actual_date) } catch(e: Exception) { null } }
        .map(::normalizeDateStatic)
        .distinct()
        .sorted()
}

private fun resolveDayNameForDate(date: Date): String {
    val calendar = Calendar.getInstance().apply { time = date }
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Пн"; Calendar.TUESDAY -> "Вт"; Calendar.WEDNESDAY -> "Ср"
        Calendar.THURSDAY -> "Чт"; Calendar.FRIDAY -> "Пт"; Calendar.SATURDAY -> "Сб"
        Calendar.SUNDAY -> "Вс"; else -> ""
    }
}

private fun normalizeDateStatic(date: Date): Date {
    val cal = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return cal.time
}

private fun formatMonthYearForSelector(date: Date): String {
    val text = SimpleDateFormat("LLLL yyyy", Locale("ru")).format(date)
    return text.replaceFirstChar { it.titlecase(Locale("ru")) }
}

internal fun mergeUpdatedMenuItemIntoState(state: HomeUiState, updatedItem: MenuItemDto): HomeUiState {
    val updatedCurrentMenu = state.currentMenu?.copy(
        items = state.currentMenu.items?.map { if (it.id == updatedItem.id) updatedItem else it }
    )
    val updatedAllMenuItems = state.allMenuItems.map { if (it.id == updatedItem.id) updatedItem else it }
    return state.copy(currentMenu = updatedCurrentMenu, allMenuItems = updatedAllMenuItems)
}

internal fun resolveGenerationStartDateString(
    formatter: SimpleDateFormat,
    selectedPlanDate: Date?,
    fallbackDate: Date = Date()
): String = formatter.format(selectedPlanDate ?: fallbackDate)

private class HomeViewModelFactory(private val preferences: SetupPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) return HomeViewModel(preferences) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private class ProductListViewModelFactory(private val menuApi: MenuApi, private val preferences: SetupPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductListViewModel::class.java)) return ProductListViewModel(menuApi, preferences) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ProfileViewModelFactory(
    private val api: SetupApi,
    private val preferences: SetupPreferences,
    private val onProfileUpdated: () -> Unit
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(api, preferences, onProfileUpdated) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
