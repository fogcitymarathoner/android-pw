package com.example.pw

import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionDateTest {
    @Test
    fun testSubscription_MonthAndCalendarFields() {
        val selectedMonth = "August"
        val selectedCalendarDate = "2026-08-14"
        val sub = Subscription(
            id = "1",
            name = "Amazon Prime",
            dueDate = "August 14th",
            period = "annual",
            month = selectedMonth,
            calendarDate = selectedCalendarDate,
            memo = "annually 8/13"
        )

        assertEquals(selectedMonth, sub.month)
        assertEquals(selectedCalendarDate, sub.calendarDate)
    }

    @Test
    fun testSubscription_EmptyDefaults() {
        val sub = Subscription(
            id = "1",
            name = "Netflix"
        )

        assertEquals("", sub.month)
        assertEquals("", sub.calendarDate)
    }

    @Test
    fun testSubscription_ActiveState() {
        val subDefault = Subscription(id = "1", name = "Netflix")
        assertEquals(true, subDefault.isActive)

        val subInactive = Subscription(id = "2", name = "Hulu", isActive = false)
        assertEquals(false, subInactive.isActive)

        val updated = subInactive.copy(isActive = true)
        assertEquals(true, updated.isActive)
    }

    @Test
    fun testSubscription_CopyPreservesFields() {
        val sub = Subscription(
            id = "1",
            name = "Netflix",
            month = "October",
            calendarDate = "2023-10-10"
        )
        
        val updated = sub.copy(name = "Netflix Premium")
        
        assertEquals("Netflix Premium", updated.name)
        assertEquals("October", updated.month)
        assertEquals("2023-10-10", updated.calendarDate)
    }

    @Test
    fun testGetOrdinal() {
        assertEquals("st", getOrdinal("1"))
        assertEquals("nd", getOrdinal("2"))
        assertEquals("rd", getOrdinal("3"))
        assertEquals("th", getOrdinal("4"))
        assertEquals("th", getOrdinal("11"))
        assertEquals("th", getOrdinal("12"))
        assertEquals("th", getOrdinal("13"))
        assertEquals("st", getOrdinal("21"))
        assertEquals("nd", getOrdinal("22"))
        assertEquals("rd", getOrdinal("23"))
        assertEquals("th", getOrdinal("24"))
        assertEquals("th", getOrdinal("30"))
        assertEquals("st", getOrdinal("31"))
        assertEquals("", getOrdinal("abc"))
    }
}
