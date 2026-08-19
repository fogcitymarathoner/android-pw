package com.example.pw

import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionSortingTest {
    @Test
    fun testSubscriptionSorting_MonthlyFirst() {
        val subs = listOf(
            Subscription(name = "Z-Annual", dueDate = "January 1st", period = "annual"),
            Subscription(name = "A-Monthly", dueDate = "15th", period = "monthly")
        )

        val sorted = subs.sortByDueDate()

        // Monthly items are at the top (period -1)
        assertEquals("A-Monthly", sorted[0].name)
        assertEquals("Z-Annual", sorted[1].name)
    }

    @Test
    fun testSubscriptionSorting_AnnualByMonth() {
        val subs = listOf(
            Subscription(name = "August Sub", dueDate = "August 10th", period = "annual"),
            Subscription(name = "March Sub", dueDate = "March 15th", period = "annual"),
            Subscription(name = "January Sub", dueDate = "January 1st", period = "annual")
        )

        val sorted = subs.sortByDueDate()

        assertEquals("January Sub", sorted[0].name)
        assertEquals("March Sub", sorted[1].name)
        assertEquals("August Sub", sorted[2].name)
    }

    @Test
    fun testSubscriptionSorting_SameMonthByDay() {
        val subs = listOf(
            Subscription(name = "Late Jan", dueDate = "January 20th", period = "annual"),
            Subscription(name = "Early Jan", dueDate = "January 5th", period = "annual")
        )

        val sorted = subs.sortByDueDate()

        assertEquals("Early Jan", sorted[0].name)
        assertEquals("Late Jan", sorted[1].name)
    }

    @Test
    fun testSubscriptionSorting_MonthlyByDay() {
        val subs = listOf(
            Subscription(name = "10th Sub", dueDate = "10th", period = "monthly"),
            Subscription(name = "2nd Sub", dueDate = "2nd", period = "monthly")
        )

        val sorted = subs.sortByDueDate()

        assertEquals("2nd Sub", sorted[0].name)
        assertEquals("10th Sub", sorted[1].name)
    }

    @Test
    fun testSubscriptionSorting_CaseInsensitivePeriod() {
        val subs = listOf(
            Subscription(name = "Annual", dueDate = "January 1st", period = "ANNUAL"),
            Subscription(name = "Monthly", dueDate = "15th", period = "MONTHLY")
        )

        val sorted = subs.sortByDueDate()

        assertEquals("Monthly", sorted[0].name)
        assertEquals("Annual", sorted[1].name)
    }

    @Test
    fun testSubscriptionSorting_SecondaryNameSort() {
        val subs = listOf(
            Subscription(name = "Beta", dueDate = "10th", period = "monthly"),
            Subscription(name = "Alpha", dueDate = "10th", period = "monthly")
        )

        val sorted = subs.sortByDueDate()

        assertEquals("Alpha", sorted[0].name)
        assertEquals("Beta", sorted[1].name)
    }

    @Test
    fun testSubscriptionSorting_InvalidDueDate() {
        val subs = listOf(
            Subscription(name = "Invalid", dueDate = "No Date Here", period = "monthly"),
            Subscription(name = "Valid", dueDate = "1st", period = "monthly")
        )

        val sorted = subs.sortByDueDate()

        // 99 for invalid day extraction vs 1 for "1st"
        assertEquals("Valid", sorted[0].name)
        assertEquals("Invalid", sorted[1].name)
    }

    @Test
    fun testSubscriptionSorting_AnnualMonthCaseInsensitive() {
        val subs = listOf(
            Subscription(name = "Feb Sub", dueDate = "February 1st", period = "annual"),
            Subscription(name = "jan sub", dueDate = "january 1st", period = "annual")
        )

        val sorted = subs.sortByDueDate()

        // This might fail if the implementation is not case-insensitive for month names
        assertEquals("jan sub", sorted[0].name)
        assertEquals("Feb Sub", sorted[1].name)
    }
}
