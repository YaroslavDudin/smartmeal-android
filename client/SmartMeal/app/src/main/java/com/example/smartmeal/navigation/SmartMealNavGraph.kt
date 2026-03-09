package com.example.smartmeal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.smartmeal.feature.auth.presentation.WelcomeScreen
import com.example.smartmeal.feature.auth.presentation.LoginRegisterForm

/**
 * Отдельный компонент для управления навигацией.
 * Принимает [navController], который создается выше в MainActivity.
 */
@Composable
fun SmartMealNavGraph(navController: NavHostController) {
    // NavHost - это "контейнер", в котором сменяются экраны
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route // Указываем, откуда стартует приложение
    ) {
        
        // --- ЗОНА АВТОРИЗАЦИИ / ОНБОРДИНГА ---
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                // Передаем логику перехода в лямбду
                onNavigateNext = {
                    navController.navigate(Screen.AuthForm.route) {
                        // Пример: если нужно, чтобы по кнопке "Назад" нельзя было 
                        // вернуться на экран Welcome, можно очистить стек:
                        /*
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        */
                    }
                }
            )
        }

        // --- ЗОНА ОСНОВНОГО ПРИЛОЖЕНИЯ ---
        composable(route = Screen.AuthForm.route) {
            LoginRegisterForm()
        }
    }
}