package com.example.pw

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordMemoTest {
    @Test
    fun testPwEntity_MemoField() {
        val memoText = "Secret question: What is your favorite color? Answer: Blue"
        val entity = PwEntity(
            id = "1",
            vendor = "Example Bank",
            account = "user123",
            pw = "password123",
            memo = memoText
        )

        assertEquals(memoText, entity.memo)
    }

    @Test
    fun testPwEntity_EmptyMemoByDefault() {
        val entity = PwEntity(
            id = "1",
            vendor = "Example Bank",
            account = "user123",
            pw = "password123"
        )

        assertEquals("", entity.memo)
    }

    @Test
    fun testPwEntity_CopyPreservesMemo() {
        val entity = PwEntity(
            id = "1",
            vendor = "Example Bank",
            memo = "Original memo"
        )
        
        val updated = entity.copy(vendor = "New Bank")
        
        assertEquals("New Bank", updated.vendor)
        assertEquals("Original memo", updated.memo)
    }
}
