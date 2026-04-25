package com.example.smartmeal.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

/**
 * Главный граф навигации приложения.
 * Принимает [navController], который создается в MainActivity.
 */
@Composable
fun SmartMealNavGraph(navController: NavHostController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenManager = remember { com.example.smartmeal.data.local.TokenManager(context) }

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

    NavHost(
        navController = navController,
        startDestination = startDestination,
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
