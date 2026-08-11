package com.example.pw

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun ensureLoggedIn() {
        // 1. Wait for EITHER the Debug Login button OR the Navigation Bar
        // This confirms the app has initialized and is showing one of the two main states.
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("debug_login").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithTag("nav_passwords").fetchSemanticsNodes().isNotEmpty()
        }

        // 2. If we are on the Auth screen, click the Debug Login button
        val debugLogin = composeTestRule.onAllNodesWithTag("debug_login")
        if (debugLogin.fetchSemanticsNodes().isNotEmpty()) {
            debugLogin.onFirst().performClick()
            
            // 3. After clicking, wait for the app to navigate to the main screen
            composeTestRule.waitUntil(30000) {
                composeTestRule.onAllNodesWithTag("nav_passwords").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @Test
    fun testNavigationToExpenses() {
        ensureLoggedIn()

        // Navigate to Expenses
        composeTestRule.onNodeWithTag("nav_expenses").performClick()
        
        // Wait for the header to appear to confirm navigation completed
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag("header_expenses").fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithTag("header_expenses").assertIsDisplayed()
    }

    @Test
    fun testNavigationToSubs() {
        ensureLoggedIn()

        // Navigate to Subs
        composeTestRule.onNodeWithTag("nav_subs").performClick()
        
        // Wait for the header to appear
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag("header_subscriptions").fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithTag("header_subscriptions").assertIsDisplayed()
    }

    @Test
    fun testNavigationToPasswords() {
        ensureLoggedIn()

        // Go to Expenses first to ensure we are moving between screens
        composeTestRule.onNodeWithTag("nav_expenses").performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("header_expenses").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Go back to Passwords
        composeTestRule.onNodeWithTag("nav_passwords").performClick()
        
        // Wait for header
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag("header_my_passwords").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("header_my_passwords").assertIsDisplayed()
    }
}
