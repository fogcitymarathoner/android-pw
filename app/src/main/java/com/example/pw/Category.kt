package com.example.pw

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val remoteId: String, // Firebase Key is the Primary Key
    val name: String,
    val userId: String
)

fun List<Category>.sortByName(): List<Category> {
    return this.sortedBy { it.name.lowercase() }
}
