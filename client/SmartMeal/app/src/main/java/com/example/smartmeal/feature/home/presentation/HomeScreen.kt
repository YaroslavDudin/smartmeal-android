package com.example.smartmeal.feature.home.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.smartmeal.feature.profile.presentation.ProfileSubScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartmeal.data.api.RetrofitClient
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.home.data.api.MenuApi
import com.example.smartmeal.feature.home.data.menu.MenuItemDto
import com.example.smartmeal.feature.products.presentation.ProductListScreen
import com.example.smartmeal.feature.products.presentation.ProductListViewModel
import com.example.smartmeal.feature.profile.presentation.ProfileScreen
import com.example.smartmeal.feature.profile.presentation.ProfileViewModel
import com.example.smartmeal.feature.setup.data.api.SetupApi
import com.example.smartmeal.feature.statistics.presentation.StatisticsScreen
import com.example.smartmeal.ui.components.SmartMealText
import com.example.smartmeal.ui.components.buttons.CircleIconButton
import com.example.smartmeal.ui.components.buttons.CircleIconType
import com.example.smartmeal.ui.components.cards.BottomNavigationBar
import com.example.smartmeal.ui.components.cards.MealCard
import com.example.smartmeal.ui.components.feedback.ExitConfirmDialog
import com.example.smartmeal.ui.components.feedback.HomeScreenSkeleton
import com.example.smartmeal.ui.components.selectors.DateSelector
import com.example.smartmeal.ui.components.selectors.buildDateSelectorId
import com.example.smartmeal.ui.components.selectors.buildDateSelectorItems
import com.example.smartmeal.ui.components.selectors.formatSelectedDateLabel
import com.example.smartmeal.ui.theme.LightGreenBg
import com.example.smartmeal.ui.theme.PrimaryGreen
import com.example.smartmeal.ui.theme.SmartMealBackground
import com.example.smartmeal.ui.theme.SmartMealBlue
import com.example.smartmeal.ui.theme.SmartMealCardBorder
import com.example.smartmeal.ui.theme.SmartMealGreen
import com.example.smartmeal.ui.theme.SmartMealHeart
import com.example.smartmeal.ui.theme.SmartMealOrange
import com.example.smartmeal.ui.theme.SmartMealPurple
import com.example.smartmeal.ui.theme.SmartMealSurfaceSoft
import com.example.smartmeal.ui.theme.SmartMealTextMuted
import com.example.smartmeal.ui.theme.SmartMealTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ModalBackground = SmartMealBackground
private val HomeHeroStart = Color(0xFFFFFFFF)
private val HomeHeroEnd = Color(0xFFFFF0EB)
private val HomeCardBorder = SmartMealCardBorder
private val HomeMutedText = SmartMealTextSecondary
private val HomeSoftSurface = SmartMealSurfaceSoft
private val HomePageBackground = PageBackgroundPalette(
    start = SmartMealBackground,
    end = Color(0xFFFFFFFF)
)
private val ProductsPageBackground = PageBackgroundPalette(
    start = SmartMealBackground,
    end = Color(0xFFFFFFFF)
)
private val StatisticsPageBackground = PageBackgroundPalette(
    start = SmartMealBackground,
    end = Color(0xFFFFFFFF)
)
private val ProfilePageBackground = PageBackgroundPalette(
    start = SmartMealBackground,
    end = Color(0xFFFFFFFF)
)

private data class PageBackgroundPalette(
    val start: Color,
    val end: Color
)

private enum class ShoppingPeriodPreset(
    val days: Int,
    val title: String,
    val actionLabel: String
) {
    DAY(1, "На 1 день", "Собрать продукты на ближайший день"),
    WEEK(7, "На 1 неделю", "Подготовить стандартную недельную закупку"),
    TWO_WEEKS(14, "На 2 недели", "Собрать расширенный список на 14 дней"),
    MONTH(30, "На 1 месяц", "Посмотреть полный продуктовый горизонт")
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val customPlan = remember(planType, planRange, selectedPlanDateMillis) {
        when (planType) {
            SetupPreferences.PLAN_TYPE_CUSTOM -> {
                planRange?.let { range -> CustomPlan(Date(range.first), Date(range.second)) }
            }
            SetupPreferences.PLAN_TYPE_WEEKLY -> {
                selectedPlanDateMillis?.let { startMillis ->
                    val endMillis = Calendar.getInstance().apply {
                        time = Date(startMillis)
                        add(Calendar.DATE, 6)
                    }.timeInMillis
                    CustomPlan(Date(startMillis), Date(endMillis))
                }
            }
            SetupPreferences.PLAN_TYPE_DAILY -> {
                selectedPlanDateMillis?.let { startMillis ->
                    CustomPlan(Date(startMillis), Date(startMillis))
                }
            }
            else -> null
        }
    }

    val showMyPlanSection = remember(planType, customPlan) {
        planType == SetupPreferences.PLAN_TYPE_CUSTOM && customPlan != null && resolvePlanDays(customPlan) > 7
    }

    val visibleCustomPlan = remember(customPlan) {
        trimCustomPlanToToday(customPlan)
    }

    val menuItemsForProducts = remember(uiState.allMenuItems, uiState.currentMenu) {
        uiState.allMenuItems.ifEmpty { uiState.currentMenu?.items ?: emptyList() }
    }

    val cartPreviewEndDate = remember(menuItemsForProducts, visibleCustomPlan) {
        buildAvailableDates(menuItemsForProducts, visibleCustomPlan).lastOrNull()
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

    var selectedNavItem by rememberSaveable { mutableIntStateOf(0) }
    var shouldOpenOrderModal by remember { mutableStateOf(false) }
    var shouldScrollToCart by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showCartPeriodSheet by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (selectedNavItem != 0) {
            selectedNavItem = 0
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        ExitConfirmDialog(
            onConfirm = { (context as? android.app.Activity)?.finish() },
            onDismiss = { showExitDialog = false }
        )
    }

    fun openProductsForPreset(preset: ShoppingPeriodPreset) {
        if (menuItemsForProducts.isNotEmpty()) {
            productListViewModel.generateProductsFromMenuItems(menuItemsForProducts)
            productListViewModel.selectPresetRange(
                days = preset.days,
                anchorDate = uiState.selectedDate ?: visibleCustomPlan?.startDate
            )
        } else {
            productListViewModel.clearProducts()
        }

        shouldScrollToCart = false
        showCartPeriodSheet = false
        selectedNavItem = 1
    }

    LaunchedEffect(selectedNavItem, menuItemsForProducts) {
        if (selectedNavItem == 1) {
            if (menuItemsForProducts.isNotEmpty()) {
                productListViewModel.generateProductsFromMenuItems(menuItemsForProducts)
            } else {
                productListViewModel.clearProducts()
            }
        }
    }

    val pageBackground = remember(selectedNavItem) {
        when (selectedNavItem) {
            1 -> ProductsPageBackground
            2 -> StatisticsPageBackground
            3 -> ProfilePageBackground
            else -> HomePageBackground
        }
    }
    val animatedBackgroundStart by animateColorAsState(
        targetValue = pageBackground.start,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "homePageBackgroundStart"
    )
    val animatedBackgroundEnd by animateColorAsState(
        targetValue = pageBackground.end,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "homePageBackgroundEnd"
    )

    var initialProfileSubScreen by remember { mutableStateOf(ProfileSubScreen.NONE) }
    val onCalorieSettingsClick = {
        initialProfileSubScreen = ProfileSubScreen.CALORIES
        selectedNavItem = 3
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(animatedBackgroundStart, animatedBackgroundEnd)
                )
            )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                BottomNavigationBar(
                    selectedItem = selectedNavItem,
                    onItemSelected = { selectedNavItem = it }
                )
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedNavItem,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                transitionSpec = {
                    (fadeIn(animationSpec = tween(320, delayMillis = 40)) +
                        scaleIn(
                            initialScale = 0.985f,
                            animationSpec = tween(320, delayMillis = 40)
                        )) togetherWith
                        (fadeOut(animationSpec = tween(220)) +
                            scaleOut(
                                targetScale = 1.01f,
                                animationSpec = tween(220)
                            ))
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
                                interactionSource = remember {
                                    androidx.compose.foundation.interaction.MutableInteractionSource()
                                }
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
                            onCartClick = { showCartPeriodSheet = true },
                            onProductsClick = { selectedNavItem = 1 },
                            onStatisticsClick = { selectedNavItem = 2 },
                            onProfileClick = { selectedNavItem = 3 },
                            onCalorieSettingsClick = onCalorieSettingsClick,
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
                        customPlan = if (showMyPlanSection) visibleCustomPlan else null,
                        openOrderModal = shouldOpenOrderModal,
                        scrollToCart = shouldScrollToCart,
                        onOrderModalConsumed = { shouldOpenOrderModal = false },
                        onScrollToCartConsumed = { shouldScrollToCart = false }
                    )
                    2 -> StatisticsScreen(
                        preferences = setupPreferences,
                        onRecipeClick = onRecipeClick,
                        onCalorieSettingsClick = {
                            initialProfileSubScreen = ProfileSubScreen.CALORIES
                            selectedNavItem = 3
                        }
                    )
                    3 -> ProfileScreen(
                        viewModel = profileViewModel,
                        onLogout = onLogout,
                        onLogoutSuccess = onLogoutSuccess,
                        onGoToProducts = {
                            selectedNavItem = 1
                            shouldOpenOrderModal = true
                        },
                        onRecipeClick = { recipeId -> onRecipeClick(recipeId, null) },
                        onProfileUpdatedSuccessfully = {},
                        initialSubScreen = initialProfileSubScreen,
                        onInitialSubScreenConsumed = { initialProfileSubScreen = ProfileSubScreen.NONE }
                    )
                }
            }
        }

        if (uiState.isSyncing) {
            SyncingOverlay()
        }
    }

    if (showCartPeriodSheet) {
        val planDuration = resolvePlanDays(visibleCustomPlan)
        ShoppingRangeBottomSheet(
            anchorDate = uiState.selectedDate ?: visibleCustomPlan?.startDate,
            maxAvailableDate = cartPreviewEndDate,
            onDismiss = { showCartPeriodSheet = false },
            onPresetSelected = { preset -> openProductsForPreset(preset) },
            planDurationDays = planDuration
        )
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
    onProductsClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCalorieSettingsClick: () -> Unit,
    customPlan: CustomPlan?,
    showMyPlanSection: Boolean = false
) {
    val sourceMenuItems = remember(uiState.allMenuItems, uiState.currentMenu) {
        uiState.allMenuItems.ifEmpty { uiState.currentMenu?.items ?: emptyList() }
    }
    val availableDates = remember(sourceMenuItems, customPlan) {
        buildAvailableDates(sourceMenuItems, customPlan)
    }
    val dateSelectorItems = remember(availableDates) { buildDateSelectorItems(availableDates) }
    val selectedDateId = uiState.selectedDate?.let { buildDateSelectorId(it) }
    val hasAvailableDates = availableDates.isNotEmpty()
    var pendingReplacement by remember { mutableStateOf<MealSection?>(null) }

    val showMyPlanButton = remember(showMyPlanSection, customPlan) {
        showMyPlanSection && customPlan != null && resolvePlanDays(customPlan) > 7
    }

    val anchorDate = uiState.selectedDate ?: availableDates.firstOrNull()
    val monthYearLabel = anchorDate?.let(::formatMonthYearForSelector).orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp)
    ) {
        item {
            HomeHeroSection(
                selectedDate = anchorDate,
                mealsCount = uiState.mealSections.size,
                hasMenu = uiState.hasMenu,
                isLoading = uiState.isLoading,
                onSearchClick = onSearchClick,
                onCartClick = onCartClick
            )
        }

        item {
            PlanDayCard(
                sections = uiState.mealSections,
                hasMenu = uiState.hasMenu,
                isLoading = uiState.isLoading,
                onGenerateMenu = onGenerateMenu
            )
        }

        item {
            QuickActionsSection(
                onProductsClick = onProductsClick,
                onStatisticsClick = onStatisticsClick,
                onRecipesClick = onSearchClick,
                onProfileClick = onProfileClick
            )
        }

        if (hasAvailableDates || showMyPlanButton) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    tonalElevation = 1.dp,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        if (monthYearLabel.isNotBlank() && availableDates.size > 1) {
                            SmartMealText(
                                text = monthYearLabel,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_month_year"),
                                color = HomeMutedText,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (hasAvailableDates) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_day_selector")
                            ) {
                                if (availableDates.size == 1) {
                                    SmartMealText(
                                        text = formatSelectedDateLabel(availableDates.first()),
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("home_selected_date_summary"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    DateSelector(
                                        items = dateSelectorItems,
                                        selectedStartId = selectedDateId,
                                        onItemClick = { dateId ->
                                            availableDates.firstOrNull { buildDateSelectorId(it) == dateId }
                                                ?.let(onDateSelected)
                                        }
                                    )
                                }
                            }

                            if (uiState.isCaloriesEnabled) {
                                // Spacer(modifier = Modifier.height(16.dp))
                                // CalorieGoalButton removed from here
                            }
                        }

                        if (showMyPlanButton) {
                            if (hasAvailableDates) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = HomeCardBorder)
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            SmartMealText(
                                text = "План на период",
                                style = MaterialTheme.typography.labelLarge,
                                color = HomeMutedText,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            MyPlanSection(
                                customPlan = customPlan,
                                selectedDate = uiState.selectedDate,
                                onDateSelectedFromPlan = onDateSelected,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_my_plan")
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isLoading && !uiState.hasMenu) {
            item {
                Box(modifier = Modifier.testTag("home_loading")) {
                    HomeScreenSkeleton()
                }
            }
        } else {
            if (!uiState.hasMenu) {
                item {
                    EmptyMenuStateCard(
                        onGenerateMenu = onGenerateMenu
                    )
                }
            } else if (availableDates.isEmpty()) {
                item {
                    ExpiredMenuStateCard(
                        onReselectPlan = onReselectPlan
                    )
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

        uiState.error?.let { errorMessage ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmartMealText(
                            text = errorMessage,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = onDismissError) {
                            SmartMealText(
                                text = "Скрыть",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
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
private fun PlanDayCard(
    sections: List<MealSection>,
    hasMenu: Boolean,
    isLoading: Boolean,
    onGenerateMenu: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmartMealText(
                    text = "План на день",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!hasMenu && !isLoading) {
                    TextButton(onClick = onGenerateMenu) {
                        SmartMealText(
                            text = "Создать",
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (hasMenu && sections.isNotEmpty()) {
                sections.take(3).forEachIndexed { index, section ->
                    PlanMealRow(
                        title = section.title,
                        recipeTitle = section.meal.recipe_title,
                        accent = when (index) {
                            0 -> SmartMealOrange
                            1 -> SmartMealGreen
                            else -> SmartMealPurple
                        }
                    )
                }
            } else {
                SmartMealText(
                    text = if (isLoading) "Собираем рацион..." else "План еще не сформирован",
                    color = HomeMutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PlanMealRow(
    title: String,
    recipeTitle: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = accent.copy(alpha = 0.14f)
        ) {
            Icon(
                imageVector = Icons.Default.RestaurantMenu,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            SmartMealText(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            SmartMealText(
                text = recipeTitle,
                style = MaterialTheme.typography.bodySmall,
                color = HomeMutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    onProductsClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SmartMealText(
            text = "Быстрые действия",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 2.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                title = "Продукты",
                icon = Icons.Default.ShoppingCart,
                accent = SmartMealOrange,
                onClick = onProductsClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Статистика",
                icon = Icons.Default.BarChart,
                accent = SmartMealGreen,
                onClick = onStatisticsClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Рацион",
                icon = Icons.Default.RestaurantMenu,
                accent = SmartMealBlue,
                onClick = onRecipesClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Профиль",
                icon = Icons.Default.Person,
                accent = SmartMealHeart,
                onClick = onProfileClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp)
                )
            }
            SmartMealText(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeMutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeHeroSection(
    selectedDate: Date?,
    mealsCount: Int,
    hasMenu: Boolean,
    isLoading: Boolean,
    onSearchClick: () -> Unit,
    onCartClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 8.dp,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(HomeHeroStart, HomeHeroEnd)
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SmartMealText(
                            text = "Привет!",
                            fontSize = 24.sp,
                            modifier = Modifier.testTag("home_title"),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SmartMealText(
                            text = selectedDate?.let(::formatHeroDate)
                                ?: "Подберем рацион под ваш текущий план",
                            style = MaterialTheme.typography.bodyLarge,
                            color = HomeMutedText
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(
                            onClick = onCartClick,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = PrimaryGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("home_cart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Корзина"
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroStatChip(
                        icon = Icons.Default.CalendarMonth,
                        label = if (hasMenu) "План активен" else "Нужна генерация"
                    )
                    HeroStatChip(
                        icon = Icons.Default.RestaurantMenu,
                        label = if (isLoading) "Обновляем" else "$mealsCount блюда"
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.82f),
        border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(16.dp)
            )
            SmartMealText(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyMenuStateCard(
    onGenerateMenu: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_empty_state"),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = LightGreenBg
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            SmartMealText(
                text = "Меню еще не сформировано",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            SmartMealText(
                text = "Сгенерируем план питания и сразу подготовим список блюд по дням.",
                style = MaterialTheme.typography.bodyMedium,
                color = HomeMutedText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onGenerateMenu,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("home_generate_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                SmartMealText(
                    text = "Сгенерировать меню",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ExpiredMenuStateCard(
    onReselectPlan: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_expired_state"),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = HomeSoftSurface
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            SmartMealText(
                text = "Текущий рацион завершился",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            SmartMealText(
                text = "Выберите новый период, чтобы продолжить план и заново собрать продукты.",
                style = MaterialTheme.typography.bodyMedium,
                color = HomeMutedText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onReselectPlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("home_reselect_plan_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                SmartMealText(
                    text = "Выбрать новый рацион",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = HomeSoftSurface
            ) {
                SmartMealText(
                    text = title,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
            }

            CircleIconButton(
                iconType = CircleIconType.REPLACE,
                onClick = onReplaceClick,
                backgroundColor = Color.Transparent,
                contentColor = Color.Black.copy(alpha = 0.64f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("home_replace_$sectionId")
            )
        }

        AnimatedContent(
            targetState = meal,
            transitionSpec = {
                (fadeIn(animationSpec = tween(360)) + scaleIn(initialScale = 0.96f)) togetherWith
                    (fadeOut(animationSpec = tween(240)) + scaleOut(targetScale = 0.96f))
            },
            contentKey = { it.recipe },
            label = "MealReplacementAnimation"
        ) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
            shape = RoundedCornerShape(28.dp),
            color = ModalBackground,
            shadowElevation = 10.dp
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
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(14.dp))

                SmartMealText(
                    text = "Подберем другое блюдо для этого приема пищи. Избранное сохранится, а меню обновится сразу после замены.",
                    fontSize = 16.sp,
                    color = Color.Black.copy(alpha = 0.78f),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(26.dp))

                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("home_replace_confirm_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    )
                ) {
                    SmartMealText("Заменить блюдо", color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("home_replace_cancel_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    )
                ) {
                    SmartMealText("Отменить", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingRangeBottomSheet(
    anchorDate: Date?,
    maxAvailableDate: Date?,
    onDismiss: () -> Unit,
    onPresetSelected: (ShoppingPeriodPreset) -> Unit,
    planDurationDays: Long
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val availablePresets = remember(planDurationDays) {
        ShoppingPeriodPreset.entries.filter { preset ->
            // Всегда показываем "На 1 день"
            if (preset == ShoppingPeriodPreset.DAY) return@filter true
            
            // Показываем остальные только если они помещаются в текущий план
            planDurationDays >= preset.days
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmartMealText(
                text = "Собрать список продуктов",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            SmartMealText(
                text = "Выберите период, и мы сразу откроем продукты с уже подготовленным диапазоном.",
                style = MaterialTheme.typography.bodyMedium,
                color = HomeMutedText
            )
            Spacer(modifier = Modifier.height(4.dp))

            availablePresets.forEach { preset ->
                ShoppingPresetCard(
                    preset = preset,
                    preview = buildShoppingPreview(
                        preset = preset,
                        anchorDate = anchorDate,
                        maxAvailableDate = maxAvailableDate
                    ),
                    onClick = { onPresetSelected(preset) }
                )
            }
        }
    }
}

@Composable
private fun ShoppingPresetCard(
    preset: ShoppingPeriodPreset,
    preview: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = HomeSoftSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmartMealText(
                text = preset.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            SmartMealText(
                text = preset.actionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = HomeMutedText
            )
            SmartMealText(
                text = preview,
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryGreen,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SyncingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
            .clickable(enabled = true, onClick = {})
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = PrimaryGreen)
                Spacer(modifier = Modifier.height(14.dp))
                SmartMealText(
                    text = "Сверяем актуальные данные...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                SmartMealText(
                    text = "Ищем последние обновления рациона",
                    style = MaterialTheme.typography.bodySmall,
                    color = HomeMutedText
                )
            }
        }
    }
}

private class HomeViewModelFactory(
    private val application: android.app.Application,
    private val preferences: SetupPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(application, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private class ProductListViewModelFactory(
    private val menuApi: MenuApi,
    private val preferences: SetupPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductListViewModel(menuApi, preferences) as T
        }
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
            return ProfileViewModel(
                api,
                preferences,
                onCriticalSettingsChanged,
                onSimpleSettingsChanged,
                onMenuManualChanged
            ) as T
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
    val menuDates = menuItems
        .mapNotNull { item ->
            try {
                formatter.parse(item.actual_date)
            } catch (_: Exception) {
                null
            }
        }
        .map(::normalizeDateStatic)
        .filter { !it.before(normalizedToday) }
        .distinct()
        .sorted()

    if (menuDates.isNotEmpty() || customPlan == null) {
        return menuDates
    }

    val startDate = maxOf(normalizeDateStatic(customPlan.startDate), normalizedToday)
    val endDate = normalizeDateStatic(customPlan.endDate)
    if (startDate.after(endDate)) return emptyList()

    val dates = mutableListOf<Date>()
    val calendar = Calendar.getInstance().apply { time = startDate }
    while (!calendar.time.after(endDate)) {
        dates += calendar.time
        calendar.add(Calendar.DATE, 1)
    }
    return dates
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
    val calendar = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.time
}

private fun formatMonthYearForSelector(date: Date): String {
    return SimpleDateFormat("LLLL yyyy", Locale("ru"))
        .format(date)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
}

private fun formatHeroDate(date: Date): String {
    return SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
        .format(date)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
}

private fun buildShoppingPreview(
    preset: ShoppingPeriodPreset,
    anchorDate: Date?,
    maxAvailableDate: Date?
): String {
    val start = normalizeDateStatic(anchorDate ?: Date())
    val rawEnd = Calendar.getInstance().apply {
        time = start
        add(Calendar.DATE, preset.days - 1)
    }.time
    val end = if (maxAvailableDate != null) minOf(rawEnd, normalizeDateStatic(maxAvailableDate)) else rawEnd

    val formatter = SimpleDateFormat("d MMM", Locale("ru"))
    return if (start == end) {
        formatter.format(start)
    } else {
        "${formatter.format(start)} - ${formatter.format(end)}"
    }
}

private fun resolvePlanDays(customPlan: CustomPlan?): Long {
    if (customPlan == null) return 0
    val diff = normalizeDateStatic(customPlan.endDate).time - normalizeDateStatic(customPlan.startDate).time
    return (diff / (1000L * 60L * 60L * 24L)) + 1L
}

internal fun mergeUpdatedMenuItemIntoState(state: HomeUiState, updatedItem: MenuItemDto): HomeUiState {
    val updatedAll = state.allMenuItems.map { if (it.id == updatedItem.id) updatedItem else it }
    val updatedCurrentMenu = state.currentMenu?.copy(
        items = state.currentMenu.items?.map { if (it.id == updatedItem.id) updatedItem else it }
    )
    return state.copy(
        allMenuItems = updatedAll,
        currentMenu = updatedCurrentMenu
    )
}

internal fun resolveCustomDays(range: Pair<Long, Long>?): Int? {
    if (range == null) return null
    return ((range.second - range.first) / (1000L * 60L * 60L * 24L)).toInt() + 1
}
