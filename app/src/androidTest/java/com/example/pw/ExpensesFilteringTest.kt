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

@RunWith(AndroidJUnit4::class)
class ExpensesFilteringTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var db: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var vendorDao: VendorDao

    private val catAId = "cat_a_id"
    private val catBId = "cat_b_id"
    private val ven1Id = "ven_1_id"
    private val ven2Id = "ven_2_id"
    private val userId = "debug_user_123"

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = AppDatabase.getDatabase(context)
        expenseDao = db.expenseDao()
        categoryDao = db.categoryDao()
        vendorDao = db.vendorDao()

        runBlocking {
            expenseDao.deleteAllForUser(userId)
            categoryDao.deleteAllForUser(userId)
            vendorDao.deleteAllForUser(userId)

            // Seed Data
            categoryDao.insertAll(listOf(
                Category(remoteId = catAId, name = "Category A", userId = userId),
                Category(remoteId = catBId, name = "Category B", userId = userId)
            ))
            vendorDao.insertAll(listOf(
                Vendor(remoteId = ven1Id, name = "Vendor 1", userId = userId),
                Vendor(remoteId = ven2Id, name = "Vendor 2", userId = userId)
            ))
            expenseDao.insertAll(listOf(
                Expense(remoteId = "e1", categoryId = catAId, vendorId = ven1Id, amount = "10.00", date = "2023-01-01", memo = "Exp 1", userId = userId),
                Expense(remoteId = "e2", categoryId = catAId, vendorId = ven2Id, amount = "20.00", date = "2023-01-02", memo = "Exp 2", userId = userId),
                Expense(remoteId = "e3", categoryId = catBId, vendorId = ven1Id, amount = "30.00", date = "2023-01-03", memo = "Exp 3", userId = userId)
            ))
        }
    }

    private fun loginAsTestUser() {
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("debug_login").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("debug_login").performClick()
        
        composeTestRule.onNodeWithTag("nav_expenses").performClick()
        
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("expense_card_Vendor 1").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun testFilter_AllCategory_AllVendor() {
        loginAsTestUser()
        composeTestRule.onAllNodesWithTag("expense_card_Vendor 1").assertCountEquals(2)
        composeTestRule.onAllNodesWithTag("expense_card_Vendor 2").assertCountEquals(1)
    }

    @Test
    fun testFilter_SelectedCategory_AllVendor() {
        loginAsTestUser()
        
        composeTestRule.onNodeWithTag("chip_category").performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("filter_cat_Category A").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("filter_cat_Category A").performClick()
        
        composeTestRule.onAllNodesWithTag("expense_card_Vendor 1").assertCountEquals(1)
        composeTestRule.onAllNodesWithTag("expense_card_Vendor 2").assertCountEquals(1)
    }

    @Test
    fun testFilter_SelectedCategory_SelectedVendor() {
        loginAsTestUser()
        
        composeTestRule.onNodeWithTag("chip_category").performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("filter_cat_Category A").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("filter_cat_Category A").performClick()
        
        composeTestRule.onNodeWithTag("chip_vendor").performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("filter_ven_Vendor 1").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("filter_ven_Vendor 1").performClick()
        
        composeTestRule.onAllNodesWithTag("expense_card_Vendor 1").assertCountEquals(1)
    }

    @Test
    fun testFilter_AllCategory_SelectedVendor() {
        loginAsTestUser()
        
        composeTestRule.onNodeWithTag("chip_vendor").performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("filter_ven_Vendor 1").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("filter_ven_Vendor 1").performClick()
        
        composeTestRule.onAllNodesWithTag("expense_card_Vendor 1").assertCountEquals(2)
        composeTestRule.onAllNodesWithTag("expense_card_Vendor 2").assertCountEquals(0)
    }
}
