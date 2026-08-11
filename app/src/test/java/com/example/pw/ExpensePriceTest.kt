package com.example.pw

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpensePriceTest {
    @Test
    fun testFormatPrice_WholeNumber() {
        assertEquals("10.00", formatPrice("10"))
    }

    @Test
    fun testFormatPrice_OneDecimal() {
        assertEquals("10.50", formatPrice("10.5"))
    }

    @Test
    fun testFormatPrice_TwoDecimals() {
        assertEquals("10.55", formatPrice("10.55"))
    }

    @Test
    fun testFormatPrice_ManyDecimals() {
        assertEquals("10.56", formatPrice("10.5555"))
    }

    @Test
    fun testFormatPrice_Empty() {
        assertEquals("0.00", formatPrice(""))
    }

    @Test
    fun testFormatPrice_Invalid() {
        assertEquals("0.00", formatPrice("abc"))
    }
}
