package com.example.smartmeal.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.smartmeal.data.api.RetrofitClient
import com.example.smartmeal.data.local.SetupPreferences
import com.example.smartmeal.feature.auth.data.api.AuthApi
import com.example.smartmeal.feature.auth.presentation.AuthViewModel
import com.example.smartmeal.feature.auth.presentation.LoginRegisterForm
import com.example.smartmeal.feature.auth.presentation.WelcomeScreen
import com.example.smartmeal.feature.home.presentation.HomeScreen
import com.example.smartmeal.feature.recipes.data.api.RecipeApi
import com.example.smartmeal.feature.recipes.presentation.RecipeDetailScreen
import com.example.smartmeal.feature.recipes.presentation.RecipeDetailViewModel
import com.example.smartmeal.feature.sandbox.TestScreen
import com.example.smartmeal.feature.setup.data.api.SetupApi
import com.example.smartmeal.feature.setup.presentation.SetupIntroScreen
import com.example.smartmeal.feature.setup.presentation.SetupStep1Screen
import com.example.smartmeal.feature.setup.presentation.SetupStep2Screen
import com.example.smartmeal.feature.setup.presentation.SetupStep3Screen
import com.example.smartmeal.feature.setup.presentation.SetupViewModel

import androidx.navigation.navDeepLink
import com.example.smartmeal.feature.auth.presentation.ForgotPasswordScreen
import com.example.smartmeal.feature.auth.presentation.ResetPasswordScreen

import com.example.smartmeal.feature.recipes.presentation.RecipeListScreen
import com.example.smartmeal.feature.recipes.presentation.RecipeListViewModel

private data class NavigationBackgroundPalette(
    val start: Color,
    val end: Color
)

private val AuthBackgroundPalette = NavigationBackgroundPalette(
    start = Color(0xFFFFFFFF),
    end = Color(0xFFFBFBFB)
)
private val SetupBackgroundPalette = NavigationBackgroundPalette(
    start = Color(0xFFFFFFFF),
    end = Color(0xFFFBFBFB)
)
private val AppBackgroundPalette = NavigationBackgroundPalette(
    start = Color(0xFFFFFFFF),
    end = Color(0xFFFBFBFB)
)
private val DetailBackgroundPalette = NavigationBackgroundPalette(
    start = Color(0xFFFFFFFF),
    end = Color(0xFFFBFBFB)
)

private val routeEnterTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(durationMillis = 320, delayMillis = 40)) +
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            initialOffset = { fullWidth -> (fullWidth * 0.08f).toInt() }
        ) +
        scaleIn(
            initialScale = 0.985f,
            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
        )
}

private val routeExitTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(durationMillis = 220)) +
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            targetOffset = { fullWidth -> (fullWidth * 0.04f).toInt() }
        ) +
        scaleOut(
            targetScale = 1.01f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )
}

private val routePopEnterTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(durationMillis = 320, delayMillis = 40)) +
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            initialOffset = { fullWidth -> (fullWidth * 0.08f).toInt() }
        ) +
        scaleIn(
            initialScale = 0.985f,
            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
        )
}

private val routePopExitTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(durationMillis = 220)) +
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            targetOffset = { fullWidth -> (fullWidth * 0.04f).toInt() }
        ) +
        scaleOut(
            targetScale = 1.01f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )
}

/**
 * Главный граф навигации приложения.
 * Принимает [navController], который создается в MainActivity.
 */
@Composable
fun SmartMealNavGraph(navController: NavHostController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenManager = remember { com.example.smartmeal.data.local.TokenManager(context) }
    val backStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(tokenManager) {
        RetrofitClient.init(tokenManager)
    }

    val authApi = remember { RetrofitClient.createService(AuthApi::class.java) }
    val setupApi = remember { RetrofitClient.createService(SetupApi::class.java) }
    val recipeApi = remember { RetrofitClient.createService(RecipeApi::class.java) }
    val menuApi = remember { RetrofitClient.createService(com.example.smartmeal.feature.home.data.api.MenuApi::class.java) }
    val setupPreferences = remember { SetupPreferences(context) }

    val authViewModel: AuthViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authApi, tokenManager, setupPreferences) as T
        }
    })

    val setupViewModel: SetupViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return SetupViewModel(setupApi, setupPreferences) as T
        }
    })

    val startDestination = if (tokenManager.getAccessToken() != null) {
        Screen.SetupIntro.route
    } else {
        Screen.Welcome.route
    }

    val currentRoute = backStackEntry?.destination?.route.orEmpty()
    val backgroundPalette = remember(currentRoute) {
        when {
            currentRoute == Screen.Welcome.route ||
                currentRoute == Screen.AuthForm.route ||
                currentRoute == Screen.ForgotPassword.route ||
                currentRoute == Screen.ResetPassword.route -> AuthBackgroundPalette

            currentRoute == Screen.SetupIntro.route ||
                currentRoute == Screen.SetupStep1.route ||
                currentRoute == Screen.SetupStep2.route ||
                currentRoute == Screen.SetupStep3.route -> SetupBackgroundPalette

            currentRoute == Screen.RecipeList.route ||
                currentRoute.startsWith("recipe_detail") -> DetailBackgroundPalette

            else -> AppBackgroundPalette
        }
    }
    val animatedBackgroundStart by animateColorAsState(
        targetValue = backgroundPalette.start,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "navBackgroundStart"
    )
    val animatedBackgroundEnd by animateColorAsState(
        targetValue = backgroundPalette.end,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "navBackgroundEnd"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(animatedBackgroundStart, animatedBackgroundEnd)
                )
            )
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
            enterTransition = routeEnterTransition,
            exitTransition = routeExitTransition,
            popEnterTransition = routePopEnterTransition,
            popExitTransition = routePopExitTransition
        ) {

        fun navigateToHomeClearingOnboardingStack() {
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }

        // Зона авторизации
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateNext = { navController.navigate(Screen.AuthForm.route) }
            )
        }

        composable(route = Screen.Test.route) {
            TestScreen(onBack = { navController.popBackStack() })
        }

        composable(route = Screen.AuthForm.route) {
            LoginRegisterForm(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.SetupIntro.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onNavigateToSandbox = {
                    navController.navigate(Screen.Test.route)
                }
            )
        }

        composable(route = Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
                navArgument("token") { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "smartmeal://reset-password?uid={uid}&token={token}"
                }
            )
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val token = backStackEntry.arguments?.getString("token") ?: ""
            
            // Сбрасываем состояние при входе на экран, чтобы убрать Loading или Error от прошлых попыток
            LaunchedEffect(uid, token) {
                authViewModel.resetAuthState()
            }

            ResetPasswordScreen(
                viewModel = authViewModel,
                uid = uid,
                token = token,
                onSuccess = {
                    authViewModel.resetAuthState()
                    navController.navigate(Screen.AuthForm.route) {
                        popUpTo(Screen.AuthForm.route) { inclusive = true }
                    }
                }
            )
        }

        // Зона настройки профиля
        composable(route = Screen.SetupIntro.route) {
            SetupIntroScreen(
                viewModel = setupViewModel,
                onStartSetup = { navController.navigate(Screen.SetupStep1.route) },
                onAlreadyConfigured = { navigateToHomeClearingOnboardingStack() }
            )
        }

        composable(route = Screen.SetupStep1.route) {
            SetupStep1Screen(
                viewModel = setupViewModel,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Screen.SetupStep2.createRoute()) },
            )
        }

        composable(
            route = Screen.SetupStep2.route,
            arguments = listOf(
                navArgument("reselect") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val isReselectFlow = backStackEntry.arguments?.getBoolean("reselect") ?: false
            SetupStep2Screen(
                viewModel = setupViewModel,
                onBack = { navController.popBackStack() },
                onNext = {
                    if (isReselectFlow) {
                        setupPreferences.setPendingPlanRegeneration(true)
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.SetupStep3.route)
                    }
                },
                nextButtonText = if (isReselectFlow) "Готово" else "Дальше",
            )
        }

        composable(route = Screen.SetupStep3.route) {
            SetupStep3Screen(
                viewModel = setupViewModel,
                onBack = { navController.popBackStack() },
                onComplete = {
                    setupPreferences.setPendingPlanRegeneration(true)
                    navigateToHomeClearingOnboardingStack()
                },
            )
        }

        // Зона основного приложения
        composable(route = Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    authViewModel.logout()
                    setupViewModel.reset()
                },
                onLogoutSuccess = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onReselectPlan = {
                    navController.navigate(Screen.SetupStep2.createRoute(reselect = true))
                },
                onRecipeClick = { recipeId, menuItemId ->
                    val portionSize = setupPreferences.getPortionSize()
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId, portionSize, menuItemId))
                },
                onSearchClick = {
                    navController.navigate(Screen.RecipeList.route)
                }
            )
        }

        composable(route = Screen.RecipeList.route) {
            val recipeListViewModel: RecipeListViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return RecipeListViewModel(recipeApi) as T
                }
            })

            RecipeListScreen(
                viewModel = recipeListViewModel,
                onBack = { navController.popBackStack() },
                onRecipeClick = { recipeId ->
                    val portionSize = setupPreferences.getPortionSize()
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId, portionSize, null))
                }
            )
        }

        composable(
            route = Screen.RecipeDetail.route,
            arguments = listOf(
                navArgument("recipeId") { type = NavType.IntType },
                navArgument("portionSize") { 
                    type = NavType.IntType 
                    defaultValue = -1 // Будем брать из настроек, если -1
                },
                navArgument("menuItemId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "smartmeal://recipe/{recipeId}"
                },
                navDeepLink {
                    uriPattern = "https://smartmeal.com/recipe/{recipeId}"
                }
            )
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getInt("recipeId") ?: 0
            val portionSizeRaw = backStackEntry.arguments?.getInt("portionSize") ?: -1
            val portionSize = if (portionSizeRaw != -1) portionSizeRaw else setupPreferences.getPortionSize()
            val menuItemIdRaw = backStackEntry.arguments?.getInt("menuItemId") ?: -1
            val menuItemId = if (menuItemIdRaw != -1) menuItemIdRaw else null

            val recipeViewModel: RecipeDetailViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return RecipeDetailViewModel(recipeApi, menuApi, setupPreferences) as T
                }
            })

            RecipeDetailScreen(
                recipeId = recipeId,
                menuItemId = menuItemId,
                portionSize = portionSize,
                viewModel = recipeViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        }
    }
}
