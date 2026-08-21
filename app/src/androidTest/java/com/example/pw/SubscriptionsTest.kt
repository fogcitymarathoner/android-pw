package com.example.pw

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SubscriptionsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase.setTestInstance(db)
    }

    @After
    fun tearDown() {
        AppDatabase.setTestInstance(null)
        db.close()
    }

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
        
        // Select 5th from dropdown
        composeTestRule.onNodeWithTag("input_sub_due_day").performClick()
        composeTestRule.onNodeWithText("5th").performClick()
        
        composeTestRule.onNodeWithTag("btn_save_sub").performClick()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText(uniqueName).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(uniqueName).assertIsDisplayed()
        composeTestRule.onNodeWithText("$9.99 • Due: 5th").assertIsDisplayed()
    }

    @Test
    fun testActiveInactiveState() {
        loginAndGoToSubs()
        val uniqueName = "ActiveSub-${UUID.randomUUID().toString().take(5)}"
        addMockSubscription(uniqueName, "$5.00", "15")

        // 1. Initially active, so check if top-level active filter switch is checked
        composeTestRule.onNodeWithTag("switch_filter_active").assertIsOn()
        composeTestRule.onNodeWithText(uniqueName).assertIsDisplayed()

        // 2. Switch to calendar view
        composeTestRule.onNodeWithContentDescription("Calendar View").performClick()

        // Verify the subscription is visible in the calendar grid (active)
        composeTestRule.onNodeWithText(uniqueName).assertIsDisplayed()

        // 3. Switch back to list view
        composeTestRule.onNodeWithContentDescription("List View").performClick()

        // Edit the subscription to make it inactive
        composeTestRule.onNode(
            hasTestTag("btn_edit_sub") and hasAnyAncestor(hasTestTag("sub_card_$uniqueName"))
        ).performClick()

        // Toggle the dialog active switch to off (inactive)
        composeTestRule.onNodeWithTag("switch_sub_dialog_active").performClick()
        composeTestRule.onNodeWithTag("btn_save_sub").performClick()

        // 4. Back on the list view: since top filter is still "Showing Active", the inactive sub should not be displayed
        composeTestRule.onNodeWithText(uniqueName).assertDoesNotExist()

        // 5. Toggle the top-level filter switch to show Inactive subscriptions
        composeTestRule.onNodeWithTag("switch_filter_active").performClick()
        composeTestRule.onNodeWithTag("switch_filter_active").assertIsOff()

        // The inactive sub should now be displayed
        composeTestRule.onNodeWithText(uniqueName).assertIsDisplayed()

        // 6. Switch to calendar view again
        composeTestRule.onNodeWithContentDescription("Calendar View").performClick()

        // Verify the subscription is visible in the calendar grid (as we are filtering for inactive ones)
        composeTestRule.onNodeWithText(uniqueName).assertIsDisplayed()
    }

    @Test
    fun testDeleteSubscription() {
        loginAndGoToSubs()
        val uniqueName = "DeleteSub-${UUID.randomUUID().toString().take(5)}"
        addMockSubscription(uniqueName, "$0", "10")

        composeTestRule.onNode(
            hasTestTag("btn_delete_sub") and hasAnyAncestor(hasTestTag("sub_card_$uniqueName"))
        ).performClick()

        composeTestRule.onNodeWithTag("btn_confirm_delete_sub").performClick()
        composeTestRule.onNodeWithText(uniqueName).assertDoesNotExist()
    }

    private fun addMockSubscription(name: String, amount: String, dueDay: String) {
        composeTestRule.onNodeWithTag("fab_add_sub").performClick()
        composeTestRule.onNodeWithTag("input_sub_name").performTextInput(name)
        composeTestRule.onNodeWithTag("input_sub_amount").performTextInput(amount)
        
        // Select from dropdown
        composeTestRule.onNodeWithTag("input_sub_due_day").performClick()
        composeTestRule.onNodeWithText("$dueDay${getOrdinal(dueDay)}").performClick()
        
        composeTestRule.onNodeWithTag("btn_save_sub").performClick()
        
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("sub_card_$name").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun getOrdinal(n: String): String {
        val i = n.toIntOrNull() ?: return ""
        return when {
            i in 11..13 -> "th"
            i % 10 == 1 -> "st"
            i % 10 == 2 -> "nd"
            i % 10 == 3 -> "rd"
            else -> "th"
        }
    }
}
