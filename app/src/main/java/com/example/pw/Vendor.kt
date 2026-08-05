package com.example.pw

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vendors")
data class Vendor(
    @PrimaryKey val remoteId: String, // Firebase Key
    val name: String,
    val userId: String
)
