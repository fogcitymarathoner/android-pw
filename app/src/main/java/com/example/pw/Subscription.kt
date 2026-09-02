package com.example.pw

data class Subscription(
    val id: String? = null,
    val name: String = "",
    val account: String = "",
    val amount: String = "",
    val dueDate: String = "",   // e.g. "August 14th" or "14th"
    val period: String = "",    // "monthly" or "annual"
    val month: String = "",     // Month dropdown selection
    val calendarDate: String = "", // Full date from calendar grid
    val memo: String = "",
    val isActive: Boolean = true
)

fun getOrdinal(n: String): String {
    val i = n.toIntOrNull() ?: return ""
    return when {
        i in 11..13 -> "th"
        i % 10 == 1 -> "st"
        i % 10 == 2 -> "nd"
        i % 10 == 3 -> "rd"
        else -> "th"
    }
}

fun List<Subscription>.sortByDueDate(): List<Subscription> {
    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    
    return this.sortedWith(compareBy({ 
        when (it.period.lowercase().replace("_", " ")) {
            "annual" -> 2
            "every two months" -> 1
            else -> 0 // Monthly and others at the top
        }
    }, {
        val periodLower = it.period.lowercase().replace("_", " ")
        if (periodLower == "annual" || periodLower == "every two months") {
            val monthName = it.dueDate.split(" ").firstOrNull()
            months.indexOfFirst { m -> m.equals(monthName, ignoreCase = true) }.takeIf { idx -> idx != -1 } ?: 99
        } else {
            -1
        }
    }, {
        val digitRegex = Regex("(\\d+)")
        digitRegex.find(it.dueDate)?.value?.toInt() ?: 99
    }, { 
        it.name.lowercase() 
    }))
}
