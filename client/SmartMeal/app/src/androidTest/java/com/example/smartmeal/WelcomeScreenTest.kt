package com.example.smartmeal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.smartmeal.ui.screens.auth.WelcomeScreen
import org.junit.Rule
import org.junit.Test
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Assert.assertTrue

class WelcomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_displaysTextButtonImage() {
        composeTestRule.setContent {
            SmartMealTheme {
                WelcomeScreen()
            }
        }
        
        // Проверяем картинку
        composeTestRule
            .onNodeWithTag("food_image")
            .performScrollTo()
            .assertIsDisplayed()

        // Проверяем главный заголовок
        composeTestRule
            .onNodeWithText("SmartMeal")
            .performScrollTo()
            .assertIsDisplayed()

        // Проверяем подзаголовок (используем substring для надежности из-за \n)
        composeTestRule
            .onNodeWithText("Сгенерируйте своё недельное", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        // Проверяем кнопку
        composeTestRule
            .onNodeWithText("Начать")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun welcomeScreen_buttonClick_callsNavigation() {
        var clicked = false
        composeTestRule.setContent {
            SmartMealTheme {
                WelcomeScreen(
                    onNavigateNext = { clicked = true }
                )
            }
        }

        // Находим кнопку по тексту, скроллим и кликаем
        composeTestRule
            .onNodeWithText("Начать")
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
        assertTrue("Клик по кнопке не вызвал навигацию", clicked)
    }
}
