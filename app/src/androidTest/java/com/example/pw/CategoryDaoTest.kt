package com.example.pw

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        categoryDao = database.categoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getCategoriesForUser_ReturnsSortedByName() = runBlocking {
        val userId = "test_user"
        val categories = listOf(
            Category("3", "Zebra", userId),
            Category("1", "Apple", userId),
            Category("2", "Banana", userId),
            Category("4", "apple", userId)
        )

        categoryDao.insertAll(categories)

        val result = categoryDao.getCategoriesForUser(userId).first()

        assertEquals(4, result.size)
        // Case-insensitive sorting from SQL: apple, Apple, Banana, Zebra
        assertEquals("apple", result[0].name.lowercase())
        assertEquals("apple", result[1].name.lowercase())
        assertEquals("Banana", result[2].name)
        assertEquals("Zebra", result[3].name)
    }
}
