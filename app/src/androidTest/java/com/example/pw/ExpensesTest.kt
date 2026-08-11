package com.example.pw

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ExpensesTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.getDatabase(context)
        runBlocking {
            db.expenseDao().deleteAllForUser("debug_user_123")
            db.categoryDao().deleteAllForUser("debug_user_123")
            db.vendorDao().deleteAllForUser("debug_user_123")
        }
    }

    private fun loginAndGoToExpenses() {
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("debug_login").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("debug_login").performClick()
        
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("nav_expenses").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("nav_expenses").performClick()

        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("header_expenses").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testVendorCRUD() {
        loginAndGoToExpenses()

        val uniqueVendor = "Vendor-${UUID.randomUUID().toString().take(5)}"
        openManageVendors()
        
        composeTestRule.onNodeWithTag("input_manage_add").performTextInput(uniqueVendor)
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag("manage_row_$uniqueVendor").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("manage_row_$uniqueVendor").assertIsDisplayed()

        val updatedVendor = "$uniqueVendor-New"
        composeTestRule.onNode(
            hasTestTag("btn_manage_rename") and hasAnyAncestor(hasTestTag("manage_row_$uniqueVendor"))
        ).performClick()
        
        composeTestRule.onNodeWithTag("input_rename_item").performTextReplacement(updatedVendor)
        composeTestRule.onNodeWithText("Save").performClick()
        
        composeTestRule.onNodeWithTag("manage_row_$updatedVendor").assertIsDisplayed()

        composeTestRule.onNode(
            hasTestTag("btn_manage_delete") and hasAnyAncestor(hasTestTag("manage_row_$updatedVendor"))
        ).performClick()
        composeTestRule.onNodeWithTag("manage_row_$updatedVendor").assertDoesNotExist()
        
        composeTestRule.onNodeWithText("Close").performClick()
    }

    @Test
    fun testCategoryCRUD() {
        loginAndGoToExpenses()

        val uniqueCat = "Cat-${UUID.randomUUID().toString().take(5)}"
        openManageCategories()
        
        composeTestRule.onNodeWithTag("input_manage_add").performTextInput(uniqueCat)
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag("manage_row_$uniqueCat").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("manage_row_$uniqueCat").assertIsDisplayed()

        val updatedCat = "$uniqueCat-New"
        composeTestRule.onNode(
            hasTestTag("btn_manage_rename") and hasAnyAncestor(hasTestTag("manage_row_$uniqueCat"))
        ).performClick()
        
        composeTestRule.onNodeWithTag("input_rename_item").performTextReplacement(updatedCat)
        composeTestRule.onNodeWithText("Save").performClick()
        
        composeTestRule.onNodeWithTag("manage_row_$updatedCat").assertIsDisplayed()

        composeTestRule.onNode(
            hasTestTag("btn_manage_delete") and hasAnyAncestor(hasTestTag("manage_row_$updatedCat"))
        ).performClick()
        composeTestRule.onNodeWithTag("manage_row_$updatedCat").assertDoesNotExist()
        
        composeTestRule.onNodeWithText("Close").performClick()
    }

    @Test
    fun testExpenseCRUD() {
        loginAndGoToExpenses()

        val vendor = "CRUD-Vendor"
        val category = "CRUD-Cat"
        
        ensureVendorExists(vendor)
        ensureCategoryExists(category)

        composeTestRule.onNodeWithTag("fab_add_exp").performClick()
        composeTestRule.onNodeWithTag("btn_expense_vendor").performClick()
        composeTestRule.onNodeWithText(vendor).performClick()
        composeTestRule.onNodeWithTag("btn_expense_category").performClick()
        composeTestRule.onNodeWithText(category).performClick()
        composeTestRule.onNodeWithTag("input_expense_amount").performTextInput("100")
        composeTestRule.onNodeWithTag("btn_save_expense").performClick()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag("expense_card_$vendor").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("expense_card_$vendor").assertIsDisplayed()

        composeTestRule.onNode(hasTestTag("btn_delete_exp") and hasAnyAncestor(hasTestTag("expense_card_$vendor"))).performClick()
        composeTestRule.onNodeWithTag("btn_confirm_delete_exp").performClick()
        composeTestRule.onNodeWithTag("expense_card_$vendor").assertDoesNotExist()
    }

    private fun openManageVendors() {
        composeTestRule.onNodeWithTag("chip_vendor").performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Manage Vendors...").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Manage Vendors...").performClick()
    }

    private fun openManageCategories() {
        composeTestRule.onNodeWithTag("chip_category").performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Manage Categories...").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Manage Categories...").performClick()
    }

    private fun ensureVendorExists(name: String) {
        openManageVendors()
        val nodes = composeTestRule.onAllNodesWithText(name).fetchSemanticsNodes()
        if (nodes.isEmpty()) {
            composeTestRule.onNodeWithTag("input_manage_add").performTextInput(name)
            composeTestRule.onNodeWithContentDescription("Add").performClick()
        }
        composeTestRule.onNodeWithText("Close").performClick()
    }

    private fun ensureCategoryExists(name: String) {
        openManageCategories()
        val nodes = composeTestRule.onAllNodesWithText(name).fetchSemanticsNodes()
        if (nodes.isEmpty()) {
            composeTestRule.onNodeWithTag("input_manage_add").performTextInput(name)
            composeTestRule.onNodeWithContentDescription("Add").performClick()
        }
        composeTestRule.onNodeWithText("Close").performClick()
    }
}
