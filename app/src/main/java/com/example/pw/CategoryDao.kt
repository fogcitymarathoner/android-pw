package com.example.pw

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId")
    fun getCategoriesForUser(userId: String): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Query("SELECT * FROM categories WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByCategoryRemoteId(remoteId: String): Category?

    @Delete
    suspend fun delete(category: Category)
}
