package com.example.pw

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val remoteId: String? = null, // Firebase ID
    val name: String,
    val userId: String
)
