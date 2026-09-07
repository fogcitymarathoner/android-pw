package com.example.pw

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GetSubscriptionsForDateTest {

    private fun createCalendar(year: Int, monthIndex: Int, dayOfMonth: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
    }

    @Test
    fun testGetSubscriptionsForDate_MonthlyMatchesEveryMonth() {
        val monthlySub = Subscription(
            id = "1",
            name = "Spotify",
            dueDate = "15th",
            period = "monthly"
        )
        val subs = listOf(monthlySub)

        val jan15 = createCalendar(2026, Calendar.JANUARY, 15)
        val feb15 = createCalendar(2026, Calendar.FEBRUARY, 15)
        val mar10 = createCalendar(2026, Calendar.MARCH, 10)

        assertEquals(1, getSubscriptionsForDate(subs, jan15).size)
        assertEquals(1, getSubscriptionsForDate(subs, feb15).size)
        assertEquals(0, getSubscriptionsForDate(subs, mar10).size)
    }

    @Test
    fun testGetSubscriptionsForDate_AnnualMatchesOnlySpecificMonth() {
        val annualSub = Subscription(
            id = "2",
            name = "Amazon Prime",
            dueDate = "March 10th",
            period = "annual"
        )
        val subs = listOf(annualSub)

        val mar10 = createCalendar(2026, Calendar.MARCH, 10)
        val apr10 = createCalendar(2026, Calendar.APRIL, 10)

        val marResults = getSubscriptionsForDate(subs, mar10)
        assertEquals(1, marResults.size)
        assertEquals("Amazon Prime", marResults[0].name)

        val aprResults = getSubscriptionsForDate(subs, apr10)
        assertEquals(0, aprResults.size)
    }

    @Test
    fun testGetSubscriptionsForDate_EveryTwoMonthsRecurrence() {
        val bimonthlySub = Subscription(
            id = "3",
            name = "Recology",
            dueDate = "November 1st",
            period = "every two months",
            month = "November"
        )
        val subs = listOf(bimonthlySub)

        // November (start month, monthDiff = 0 % 2 == 0) -> Should match
        val nov1 = createCalendar(2026, Calendar.NOVEMBER, 1)
        assertEquals(1, getSubscriptionsForDate(subs, nov1).size)

        // December (monthDiff = 1 % 2 != 0) -> Should NOT match
        val dec1 = createCalendar(2026, Calendar.DECEMBER, 1)
        assertEquals(0, getSubscriptionsForDate(subs, dec1).size)

        // January (monthDiff = 2 % 2 == 0) -> Should match
        val jan1 = createCalendar(2027, Calendar.JANUARY, 1)
        assertEquals(1, getSubscriptionsForDate(subs, jan1).size)
    }

    @Test
    fun testGetSubscriptionsForDate_EmptyList() {
        val cal = createCalendar(2026, Calendar.JUNE, 1)
        val results = getSubscriptionsForDate(emptyList(), cal)
        assertTrue(results.isEmpty())
    }

    @Test
    fun testGetSubscriptionsForDate_CaseInsensitivePeriod() {
        val subUpper = Subscription(
            id = "4",
            name = "Cloud Storage",
            dueDate = "1st",
            period = "EVERY_TWO_MONTHS",
            month = "January"
        )
        val subs = listOf(subUpper)

        val jan1 = createCalendar(2026, Calendar.JANUARY, 1)
        assertEquals(1, getSubscriptionsForDate(subs, jan1).size)
    }
}
