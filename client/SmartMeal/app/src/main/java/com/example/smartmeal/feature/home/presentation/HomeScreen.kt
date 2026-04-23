package com.example.smartmeal.feature.home.presentation

import androidx.compose.animation.AnimatedContent      
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.example.smartmeal.ui.theme.PrimaryGreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ModalBackground = Color(0xFFF4F4F4)
private val ReplaceButtonBackground = Color(0xFFF5F5F5)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onReselectPlan: () -> Unit,
    onRecipeClick: (Int, Int?) -> Unit,
    onSearchClick: () -> Unit,
) {
    val menuApi = remember { RetrofitClient.createService(MenuApi::class.java) }
    val setupApi = remember { RetrofitClient.createService(SetupApi::class.java) }
    val context = LocalContext.current
    val setupPreferences = remember { SetupPreferences(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

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
                onCriticalSettingsChanged = { viewModel.regenerateMenuForCurrentPlan() },
                onSimpleSettingsChanged = { viewModel.reloadMenu() },
                onMenuManualChanged = { viewModel.reloadMenu() }
            )
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val planType = setupPreferences.getPlanType()
    val planRange = setupPreferences.getCustomPlanRange()
    val selectedPlanDateMillis = setupPreferences.getSelectedPlanDate()
    // "Мой план" нужен только для пользовательского диапазона.
    // Для daily/weekly даты уже и так очевидны из самого плана, поэтому секцию скрываем.
    val showMyPlanSection = planType == SetupPreferences.PLAN_TYPE_CUSTOM

    val customPlan = remember(planType, planRange, selectedPlanDateMillis) {
        when (planType) {
            SetupPreferences.PLAN_TYPE_CUSTOM -> {
                planRange?.let { range: Pair<Long, Long> -> 
                    CustomPlan(Date(range.first), Date(range.second)) 
                }
            }
            SetupPreferences.PLAN_TYPE_WEEKLY -> {
                selectedPlanDateMillis?.let { startMillis: Long ->
                    val endMillis = Calendar.getInstance().apply {
                        time = Date(startMillis)
                        add(Calendar.DATE, 6)
                    }.timeInMillis
                    CustomPlan(Date(startMillis), Date(endMillis))
                }
            }
            SetupPreferences.PLAN_TYPE_DAILY -> {
                selectedPlanDateMillis?.let { startMillis: Long ->
                    CustomPlan(Date(startMillis), Date(startMillis))
                }
            }
            else -> null
        }
    }

    val visibleCustomPlan = remember(customPlan) {
        trimCustomPlanToToday(customPlan)
    }

    LaunchedEffect(visibleCustomPlan?.startDate?.time, visibleCustomPlan?.endDate?.time) {
        if (visibleCustomPlan != null) {
            val selected = uiState.selectedDate
            val outOfRange = selected == null ||
                selected.before(visibleCustomPlan.startDate) ||
                selected.after(visibleCustomPlan.endDate)

            // СБРОС ДАТЫ: Только если текущая дата ВНЕ диапазона нового плана
            if (outOfRange) {
                viewModel.selectDate(visibleCustomPlan.startDate, visibleCustomPlan)
            }
        }
    }

    DisposableEffect(lifecycleOwner, planType, planRange?.first, planRange?.second) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && setupPreferences.consumePendingPlanRegeneration()) {
                val selectedPlanDate = setupPreferences.getSelectedPlanDate()?.let(::Date)
                val customDays = resolveCustomDays(setupPreferences.getCustomPlanRange())
                viewModel.generateMenu(setupPreferences.getPlanType(), selectedPlanDate, customDays)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var selectedNavItem: Int by rememberSaveable { mutableIntStateOf(0) }
    var shouldOpenOrderModal: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(selectedNavItem, uiState.currentMenu) {
        if (selectedNavItem == 1) {
            val currentItems = uiState.currentMenu?.items ?: emptyList()
            if (currentItems.isNotEmpty()) {
                productListViewModel.generateProductsFromMenuItems(currentItems)
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
        // ПЛАВНОСТЬ: Используем AnimatedContent для переключения экранов
        androidx.compose.animation.AnimatedContent(
            targetState = selectedNavItem,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "ScreenSwitchAnimation"
        ) { targetIndex ->
            when (targetIndex) {
                // ── ГЛАВНАЯ (ТВОЯ ЛОГИКА С БАГФИКСАМИ) ─────────────────────
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
                        onDismissError = { viewModel.dismissError() },
                        onSetActiveSlot = { viewModel.setActiveSlot(it) },
                        onDateSelected = { viewModel.selectDate(it, visibleCustomPlan) },
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
                                        customDays = resolveCustomDays(range)
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
                        onSearchClick = onSearchClick,
                        onDateSelectedFromPlan = { viewModel.selectDate(it, visibleCustomPlan) },
                        onReselectPlan = onReselectPlan,
                        customPlan = visibleCustomPlan,
                        showMyPlanSection = showMyPlanSection
                    )
                }

                // ── ПРОДУКТЫ ───────────────────────────────────────────────
                1 -> {
                    ProductListScreen(
                        viewModel = productListViewModel,
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
                        onReselectPlan = onReselectPlan,
                        hasNoAvailableDays = productListViewModel.hasNoAvailableDays,
                        isLoading = productListViewModel.isLoading,
                        errorMessage = productListViewModel.errorMessage,
                        customPlan = if (showMyPlanSection) customPlan else null,
                        openOrderModal = shouldOpenOrderModal,
                        onOrderModalConsumed = { shouldOpenOrderModal = false }
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
                        onGoToProducts = {
                            selectedNavItem = 1
                            shouldOpenOrderModal = true
                        },
                        onRecipeClick = { recipeId -> onRecipeClick(recipeId, null) },
                        onProfileUpdatedSuccessfully = {
                            // Оставляем пользователя на вкладке профиля
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onDismissError: () -> Unit,
    onSetActiveSlot: (String) -> Unit,
    onDateSelected: (Date) -> Unit,
    onGenerateMenu: () -> Unit,
    onReplaceMeal: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onRecipeClick: (Int, Int?) -> Unit,
    onSearchClick: () -> Unit,
    onDateSelectedFromPlan: (Date) -> Unit,
    onReselectPlan: () -> Unit,
    customPlan: CustomPlan?,
    showMyPlanSection: Boolean = false
) {
    val availableDates = remember(uiState.currentMenu?.items, uiState.allMenuItems, customPlan) {
        val sourceItems = if (customPlan != null) uiState.allMenuItems
                         else uiState.currentMenu?.items ?: emptyList()
        buildAvailableDates(sourceItems, customPlan)
    }

    val dateSelectorItems = remember(availableDates) { buildDateSelectorItems(availableDates) }
    val selectedDateId = uiState.selectedDate?.let { buildDateSelectorId(it) }
    val hasSingleAvailableDate = availableDates.size == 1
    val hasAvailableDates = availableDates.isNotEmpty()
    var pendingReplacement by remember(uiState.mealSections) { mutableStateOf<MealSection?>(null) }

    // ЛАНДШАФТ: Используем LazyColumn для ВСЕГО контента, чтобы он прокручивался целиком
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 0.dp)) {
                SmartMealText(
                    text = "Меню",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.Center).testTag("home_title"),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        }

        val monthYearLabel = availableDates.firstOrNull()?.let {
            formatMonthYearForSelector(uiState.selectedDate ?: it)
        }.orEmpty()

        if (monthYearLabel.isNotBlank() && !hasSingleAvailableDate) {
            item {
                SmartMealText(
                    text = monthYearLabel,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("home_month_year"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ТАБЛЕТКИ КАЛЕНДАРЯ
        if (hasAvailableDates) {
            item {
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
            }
        }

        if (showMyPlanSection && customPlan != null && hasAvailableDates) {
            item {
                MyPlanSection(
                    customPlan = customPlan,
                    selectedDate = uiState.selectedDate,
                    onDateSelectedFromPlan = onDateSelectedFromPlan,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("home_my_plan")
                )
            }
        }

        if (uiState.isLoading && !uiState.hasMenu) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.testTag("home_loading"))
                }
            }
        } else if (!uiState.hasMenu) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
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
            }
        } else if (!hasAvailableDates) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.testTag("home_expired_state")
                    ) {
                        SmartMealText(
                            "Доступные дни закончились. Выберите план и дату заново",
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onReselectPlan,
                            modifier = Modifier.testTag("home_reselect_plan_button"),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            SmartMealText("Выбрать план и дату")
                        }
                    }
                }
            }
        } else {
            items(uiState.mealSections, key = { it.meal.id }) { section ->
                MealSection(
                    sectionId = section.id,
                    title = section.title,
                    meal = section.meal,
                    onSetActiveSlot = onSetActiveSlot,
                    onReplaceClick = { pendingReplacement = section },
                    onFavoriteClick = { onToggleFavorite(section.meal.id) },
                    onRecipeClick = onRecipeClick
                )
            }
        }

        if (uiState.error != null) {
            item {
                SmartMealText(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }

    pendingReplacement?.let { section ->
        ReplaceMealConfirmDialog(
            mealTitle = section.meal.recipe_title,
            onConfirm = {
                val mealId = section.meal.id
                pendingReplacement = null
                onReplaceMeal(mealId)
            },
            onDismiss = { pendingReplacement = null }
        )
    }
}

@Composable
fun MealSection(
    sectionId: String,
    title: String,
    meal: MenuItemDto,
    onSetActiveSlot: (String) -> Unit,
    onReplaceClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onRecipeClick: (Int, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmartMealText(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            CircleIconButton(
                iconType = CircleIconType.REPLACE,
                onClick = onReplaceClick,
                backgroundColor = Color.Transparent,
                contentColor = Color.Black.copy(alpha = 0.6f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                modifier = Modifier.size(36.dp).testTag("home_replace_${sectionId}")
            )
        }

        AnimatedContent<MenuItemDto>(
            targetState = meal,
            transitionSpec = {
                (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.95f)) togetherWith
                        (fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 0.95f))
            },
            contentKey = { it.recipe },
            label = "MealReplacementAnimation"
        ) { item ->
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .clickable { 
                        onSetActiveSlot(item.meal_type)
                        onRecipeClick(item.recipe, item.id) 
                    }
            ) {
                MealCard(
                    title = item.recipe_title,
                    cookTime = "${item.cook_time} мин",
                    imageUrl = item.image_url,
                    isFavorite = item.is_favorite,
                    isActive = item.is_active,
                    onFavoriteClick = onFavoriteClick
                )
            }
        }
    }
}

@Composable
private fun ReplaceMealConfirmDialog(
    mealTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("home_replace_confirm_dialog"),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = ModalBackground,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .verticalScroll(rememberScrollState()), // Добавлена возможность скролла
                horizontalAlignment = Alignment.Start // Выравнивание по левому краю
            ) {
                SmartMealText(
                    text = "Заменить \"$mealTitle\"",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                SmartMealText(
                    text = "Вы уверены что хотите заменить\nэто блюдо на что-нибудь другое?\nЕсли блюдо добавлено в\nизбранное, оно останется в\nизбранном",
                    fontSize = 16.sp,
                    color = Color.Black,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Кнопка "Заменить блюдо"
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("home_replace_confirm_button"),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50), // Зеленый
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 0.0.dp
                    )
                ) {
                    SmartMealText(
                        text = "Заменить блюдо",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Кнопка "Отменить"
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("home_replace_cancel_button"),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935), // Красный
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    SmartMealText(
                        text = "Отменить",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
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

    init {
        loadCurrentMenu()
        viewModelScope.launch {
            com.example.smartmeal.data.manager.FavoritesManager.favoriteUpdates.collect { update ->
                updateFavoriteInState(update.recipeId, update.isFavorite)
            }
        }
        viewModelScope.launch {
            com.example.smartmeal.data.manager.DateManager.dateUpdates.collect { date ->
                if (_uiState.value.selectedDate?.time != date.time) {
                    val customPlan = _uiState.value.customPlan
                    selectDate(date, customPlan, notifyManager = false)
                }
            }
        }
        viewModelScope.launch {
            com.example.smartmeal.data.manager.MealSlotManager.activeMealType.collect {
                updateMealSections()
            }
        }
    }

    private fun updateFavoriteInState(recipeId: Int, isFavorite: Boolean) {
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

    private fun loadCurrentMenu() {
        viewModelScope.launch { refreshMenu() }
    }

    fun reloadMenu() {
        viewModelScope.launch { 
            com.example.smartmeal.feature.home.data.MenuRepository.clearCache()
            refreshMenu() 
            com.example.smartmeal.data.manager.MenuUpdateManager.notifyMenuChanged()
        }
    }

    fun setActiveSlot(mealType: String) {
        com.example.smartmeal.data.manager.MealSlotManager.setActiveMealType(mealType)
    }

    fun regenerateMenuForCurrentPlan() {
        val request = resolveStoredPlanGeneration(
            planType = preferences.getPlanType(),
            selectedPlanDateMillis = preferences.getSelectedPlanDate(),
            customRange = preferences.getCustomPlanRange()
        )

        if (request.startDate == null) {
            reloadMenu()
            return
        }

        generateMenu(
            planType = request.planType,
            selectedPlanDate = request.startDate,
            customDays = request.customDays
        )
    }

    private suspend fun refreshMenu(forceRefresh: Boolean = false): MenuDto? {
        _uiState.update { it.copy(isLoading = true, error = null) }
        return try {
            val allItems = menuRepository.getMenuItems()

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
            var hasMenuToDisplay = false
            var currentMenuToDisplay: MenuDto? = null

            if (latestMenu != null) {
                if (forceRefresh) {
                    hasMenuToDisplay = true
                    currentMenuToDisplay = latestMenu
                } else if (planStart != null && planEnd != null) {
                    val menuStart = apiDateFormatter.parse(latestMenu.start_date) ?: Date(0)
                    if (!menuStart.before(normalizeDate(planStart)) && !menuStart.after(normalizeDate(planEnd))) {
                        hasMenuToDisplay = true
                        currentMenuToDisplay = latestMenu
                    }
                } else if (planType == null) {
                    hasMenuToDisplay = true
                    currentMenuToDisplay = latestMenu
                }
            }

            if (hasMenuToDisplay && currentMenuToDisplay != null) {
                val menuItems = currentMenuToDisplay.items ?: emptyList()
                MenuRepository.setMenuItemsCache(allItems)

                val menuStart = apiDateFormatter.parse(currentMenuToDisplay.start_date) ?: Date()
                val maxOffset = menuItems.maxOfOrNull { it.day_offset } ?: 0
                val menuEnd = Calendar.getInstance().apply {
                    time = menuStart
                    add(Calendar.DATE, maxOffset)
                }.time

                _uiState.update {
                    val today = normalizeDate(Date())
                    val availableDates = buildAvailableDates(menuItems, CustomPlan(menuStart, menuEnd))
                    val lastSelected = com.example.smartmeal.data.manager.DateManager.getLastSelectedDate()
                    val resolvedSelectedDate = if (lastSelected != null && availableDates.any { it.time == normalizeDate(lastSelected).time }) {
                        normalizeDate(lastSelected)
                    } else if (availableDates.any { it.time == today.time }) {
                        today
                    } else {
                        availableDates.firstOrNull()
                    }

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
            updateMealSections()
            _uiState.value.currentMenu
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "Ошибка: ${e.localizedMessage}") }
            null
        }
    }

    fun generateMenu(
        planType: String?,
        selectedPlanDate: Date?,
        customDays: Int? = null,
        totalCalories: Int? = null,
        mealCalories: Map<String, Int>? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val type = planType ?: SetupPreferences.PLAN_TYPE_WEEKLY
                val periodStr = when (type) {
                    SetupPreferences.PLAN_TYPE_CUSTOM -> "custom"
                    SetupPreferences.PLAN_TYPE_DAILY -> "day"
                    else -> "week"
                }
                val startDateStr = resolveGenerationStartDateString(
                    formatter = apiDateFormatter, selectedPlanDate = selectedPlanDate
                )

                val breakfastTime = preferences.getMealCookTime("Завтрак")
                val lunchTime = preferences.getMealCookTime("Обед")
                val dinnerTime = preferences.getMealCookTime("Ужин")

                val cookTimesMap = mutableMapOf<String, String>()
                if (breakfastTime != null && breakfastTime != "any") cookTimesMap["Завтрак"] = breakfastTime
                if (lunchTime != null && lunchTime != "any") cookTimesMap["Обед"] = lunchTime
                if (dinnerTime != null && dinnerTime != "any") cookTimesMap["Ужин"] = dinnerTime

                val dietType = preferences.getDietType()
                val allergies = preferences.getAllergies()
                val caloriesEnabled = preferences.isCaloriesEnabled()

                val response = generatorApi.autoGenerate(
                    AutoGenerateRequest(
                        period = periodStr,
                        start_date = startDateStr,
                        days = customDays,
                        diet_type = dietType,
                        exclude_allergies = allergies,
                        cook_times = if (cookTimesMap.isEmpty()) null else cookTimesMap,
                        total_calories = if (caloriesEnabled) totalCalories ?: preferences.getTotalCalories() else null,
                        calorie_margin = if (caloriesEnabled) preferences.getCalorieMargin() else null,
                        meal_calories = if (caloriesEnabled) mealCalories ?: preferences.getAllMealCalories() else null
                    )
                )
                if (response.isSuccessful) {
                    val newMenuId = response.body()?.id
                    if (newMenuId != null) {
                        try {
                            menuApi.recalculateCart(
                                com.example.smartmeal.feature.home.data.api.RecalculateCartRequest(menu_id = newMenuId)
                            )
                        } catch (e: Exception) {}
                    }
                    
                    com.example.smartmeal.feature.home.data.MenuRepository.clearCache()
                    refreshMenu(forceRefresh = true) 
                    com.example.smartmeal.data.manager.MenuUpdateManager.notifyMenuChanged()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = try {
                        val json = org.json.JSONObject(errorBody ?: "{}")
                        json.optString("detail", "Ошибка генерации")
                    } catch (e: Exception) { "Ошибка сервера: ${response.code()}" }
                    _uiState.update { it.copy(isLoading = false, error = message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun updateMealSections() {
        val state = _uiState.value
        val menu = state.currentMenu ?: return
        try {
            val resolvedDate = state.selectedDate ?: buildAvailableDates(menu.items ?: emptyList(), state.customPlan).firstOrNull()

            // СИНХРОНИЗАЦИЯ: Если мы определили дату (даже по умолчанию), уведомляем DateManager
            if (state.selectedDate == null && resolvedDate != null) {
                com.example.smartmeal.data.manager.DateManager.notifyDateSelected(resolvedDate)
            }

            val itemsForDay = if (resolvedDate != null) {
                val selectedDateStr = apiDateFormatter.format(resolvedDate)
                
                // Приоритет всегда отдаем текущему меню. 
                // allMenuItems используем только для дат, которых нет в текущем меню.
                val currentMenuItems = menu.items?.filter { it.actual_date == selectedDateStr } ?: emptyList()
                
                val sourceItems = if (currentMenuItems.isNotEmpty()) {
                    currentMenuItems
                } else if (state.selectedDateFromPlan) {
                    state.allMenuItems.filter { it.actual_date == selectedDateStr }
                } else {
                    emptyList()
                }

                // Дедупликация: оставляем только блюда из САМОГО НОВОГО меню (макс ID меню) 
                // и с самым большим ID самого элемента для каждой категории.
                val uniqueItems = sourceItems.sortedWith(
                    compareByDescending<MenuItemDto> { it.menu ?: 0 }
                        .thenByDescending { it.id }
                ).distinctBy { it.meal_type.lowercase(Locale.US) }

                // СИНХРОНИЗАЦИЯ: Обновляем глобальный менеджер для этой даты
                com.example.smartmeal.data.manager.MenuSyncManager.updateMenuForDate(
                    selectedDateStr,
                    uniqueItems.map { com.example.smartmeal.data.manager.RecipeInMenu(it.recipe, it.meal_type) }.toSet()
                )

                uniqueItems
            } else emptyList()

            val activeMealType = com.example.smartmeal.data.manager.MealSlotManager.getActiveMealType()

            val mealSections = itemsForDay.map { item ->
                val title = when (item.meal_type.lowercase(Locale.US)) {
                    "breakfast", "завтрак" -> "Завтрак"
                    "lunch", "обед" -> "Обед"
                    "dinner", "ужин" -> "Ужин"
                    else -> item.meal_type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
                val updatedItem = item.copy(is_active = item.meal_type.lowercase(Locale.US) == activeMealType?.lowercase(Locale.US))
                MealSection(id = item.meal_type, title = title, meal = updatedItem)
            }.sortedBy { section ->
                when(section.id.lowercase(Locale.US)) {
                    "breakfast", "завтрак" -> 1
                    "lunch", "обед" -> 2
                    "dinner", "ужин" -> 3
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
        val mealType = menuItem.meal_type

        viewModelScope.launch {
            try {
                val response = menuApi.toggleFavorite(com.example.smartmeal.feature.home.data.api.ToggleFavoriteRequest(
                    recipe = recipeId,
                    meal_type = mealType
                ))
                if (response.isSuccessful) {
                    val isFavorite = response.body()?.is_favorite ?: false

                    // Обновляем состояние локально
                    updateFavoriteInState(recipeId, isFavorite)

                    // Уведомляем другие экраны
                    com.example.smartmeal.data.manager.FavoritesManager.notifyFavoriteChanged(recipeId, isFavorite)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка при изменении избранного: ${e.localizedMessage}") }
            }
        }
    }

    fun selectDate(date: Date, customPlan: CustomPlan?, notifyManager: Boolean = true) {
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
        if (com.example.smartmeal.data.manager.MealSlotManager.getActiveMealType() == null) {
            com.example.smartmeal.data.manager.MealSlotManager.setActiveMealType("breakfast")
        }
        _uiState.update { it.copy(selectedDay = dayNames[dayIndex], selectedDate = normalized, selectedDateFromPlan = customPlan != null) }
        updateMealSections()

        if (notifyManager) {
            com.example.smartmeal.data.manager.DateManager.notifyDateSelected(normalized)
        }
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

    fun replaceMeal(mealId: Int) {
        val state = _uiState.value
        // Ищем блюдо везде, где оно может быть
        val menuItem = state.allMenuItems.find { it.id == mealId } 
            ?: state.currentMenu?.items?.find { it.id == mealId }
            ?: return
            
        val oldRecipeId = menuItem.recipe
        val mealType = menuItem.meal_type
        val dateStr = menuItem.actual_date
        
        com.example.smartmeal.data.manager.MealSlotManager.setActiveMealType(mealType)

        viewModelScope.launch {
            try {
                val russianMealName = when (mealType.lowercase(Locale.US)) {
                    "breakfast", "завтрак" -> "Завтрак"
                    "lunch", "обед" -> "Обед"
                    "dinner", "ужин" -> "Ужин"
                    else -> mealType
                }
                val cookTimeRange = preferences.getMealCookTime(russianMealName).takeIf { it != "any" }
                val caloriesEnabled = preferences.isCaloriesEnabled()
                val totalCalories = if (caloriesEnabled) preferences.getTotalCalories() else null
                val mealCalories = if (caloriesEnabled) preferences.getAllMealCalories() else null
                val calorieMargin = if (caloriesEnabled) preferences.getCalorieMargin() else null

                val updatedItem = menuRepository.replaceMenuItem(
                    menuItemId = mealId, 
                    cookTimeRange = cookTimeRange,
                    totalCalories = totalCalories,
                    mealCalories = mealCalories,
                    calorieMargin = calorieMargin
                )
                if (updatedItem != null) {
                    preferences.clearMenuItemServings(updatedItem.id)

                    com.example.smartmeal.data.manager.MenuSyncManager.replaceRecipeInState(
                        dateStr, oldRecipeId, updatedItem.recipe
                    )

                    _uiState.update { currentState ->
                        val updatedCurrentMenu = currentState.currentMenu?.copy(
                            items = currentState.currentMenu.items?.map { if (it.id == mealId) updatedItem else it }
                        )
                        val updatedAllMenuItems = currentState.allMenuItems.map { if (it.id == mealId) updatedItem else it }
                        currentState.copy(currentMenu = updatedCurrentMenu, allMenuItems = updatedAllMenuItems)
                    }
                    
                    // СИНХРОНИЗАЦИЯ: Обновляем кэш репозитория для мгновенного обновления статистики
                    MenuRepository.updateMenuItemInCache(updatedItem)
                    
                    updateMealSections()
                    com.example.smartmeal.data.manager.MenuUpdateManager.notifyMenuChanged()
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
    customPlan: CustomPlan?,
    today: Date = Date()
): List<Date> {
    val normalizedToday = normalizeDateStatic(today)
    if (customPlan != null) {
        val dates = mutableListOf<Date>()
        val cal = Calendar.getInstance().apply {
            time = maxOf(normalizeDateStatic(customPlan.startDate), normalizedToday)
        }
        val end = normalizeDateStatic(customPlan.endDate)

        if (cal.time.after(end)) {
            return emptyList()
        }

        val totalDays = (((end.time - cal.time.time) / (1000L * 60L * 60L * 24L)).toInt() + 1)
            .coerceAtLeast(0)
        repeat(totalDays) {
            dates.add(cal.time)
            cal.add(Calendar.DATE, 1)
        }
        if (dates.isNotEmpty()) return dates
    }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return menuItems
        .mapNotNull { item -> try { formatter.parse(item.actual_date) } catch(e: Exception) { null } }
        .map(::normalizeDateStatic)
        .filter { !it.before(normalizedToday) }
        .distinct()
        .sorted()
}

internal fun trimCustomPlanToToday(
    customPlan: CustomPlan?,
    today: Date = Date()
): CustomPlan? {
    if (customPlan == null) return null
    val normalizedToday = normalizeDateStatic(today)
    val normalizedStart = normalizeDateStatic(customPlan.startDate)
    val normalizedEnd = normalizeDateStatic(customPlan.endDate)
    if (normalizedEnd.before(normalizedToday)) {
        return null
    }
    val visibleStart = maxOf(normalizedStart, normalizedToday)
    return CustomPlan(visibleStart, normalizedEnd)
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

internal fun resolveCustomDays(range: Pair<Long, Long>?): Int? {
    if (range == null) return null
    val diff = range.second - range.first
    return (diff / (1000L * 60L * 60L * 24L)).toInt() + 1
}

internal data class StoredPlanGeneration(
    val planType: String?,
    val startDate: Date?,
    val customDays: Int?
)

internal fun resolveStoredPlanGeneration(
    planType: String?,
    selectedPlanDateMillis: Long?,
    customRange: Pair<Long, Long>?
): StoredPlanGeneration {
    return when (planType) {
        SetupPreferences.PLAN_TYPE_CUSTOM -> StoredPlanGeneration(
            planType = planType,
            startDate = customRange?.first?.let(::Date),
            customDays = resolveCustomDays(customRange)
        )
        SetupPreferences.PLAN_TYPE_WEEKLY,
        SetupPreferences.PLAN_TYPE_DAILY -> StoredPlanGeneration(
            planType = planType,
            startDate = selectedPlanDateMillis?.let(::Date),
            customDays = null
        )
        else -> StoredPlanGeneration(
            planType = planType,
            startDate = selectedPlanDateMillis?.let(::Date),
            customDays = null
        )
    }
}

private class HomeViewModelFactory(
    private val preferences: SetupPreferences
) : ViewModelProvider.Factory {
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
    private val onCriticalSettingsChanged: () -> Unit,
    private val onSimpleSettingsChanged: () -> Unit,
    private val onMenuManualChanged: () -> Unit
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(api, preferences, onCriticalSettingsChanged, onSimpleSettingsChanged, onMenuManualChanged) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
private fun GenerationSettingsDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Map<String, Int>) -> Unit
) {
    var totalCalories by remember { mutableIntStateOf(2000) }
    var breakfastCals by remember { mutableStateOf("600") }
    var lunchCals by remember { mutableStateOf("800") }
    var dinnerCals by remember { mutableStateOf("600") }

    // Sync logic: Slider -> Meals
    val updateMealsFromTotal = { total: Int ->
        breakfastCals = (total * 0.3).toInt().toString()
        lunchCals = (total * 0.4).toInt().toString()
        dinnerCals = (total * 0.3).toInt().toString()
    }

    // Sync logic: Meals -> Total
    val updateTotalFromMeals = {
        val b = breakfastCals.toIntOrNull() ?: 0
        val l = lunchCals.toIntOrNull() ?: 0
        val d = dinnerCals.toIntOrNull() ?: 0
        totalCalories = b + l + d
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = ModalBackground,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SmartMealText(
                    text = "Настройка рациона",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SmartMealText(
                    text = "Общая цель: $totalCalories ккал",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black
                )
                
                Slider(
                    value = totalCalories.toFloat(),
                    onValueChange = { 
                        val newValue = it.toInt()
                        totalCalories = newValue
                        updateMealsFromTotal(newValue)
                    },
                    valueRange = 0f..4000f,
                    steps = 40,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryGreen,
                        activeTrackColor = PrimaryGreen,
                        inactiveTrackColor = Color.LightGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalorieInput(
                        label = "Завтрак", 
                        value = breakfastCals, 
                        onValueChange = { 
                            breakfastCals = it
                            updateTotalFromMeals()
                        }, 
                        modifier = Modifier.weight(1f)
                    )
                    CalorieInput(
                        label = "Обед", 
                        value = lunchCals, 
                        onValueChange = { 
                            lunchCals = it
                            updateTotalFromMeals()
                        }, 
                        modifier = Modifier.weight(1f)
                    )
                    CalorieInput(
                        label = "Ужин", 
                        value = dinnerCals, 
                        onValueChange = { 
                            dinnerCals = it
                            updateTotalFromMeals()
                        }, 
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { 
                        val meals = mapOf(
                            "Завтрак" to (breakfastCals.toIntOrNull() ?: 0),
                            "Обед" to (lunchCals.toIntOrNull() ?: 0),
                            "Ужин" to (dinnerCals.toIntOrNull() ?: 0)
                        )
                        onConfirm(totalCalories, meals) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    SmartMealText("Сгенерировать", color = Color.White, fontSize = 18.sp)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    SmartMealText("Отмена", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun CalorieInput(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit, 
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SmartMealText(label, fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { 
                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                    onValueChange(it)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = Color.LightGray,
                cursorColor = PrimaryGreen
            )
        )
    }
}
