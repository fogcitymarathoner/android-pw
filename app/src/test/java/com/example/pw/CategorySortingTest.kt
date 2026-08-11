package com.example.pw

import org.junit.Assert.assertEquals
import org.junit.Test

class CategorySortingTest {
    @Test
    fun testCategorySorting_Alphabetical() {
        val categories = listOf(
            Category("3", "Zebra", "user1"),
            Category("1", "Apple", "user1"),
            Category("2", "Banana", "user1")
        )

        val sorted = categories.sortByName()

        assertEquals("Apple", sorted[0].name)
        assertEquals("Banana", sorted[1].name)
        assertEquals("Zebra", sorted[2].name)
    }

    @Test
    fun testCategorySorting_CaseInsensitive() {
        val categories = listOf(
            Category("1", "zebra", "user1"),
            Category("2", "Apple", "user1")
        )

        val sorted = categories.sortByName()

        assertEquals("Apple", sorted[0].name)
        assertEquals("zebra", sorted[1].name)
    }

    @Test
    fun testCategorySorting_Empty() {
        val categories = emptyList<Category>()
        val sorted = categories.sortByName()
        assertEquals(0, sorted.size)
    }
}
