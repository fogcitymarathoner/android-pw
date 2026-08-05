package com.example.pw

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val remoteId: String, // Firebase Key
    val vendorId: String? = null, // Linked to Vendor.remoteId
    val categoryId: String? = null, // Linked to Category.remoteId
    val amount: String = "",
    val date: String = "",
    val memo: String = "",
    val userId: String = ""
)

data class ExpenseWithDetails(
    @Embedded val expense: Expense,
    val categoryName: String?,
    val vendorName: String?
)
