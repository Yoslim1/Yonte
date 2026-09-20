package com.yonte.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricEnrollmentOperationTest {

    @Test
    fun `keyRemainsIntactUntilTerminalCompletion`() {
        val key = byteArrayOf(1, 2, 3, 4)
        val original = key.copyOf()
        var resultDelivered = false

        BiometricEnrollmentOperation(key) { resultDelivered = true }
            .succeed { providedKey ->
                assertSame(key, providedKey)
                assertArrayEquals(original, providedKey)
                assertFalse(resultDelivered)
            }

        assertTrue(resultDelivered)
        assertArrayEquals(ByteArray(key.size), key)
    }

    @Test
    fun `successPersistsOriginalKeyThenZeroesBuffer`() {
        val key = byteArrayOf(9, 8, 7)
        val original = key.copyOf()
        var persistedKey: ByteArray? = null
        val results = mutableListOf<Boolean>()

        BiometricEnrollmentOperation(key, results::add).succeed { providedKey ->
            persistedKey = providedKey.copyOf()
        }

        assertArrayEquals(original, persistedKey)
        assertArrayEquals(ByteArray(key.size), key)
        assertEquals(listOf(true), results)
    }

    @Test
    fun `persistenceFailureZeroesBufferAndReportsFailure`() {
        val key = byteArrayOf(5, 4, 3)
        val results = mutableListOf<Boolean>()

        BiometricEnrollmentOperation(key, results::add).succeed {
            throw IllegalStateException("persistence failed")
        }

        assertArrayEquals(ByteArray(key.size), key)
        assertEquals(listOf(false), results)
    }

    @Test
    fun `errorOrCancelZeroesBufferAndReportsFailure`() {
        val key = byteArrayOf(6, 7, 8)
        val results = mutableListOf<Boolean>()
        val operation = BiometricEnrollmentOperation(key, results::add)

        operation.fail()
        operation.fail()

        assertArrayEquals(ByteArray(key.size), key)
        assertEquals(listOf(false), results)
    }

    @Test
    fun `secondTerminalCompletionIsIgnored`() {
        val key = byteArrayOf(3, 2, 1)
        val results = mutableListOf<Boolean>()
        var persistenceCalls = 0
        val operation = BiometricEnrollmentOperation(key, results::add)

        operation.fail()
        operation.succeed {
            persistenceCalls++
        }

        assertEquals(0, persistenceCalls)
        assertEquals(listOf(false), results)
        assertArrayEquals(ByteArray(key.size), key)
    }

    @Test
    fun `resultIsDeliveredAtMostOnce`() {
        val key = byteArrayOf(4, 5, 6)
        val results = mutableListOf<Boolean>()
        var persistenceCalls = 0
        val operation = BiometricEnrollmentOperation(key, results::add)

        operation.succeed {
            persistenceCalls++
        }
        operation.fail()
        operation.succeed {
            persistenceCalls++
        }

        assertEquals(1, persistenceCalls)
        assertEquals(listOf(true), results)
        assertArrayEquals(ByteArray(key.size), key)
    }
}
