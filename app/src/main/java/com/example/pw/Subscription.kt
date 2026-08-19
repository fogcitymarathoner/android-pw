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
    val memo: String = ""
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
        if (it.period.lowercase() == "annual") {
            val monthName = it.dueDate.split(" ").firstOrNull()
            months.indexOf(monthName?.lowercase()?.replaceFirstChar { it.uppercase() }).takeIf { idx -> idx != -1 } ?: 99
        } else {
            -1 // Monthly items at the top
        }
    }, {
        val digitRegex = Regex("(\\d+)")
        digitRegex.find(it.dueDate)?.value?.toInt() ?: 99
    }, { 
        it.name.lowercase() 
    }))
}
