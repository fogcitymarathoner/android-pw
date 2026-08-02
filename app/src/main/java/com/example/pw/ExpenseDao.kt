package com.example.pw

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE userId = :userId")
    fun getExpensesForUser(userId: String): Flow<List<Expense>>

    @Transaction
    @Query("""
        SELECT expenses.*, categories.name as categoryName 
        FROM expenses 
        LEFT JOIN categories ON expenses.categoryId = categories.id 
        WHERE expenses.userId = :userId
    """)
    fun getExpensesWithCategoryForUser(userId: String): Flow<List<ExpenseWithCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByExpenseRemoteId(remoteId: String): Expense?

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses WHERE localId = :localId")
    suspend fun deleteById(localId: Int)
}
