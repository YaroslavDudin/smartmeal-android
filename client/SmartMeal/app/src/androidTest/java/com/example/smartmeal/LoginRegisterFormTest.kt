package com.example.smartmeal

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.smartmeal.feature.auth.presentation.AuthState
import com.example.smartmeal.feature.auth.presentation.LoginRegisterFormContent
import com.example.smartmeal.ui.theme.SmartMealTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginRegisterFormTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginMode_showsLoginFields() {
        composeTestRule.setContent {
            SmartMealTheme {
                LoginRegisterFormContent(
                    authState = AuthState.Idle,
                    onAuthSuccess = {},
                    onLogin = { _, _ -> },
                    onRegister = { _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("auth_toggle_login").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_email").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_password").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_forgot").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("auth_username").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("auth_confirm_password").assertCountEquals(0)
    }

    @Test
    fun registerMode_showsRegisterFields() {
        composeTestRule.setContent {
            SmartMealTheme {
                LoginRegisterFormContent(
                    authState = AuthState.Idle,
                    onAuthSuccess = {},
                    onLogin = { _, _ -> },
                    onRegister = { _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("auth_toggle_register").performClick()
        composeTestRule.onNodeWithTag("auth_username").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_confirm_password").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("auth_forgot").assertCountEquals(0)
    }

    @Test
    fun loginSubmit_callsOnLogin() {
        var emailValue = ""
        var passValue = ""

        composeTestRule.setContent {
            SmartMealTheme {
                LoginRegisterFormContent(
                    authState = AuthState.Idle,
                    onAuthSuccess = {},
                    onLogin = { email, pass ->
                        emailValue = email
                        passValue = pass
                    },
                    onRegister = { _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("auth_email").performTextInput("user@example.com")
        composeTestRule.onNodeWithTag("auth_password").performTextInput("password123")
        composeTestRule.onNodeWithTag("auth_submit").performClick()

        assertEquals("user@example.com", emailValue)
        assertEquals("password123", passValue)
    }

    @Test
    fun registerSubmit_callsOnRegister() {
        var userValue = ""
        var emailValue = ""
        var passValue = ""
        var confirmValue = ""

        composeTestRule.setContent {
            SmartMealTheme {
                LoginRegisterFormContent(
                    authState = AuthState.Idle,
                    onAuthSuccess = {},
                    onLogin = { _, _ -> },
                    onRegister = { user, email, pass, confirm ->
                        userValue = user
                        emailValue = email
                        passValue = pass
                        confirmValue = confirm
                    },
                    initialIsLoginMode = false
                )
            }
        }

        composeTestRule.onNodeWithTag("auth_username").performScrollTo().performTextInput("Ivan")
        composeTestRule.onNodeWithTag("auth_email").performScrollTo().performTextInput("ivan@example.com")
        composeTestRule.onNodeWithTag("auth_password").performScrollTo().performTextInput("password123")
        composeTestRule.onNodeWithTag("auth_confirm_password").performScrollTo().performTextInput("password123")
        composeTestRule.onNodeWithTag("auth_submit").performScrollTo().performClick()

        assertEquals("Ivan", userValue)
        assertEquals("ivan@example.com", emailValue)
        assertEquals("password123", passValue)
        assertEquals("password123", confirmValue)
    }
}
