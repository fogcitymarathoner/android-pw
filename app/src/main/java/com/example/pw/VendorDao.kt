package com.example.pw

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VendorDao {
    @Query("SELECT * FROM vendors WHERE userId = :userId")
    fun getVendorsForUser(userId: String): Flow<List<Vendor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vendor: Vendor)

    @Query("SELECT * FROM vendors WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByVendorRemoteId(remoteId: String): Vendor?

    @Delete
    suspend fun delete(vendor: Vendor)
}
