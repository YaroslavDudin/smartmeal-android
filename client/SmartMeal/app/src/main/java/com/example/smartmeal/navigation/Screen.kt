package com.example.smartmeal.ui.navigation

/**
 * Sealed class хранит все маршруты (экраны) нашего приложения.
 * Это защищает от опечаток в строках при вызове навигации.
 */
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome_screen")
    object Test : Screen("test_screen")
    object AuthForm : Screen("login_register_form")
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword : Screen("reset-password?uid={uid}&token={token}") {
        fun createRoute(uid: String, token: String): String = "reset-password?uid=$uid&token=$token"
    }
    object Home : Screen("home_screen")
    object RecipeList : Screen("recipe_list")
    object ServiceUnavailable : Screen("service_unavailable")

    // Флоу первоначальной настройки профиля (показывается после авторизации, если профиль не настроен)
    object SetupIntro : Screen("setup_intro")
    object SetupStep1 : Screen("setup_step1")
    object SetupStep2 : Screen("setup_step2?reselect={reselect}") {
        fun createRoute(reselect: Boolean = false): String = "setup_step2?reselect=$reselect"
    }
    object SetupStep3 : Screen("setup_step3")

    object RecipeDetail : Screen("recipe_detail/{recipeId}?portionSize={portionSize}&menuItemId={menuItemId}") {
        fun createRoute(recipeId: Int, portionSize: Int, menuItemId: Int? = null): String {
            var url = "recipe_detail/$recipeId?portionSize=$portionSize"
            if (menuItemId != null) {
                url += "&menuItemId=$menuItemId"
            }
            return url
        }
    }
}
