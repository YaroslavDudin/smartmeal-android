package com.example.smartmeal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.smartmeal.ui.screens.TestScreen
import com.example.smartmeal.ui.screens.auth.WelcomeScreen

@Composable
fun SmartMealNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        // ВРЕМЕННО меняем Welcome на Test
        startDestination = "test"  // Было "welcome"
    ) {
        composable(route = "welcome") {
            WelcomeScreen(
                onNavigateNext = {
                    navController.navigate("test")
                }
            )
        }

        composable(route = "test") {
            TestScreen()
        }
    }
}