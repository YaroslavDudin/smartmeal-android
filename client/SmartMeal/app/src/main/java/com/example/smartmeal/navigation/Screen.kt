package com.example.smartmeal.ui.navigation

/**
 * Sealed class хранит все маршруты (экраны) нашего приложения.
 * Это защищает от опечаток в строках при вызове навигации.
 */
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome_screen")
    object Test : Screen("test_screen")
    object AuthForm : Screen("login_register_form")
    object Home : Screen("home_screen")

    // Флоу первоначальной настройки профиля (показывается после авторизации, если профиль не настроен)
    object SetupIntro : Screen("setup_intro")
    object SetupStep1 : Screen("setup_step1")
    object SetupStep2 : Screen("setup_step2")
    object SetupStep3 : Screen("setup_step3")

    // object RecipeDetail : Screen("recipe_detail/{recipeId}")
}