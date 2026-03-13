package com.example.smartmeal.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.smartmeal.data.api.RetrofitClient
import com.example.smartmeal.feature.auth.data.api.AuthApi
import com.example.smartmeal.feature.auth.presentation.AuthViewModel
import com.example.smartmeal.feature.auth.presentation.LoginRegisterForm
import com.example.smartmeal.feature.auth.presentation.WelcomeScreen
import com.example.smartmeal.feature.home.presentation.HomeScreen
import com.example.smartmeal.feature.setup.data.api.SetupApi
import com.example.smartmeal.feature.setup.presentation.SetupIntroScreen
import com.example.smartmeal.feature.setup.presentation.SetupStep1Screen
import com.example.smartmeal.feature.setup.presentation.SetupStep2Screen
import com.example.smartmeal.feature.setup.presentation.SetupStep3Screen
import com.example.smartmeal.feature.setup.presentation.SetupViewModel

/**
 * Граф навигации приложения.
 * Принимает [navController], который создаётся в MainActivity.
 */
@Composable
fun SmartMealNavGraph(navController: NavHostController) {

    // В реальном приложении лучше использовать DI (Hilt/Koin)
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenManager = remember { com.example.smartmeal.data.local.TokenManager(context) }

    RetrofitClient.init(tokenManager)

    val authApi = remember { RetrofitClient.createService(AuthApi::class.java) }
    val setupApi = remember { RetrofitClient.createService(SetupApi::class.java) }

    val authViewModel: AuthViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authApi, tokenManager) as T
        }
    })

    val setupViewModel: SetupViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return SetupViewModel(setupApi) as T
        }
    })

    // Стартовый экран: если токен есть — SetupIntro проверит профиль и решит куда идти
    val startDestination = if (tokenManager.getAccessToken() != null) {
        Screen.SetupIntro.route
    } else {
        Screen.AuthForm.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {

        // --- Зона авторизации ---

        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateNext = { navController.navigate(Screen.AuthForm.route) }
            )
        }

        composable(route = Screen.AuthForm.route) {
            LoginRegisterForm(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.SetupIntro.route) {
                        popUpTo(Screen.AuthForm.route) { inclusive = true }
                    }
                }
            )
        }

        // --- Зона настройки профиля (онбординг) ---

        composable(route = Screen.SetupIntro.route) {
            SetupIntroScreen(
                viewModel = setupViewModel,
                onStartSetup = { navController.navigate(Screen.SetupStep1.route) },
                onAlreadyConfigured = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SetupIntro.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.SetupStep1.route) {
            SetupStep1Screen(
                viewModel = setupViewModel,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Screen.SetupStep2.route) },
            )
        }

        composable(route = Screen.SetupStep2.route) {
            SetupStep2Screen(
                viewModel = setupViewModel,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Screen.SetupStep3.route) },
            )
        }

        composable(route = Screen.SetupStep3.route) {
            SetupStep3Screen(
                viewModel = setupViewModel,
                onBack = { navController.popBackStack() },
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SetupIntro.route) { inclusive = true }
                    }
                },
            )
        }

        // --- Зона основного приложения ---

        composable(route = Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    authViewModel.logout()
                    setupViewModel.reset()
                    navController.navigate(Screen.AuthForm.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
