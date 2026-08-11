package com.example.pw

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SubscriptionsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun loginAndGoToSubs() {
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("debug_login").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("debug_login").performClick()
        
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("nav_subs").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("nav_subs").performClick()

        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("fab_add_sub").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testAddButtonOpensForm() {
        loginAndGoToSubs()
        composeTestRule.onNodeWithTag("fab_add_sub").performClick()
        composeTestRule.onNodeWithText("Subscription Details").assertIsDisplayed()
    }

    @Test
    fun testSubmitAddsSubscription() {
        loginAndGoToSubs()
        val uniqueName = "TestSub-${UUID.randomUUID().toString().take(5)}"
        
        composeTestRule.onNodeWithTag("fab_add_sub").performClick()
        composeTestRule.onNodeWithTag("input_sub_name").performTextInput(uniqueName)
        composeTestRule.onNodeWithTag("input_sub_amount").performTextInput("$9.99")
        composeTestRule.onNodeWithTag("input_sub_date").performTextInput("1st")
        composeTestRule.onNodeWithTag("btn_save_sub").performClick()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText(uniqueName).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(uniqueName).assertIsDisplayed()
    }

    @Test
    fun testDeleteSubscription() {
        loginAndGoToSubs()
        val uniqueName = "DeleteSub-${UUID.randomUUID().toString().take(5)}"
        addMockSubscription(uniqueName, "$0", "Never")

        composeTestRule.onNode(
            hasTestTag("btn_delete_sub") and hasAnyAncestor(hasTestTag("sub_card_$uniqueName"))
        ).performClick()

        composeTestRule.onNodeWithTag("btn_confirm_delete_sub").performClick()
        composeTestRule.onNodeWithText(uniqueName).assertDoesNotExist()
    }

    private fun addMockSubscription(name: String, amount: String, date: String) {
        composeTestRule.onNodeWithTag("fab_add_sub").performClick()
        composeTestRule.onNodeWithTag("input_sub_name").performTextInput(name)
        composeTestRule.onNodeWithTag("input_sub_amount").performTextInput(amount)
        composeTestRule.onNodeWithTag("input_sub_date").performTextInput(date)
        composeTestRule.onNodeWithTag("btn_save_sub").performClick()
        
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("sub_card_$name").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
