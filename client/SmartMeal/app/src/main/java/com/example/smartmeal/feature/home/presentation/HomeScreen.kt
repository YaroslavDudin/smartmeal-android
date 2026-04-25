package com.example.smartmeal.feature.home.presentation

import androidx.compose.animation.AnimatedContent      
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.example.smartmeal.ui.components.feedback.HomeScreenSkeleton

import com.example.smartmeal.ui.components.feedback.ExitConfirmDialog

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
        factory = remember { 
            HomeViewModelFactory(context.applicationContext as android.app.Application, setupPreferences) 
        }
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
    
    // БЛОКИРУЮЩИЙ ОВЕРЛЕЙ (Твоя красивая загрузка)
    if (uiState.isSyncing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(enabled = true, onClick = {}) 
                .zIndex(100f),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = PrimaryGreen)
                    Spacer(modifier = Modifier.height(16.dp))
                    SmartMealText(
                        text = "Сверяем актуальные данные...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    SmartMealText(
                        text = "Ищем последние обновления рациона",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    val planType = setupPreferences.getPlanType()
    val planRange = setupPreferences.getCustomPlanRange()
    val selectedPlanDateMillis = setupPreferences.getSelectedPlanDate()
    
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

    val showMyPlanSection = remember(planType, customPlan) {
        if (planType != SetupPreferences.PLAN_TYPE_CUSTOM || customPlan == null) false
        else {
            val diff = customPlan.endDate.time - customPlan.startDate.time
            val days = (diff / (1000L * 60 * 60 * 24)) + 1
            days > 7
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

            if (outOfRange) {
                viewModel.selectDate(visibleCustomPlan.startDate)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && setupPreferences.consumePendingPlanRegeneration()) {
                viewModel.regenerateMenuForCurrentPlan()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var selectedNavItem: Int by rememberSaveable { mutableIntStateOf(0) }
    var shouldOpenOrderModal: Boolean by remember { mutableStateOf(false) }
    var shouldScrollToCart: Boolean by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Перехват кнопки "Назад" для подтверждения выхода
    androidx.activity.compose.BackHandler(enabled = true) {
        if (selectedNavItem != 0) {
            selectedNavItem = 0
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        ExitConfirmDialog(
            onConfirm = {
                (context as? android.app.Activity)?.finish()
            },
            onDismiss = { showExitDialog = false }
        )
    }

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

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                BottomNavigationBar(
                    selectedItem = selectedNavItem,
                    onItemSelected = { selectedNavItem = it }
                )
            }
        ) { innerPadding ->
            androidx.compose.animation.AnimatedContent(
                targetState = selectedNavItem,
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ScreenSwitchAnimation"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                onClick = { viewModel.clearError() },
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            )
                    ) {
                        HomeContent(
                            uiState = uiState,
                            onDismissError = { viewModel.clearError() },
                            onDateSelected = { viewModel.selectDate(it) },
                            onGenerateMenu = { viewModel.regenerateMenuForCurrentPlan() },
                            onReplaceMeal = { viewModel.replaceMeal(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onRecipeClick = onRecipeClick,
                            onSearchClick = onSearchClick,
                            onReselectPlan = onReselectPlan,
                            onCartClick = {
                                selectedNavItem = 1
                                shouldScrollToCart = true
                            },
                            customPlan = visibleCustomPlan,
                            showMyPlanSection = showMyPlanSection
                        )
                    }
                    1 -> ProductListScreen(
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
                        scrollToCart = shouldScrollToCart,
                        onOrderModalConsumed = { shouldOpenOrderModal = false },
                        onScrollToCartConsumed = { shouldScrollToCart = false }
                    )
                    2 -> StatisticsScreen(preferences = setupPreferences)
                    3 -> ProfileScreen(
                        viewModel = profileViewModel,
                        onLogout = onLogout,
                        onLogoutSuccess = onLogoutSuccess,
                        onGoToProducts = {
                            selectedNavItem = 1
                            shouldOpenOrderModal = true
                        },
                        onRecipeClick = { recipeId -> onRecipeClick(recipeId, null) },
                        onProfileUpdatedSuccessfully = {}
                    )
                }
            }
        }

        // БЛОКИРУЮЩИЙ ОВЕРЛЕЙ (Твоя красивая загрузка) - ТЕПЕРЬ ВСЕГДА СВЕРХУ
        if (uiState.isSyncing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(enabled = true, onClick = {})
                    .zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PrimaryGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        SmartMealText(
                            text = "Сверяем актуальные данные...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        SmartMealText(
                            text = "Ищем последние обновления рациона",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onDismissError: () -> Unit,
    onDateSelected: (Date) -> Unit,
    onGenerateMenu: () -> Unit,
    onReplaceMeal: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onRecipeClick: (Int, Int?) -> Unit,
    onSearchClick: () -> Unit,
    onReselectPlan: () -> Unit,
    onCartClick: () -> Unit,
    customPlan: CustomPlan?,
    showMyPlanSection: Boolean = false
) {
    val availableDates = remember(uiState.allMenuItems, customPlan) {
        buildAvailableDates(uiState.allMenuItems, customPlan)
    }

    val dateSelectorItems = remember(availableDates) { buildDateSelectorItems(availableDates) }
    val selectedDateId = uiState.selectedDate?.let { buildDateSelectorId(it) }
    val hasAvailableDates = availableDates.isNotEmpty()
    var pendingReplacement by remember { mutableStateOf<MealSection?>(null) }

    // Проверка для кнопки "Мой план": тип CUSTOM и более 7 дней
    val showMyPlanButton = remember(showMyPlanSection, customPlan) {
        showMyPlanSection && customPlan != null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 0.dp)) {
                SmartMealText(
                    text = "Меню",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.Center).testTag("home_title"),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = onCartClick,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).testTag("home_cart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Корзина",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        val monthYearLabel = availableDates.firstOrNull()?.let {
            formatMonthYearForSelector(uiState.selectedDate ?: it)
        }.orEmpty()

        if (monthYearLabel.isNotBlank() && availableDates.size > 1) {
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

        if (hasAvailableDates) {
            item {
                Box(modifier = Modifier.testTag("home_day_selector")) {
                    if (availableDates.size == 1) {
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

        if (uiState.isLoading && !uiState.hasMenu) {
            item {
                HomeScreenSkeleton()
            }
        } else {
            if (showMyPlanButton) {
                item {
                    MyPlanSection(
                        customPlan = customPlan,
                        selectedDate = uiState.selectedDate,
                        onDateSelectedFromPlan = onDateSelected,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 0.dp, bottom = 4.dp)
                            .testTag("home_my_plan")
                    )
                }
            }

            if (!uiState.hasMenu) {
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
            } else if (availableDates.isEmpty() && uiState.hasMenu) {
                // Состояние истекшего рациона
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .testTag("home_expired_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SmartMealText(
                                text = "Ваш рацион закончился",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SmartMealText(
                                text = "Выберите новый план питания, чтобы продолжить",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onReselectPlan,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(54.dp)
                                    .testTag("home_reselect_plan_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ) {
                                SmartMealText("Выбрать новый рацион", color = Color.White, fontSize = 16.sp)
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
                        onReplaceClick = { pendingReplacement = section },
                        onFavoriteClick = { onToggleFavorite(section.meal.id) },
                        onRecipeClick = onRecipeClick
                    )
                }
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
                onReplaceMeal(section.meal.id)
                pendingReplacement = null
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
    onReplaceClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onRecipeClick: (Int, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
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

                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("home_replace_confirm_button"),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    SmartMealText("Заменить блюдо", color = Color.White, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("home_replace_cancel_button"),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    SmartMealText("Отменить", color = Color.White, fontSize = 18.sp)
                }
            }
        }
    }
}

private class HomeViewModelFactory(
    private val application: android.app.Application,
    private val preferences: SetupPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) return HomeViewModel(application, preferences) as T
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

internal fun buildAvailableDates(
    menuItems: List<MenuItemDto>,
    customPlan: CustomPlan?,
    today: Date = Date()
): List<Date> {
    val normalizedToday = normalizeDateStatic(today)
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
    if (customPlan.endDate.before(normalizedToday)) return null
    return CustomPlan(maxOf(customPlan.startDate, normalizedToday), customPlan.endDate)
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
    return SimpleDateFormat("LLLL yyyy", Locale("ru")).format(date).replaceFirstChar { it.titlecase() }
}

internal fun mergeUpdatedMenuItemIntoState(state: HomeUiState, updatedItem: MenuItemDto): HomeUiState {
    val updatedAll = state.allMenuItems.map { if (it.id == updatedItem.id) updatedItem else it }
    return state.copy(allMenuItems = updatedAll)
}

internal fun resolveCustomDays(range: Pair<Long, Long>?): Int? {
    if (range == null) return null
    return ((range.second - range.first) / (1000L * 60L * 60L * 24L)).toInt() + 1
}


