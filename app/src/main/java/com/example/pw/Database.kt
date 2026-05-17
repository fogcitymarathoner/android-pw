package com.example.pw

import androidx.room.*

@androidx.room.Entity(tableName = "pw")
data class PwEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vendor: String,
    val pw: String
)

@androidx.room.Dao
interface PwDao {
    @androidx.room.Query("SELECT * FROM pw")
    suspend fun getAll(): List<PwEntity>

    @androidx.room.Insert
    suspend fun insert(pwEntry: PwEntity)
}

@androidx.room.Database(entities = [PwEntity::class], version = 1)
abstract class AppDatabase : androidx.room.RoomDatabase() {
    abstract fun pwDao(): PwDao
}