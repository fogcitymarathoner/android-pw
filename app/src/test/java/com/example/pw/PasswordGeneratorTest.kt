package com.example.pw

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {

    @Test
    fun testGenerateStrongPassword_DefaultLengthIs12() {
        val pw = generateStrongPassword()
        assertEquals(12, pw.length)
    }

    @Test
    fun testGenerateStrongPassword_SupportedLengths() {
        val lengths = listOf(12, 13, 14)
        for (len in lengths) {
            val pw = generateStrongPassword(len)
            assertEquals(len, pw.length)
        }
    }

    @Test
    fun testGenerateStrongPassword_CustomLengths() {
        val pw8 = generateStrongPassword(8)
        assertEquals(8, pw8.length)

        val pw20 = generateStrongPassword(20)
        assertEquals(20, pw20.length)
    }

    @Test
    fun testGenerateStrongPassword_CharacterPoolInclusion() {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+".toSet()
        val pw = generateStrongPassword(100)
        
        // Every character must belong to allowed pool
        for (char in pw) {
            assertTrue("Character '$char' is in allowed pool", allowedChars.contains(char))
        }
    }

    @Test
    fun testGenerateStrongPassword_Randomness() {
        val pw1 = generateStrongPassword(12)
        val pw2 = generateStrongPassword(12)
        val pw3 = generateStrongPassword(14)
        
        // Consecutive calls should produce distinct passwords
        assertNotEquals(pw1, pw2)
        assertNotEquals(pw1, pw3)
        assertNotEquals(pw2, pw3)
    }
}
