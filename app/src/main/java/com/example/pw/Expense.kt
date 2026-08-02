package com.example.pw

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Embedded

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val remoteId: String? = null, // Firebase ID
    val vendor: String = "",
    val categoryId: Int? = null,
    val amount: String = "",
    val date: String = "",
    val memo: String = "",
    val userId: String = ""
)

data class ExpenseWithCategory(
    @Embedded val expense: Expense,
    val categoryName: String?
)
