package com.example.smartmeal.ui.navigation

/**
 * Sealed class хранит все маршруты (экраны) нашего приложения.
 * Это защищает от опечаток в строках при вызове навигации.
 */
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome_screen")
    object Test : Screen("test_screen")
    // В будущем тут появятся:
    // object Home : Screen("home_screen")
    // object RecipeDetail : Screen("recipe_detail/{recipeId}") 
}