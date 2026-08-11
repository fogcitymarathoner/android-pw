package com.example.pw

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PasswordsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        AppDatabase.setTestInstance(null)
    }

    private fun loginAsMockUser() {
        // Use the debug login button to get to the passwords screen
        // This uses the mock UID "debug_user_123" which is safe for testing
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag("debug_login").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("debug_login").performClick()
        
        // Wait for the passwords screen to load
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag("fab_add").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testAddButtonOpensForm() {
        loginAsMockUser()

        // Press the plus button
        composeTestRule.onNodeWithTag("fab_add").performClick()

        // Verify the dialog is displayed
        composeTestRule.onNodeWithText("Add Password").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_vendor").assertIsDisplayed()
    }

    @Test
    fun testCancelFormReturnsToList() {
        loginAsMockUser()

        // Open the form
        composeTestRule.onNodeWithTag("fab_add").performClick()
        composeTestRule.onNodeWithText("Add Password").assertIsDisplayed()

        // Press cancel
        composeTestRule.onNodeWithTag("dialog_cancel").performClick()

        // Verify dialog is gone
        composeTestRule.onNodeWithText("Add Password").assertDoesNotExist()
        // FAB should be visible again
        composeTestRule.onNodeWithTag("fab_add").assertIsDisplayed()
    }

    @Test
    fun testSubmitAddsPassword() {
        loginAsMockUser()

        val uniqueVendor = "TestVendor-${UUID.randomUUID().toString().take(5)}"
        
        // Open the form
        composeTestRule.onNodeWithTag("fab_add").performClick()

        // Enter data
        composeTestRule.onNodeWithTag("input_vendor").performTextInput(uniqueVendor)
        composeTestRule.onNodeWithTag("input_account").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("input_password").performTextInput("secret123")

        // Submit
        composeTestRule.onNodeWithTag("dialog_save").performClick()

        // Verify dialog closes
        composeTestRule.onNodeWithText("Add Password").assertDoesNotExist()

        // Verify the new entry appears in the list
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText(uniqueVendor).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(uniqueVendor).assertIsDisplayed()
    }

    @Test
    fun testCopyFromList() {
        loginAsMockUser()

        val uniqueVendor = "CopyTest-${UUID.randomUUID().toString().take(5)}"
        addMockPassword(uniqueVendor, "user", "pass123")

        // Find the card for this vendor and click the copy button
        // We use hasAnyAncestor to target the button inside the specific card
        composeTestRule.onNode(
            hasTestTag("btn_copy") and hasAnyAncestor(hasTestTag("password_card_$uniqueVendor"))
        ).performClick()

        // We can't easily check the system clipboard in a standard Compose test without extra permissions/logic
        // but we've triggered the action. Verification of the Toast is also tricky.
        // For now, we verify the button was clickable and the app didn't crash.
    }

    @Test
    fun testViewingPassword() {
        loginAsMockUser()

        val uniqueVendor = "ViewTest-${UUID.randomUUID().toString().take(5)}"
        val secretPass = "Secret-999"
        addMockPassword(uniqueVendor, "admin", secretPass)

        // Click the view button
        composeTestRule.onNode(
            hasTestTag("btn_view") and hasAnyAncestor(hasTestTag("password_card_$uniqueVendor"))
        ).performClick()

        // Verify viewing dialog is open
        composeTestRule.onNodeWithTag("view_title").assertTextEquals(uniqueVendor)
        composeTestRule.onNodeWithTag("view_password").assertTextEquals(secretPass)

        // Test copy from viewing form
        composeTestRule.onNodeWithTag("btn_view_copy").performClick()

        // Test close button
        composeTestRule.onNodeWithTag("btn_view_close").performClick()
        composeTestRule.onNodeWithTag("view_title").assertDoesNotExist()
    }

    @Test
    fun testDeletePassword() {
        loginAsMockUser()

        val uniqueVendor = "DeleteTest-${UUID.randomUUID().toString().take(5)}"
        addMockPassword(uniqueVendor, "trash", "dump")

        // Click delete button in list
        composeTestRule.onNode(
            hasTestTag("btn_delete") and hasAnyAncestor(hasTestTag("password_card_$uniqueVendor"))
        ).performClick()

        // Confirm delete in dialog
        composeTestRule.onNodeWithTag("btn_confirm_delete").performClick()

        // Verify it's gone from list
        composeTestRule.onNodeWithText(uniqueVendor).assertDoesNotExist()
    }

    @Test
    fun testEditPassword() {
        loginAsMockUser()

        val uniqueVendor = "EditTest-${UUID.randomUUID().toString().take(5)}"
        addMockPassword(uniqueVendor, "old", "old")

        // Click edit button
        composeTestRule.onNode(
            hasTestTag("btn_edit") and hasAnyAncestor(hasTestTag("password_card_$uniqueVendor"))
        ).performClick()

        // Change data
        val newVendor = "${uniqueVendor}-Updated"
        composeTestRule.onNodeWithTag("input_vendor").performTextReplacement(newVendor)
        composeTestRule.onNodeWithTag("dialog_save").performClick()

        // Verify updated
        composeTestRule.onNodeWithText(newVendor).assertIsDisplayed()
        composeTestRule.onNodeWithText(uniqueVendor).assertDoesNotExist()
    }

    @Test
    fun testMemoFieldPersistence() {
        loginAsMockUser()

        val uniqueVendor = "MemoTest-${UUID.randomUUID().toString().take(5)}"
        val secretMemo = "Secret answer: 42"
        
        // Add password with memo
        composeTestRule.onNodeWithTag("fab_add").performClick()
        composeTestRule.onNodeWithTag("input_vendor").performTextInput(uniqueVendor)
        composeTestRule.onNodeWithTag("input_password").performTextInput("pass")
        composeTestRule.onNodeWithTag("input_memo").performTextInput(secretMemo)
        composeTestRule.onNodeWithTag("dialog_save").performClick()

        // Verify the "Has Memo" icon appears in the card
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithContentDescription("Has Memo").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Has Memo").assertIsDisplayed()

        // Open view dialog and verify memo content
        composeTestRule.onNode(
            hasTestTag("btn_view") and hasAnyAncestor(hasTestTag("password_card_$uniqueVendor"))
        ).performClick()

        composeTestRule.onNodeWithTag("view_memo").assertTextEquals(secretMemo)
        composeTestRule.onNodeWithTag("btn_view_close").performClick()
    }

    private fun addMockPassword(vendor: String, account: String, pass: String) {
        composeTestRule.onNodeWithTag("fab_add").performClick()
        composeTestRule.onNodeWithTag("input_vendor").performTextInput(vendor)
        composeTestRule.onNodeWithTag("input_account").performTextInput(account)
        composeTestRule.onNodeWithTag("input_password").performTextInput(pass)
        composeTestRule.onNodeWithTag("dialog_save").performClick()
        composeTestRule.onNodeWithText("Add Password").assertDoesNotExist()
    }
}
