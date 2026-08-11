package com.example.pw

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE userId = :userId")
    fun getExpensesForUser(userId: String): Flow<List<Expense>>

    @Transaction
    @Query("""
        SELECT expenses.*, categories.name as categoryName, vendors.name as vendorName
        FROM expenses 
        LEFT JOIN categories ON expenses.categoryId = categories.remoteId 
        LEFT JOIN vendors ON expenses.vendorId = vendors.remoteId
        WHERE expenses.userId = :userId
    """)
    fun getExpensesWithDetailsForUser(userId: String): Flow<List<ExpenseWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>)

    @Query("SELECT * FROM expenses WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByExpenseRemoteId(remoteId: String): Expense?

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT COUNT(*) FROM expenses WHERE vendorId = :vendorId")
    suspend fun getExpenseCountForVendor(vendorId: String): Int

    @Query("SELECT COUNT(*) FROM expenses WHERE categoryId = :categoryId")
    suspend fun getExpenseCountForCategory(categoryId: String): Int

    @Query("DELETE FROM expenses WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
