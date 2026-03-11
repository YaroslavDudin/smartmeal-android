package com.example.smartmeal.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
import com.example.smartmeal.feature.auth.presentation.WelcomeScreen
import com.example.smartmeal.feature.auth.presentation.LoginRegisterForm
import com.example.smartmeal.feature.auth.presentation.AuthViewModel

/**
 * Отдельный компонент для управления навигацией.
 * Принимает [navController], который создается выше в MainActivity.
 */
@Composable
fun SmartMealNavGraph(navController: NavHostController) {
    
    // В реальном приложении лучше использовать DI (Hilt/Koin)
    // Сейчас создаем ViewModel "вручную" для демонстрации
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenManager = remember { com.example.smartmeal.data.local.TokenManager(context) }
    val authApi = remember { RetrofitClient.createService(AuthApi::class.java, tokenManager) }
    val authViewModel: AuthViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authApi, tokenManager) as T
        }
    })

    // NavHost - это "контейнер", в котором сменяются экраны
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route // Указываем, откуда стартует приложение
    ) {
        
        // --- ЗОНА АВТОРИЗАЦИИ / ОНБОРДИНГА ---
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateNext = {
                    navController.navigate(Screen.AuthForm.route)
                }
            )
        }

        composable(route = Screen.AuthForm.route) {
            LoginRegisterForm(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // --- ЗОНА ОСНОВНОГО ПРИЛОЖЕНИЯ ---
        composable(route = Screen.Home.route) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Главный экран (в разработке)", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
