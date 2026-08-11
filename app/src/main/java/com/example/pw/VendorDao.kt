package com.example.pw

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VendorDao {
    @Query("SELECT * FROM vendors WHERE userId = :userId ORDER BY name COLLATE NOCASE ASC")
    fun getVendorsForUser(userId: String): Flow<List<Vendor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vendor: Vendor)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vendors: List<Vendor>)

    @Update
    suspend fun update(vendor: Vendor)

    @Query("SELECT * FROM vendors WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByVendorRemoteId(remoteId: String): Vendor?

    @Delete
    suspend fun delete(vendor: Vendor)

    @Query("DELETE FROM vendors WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
