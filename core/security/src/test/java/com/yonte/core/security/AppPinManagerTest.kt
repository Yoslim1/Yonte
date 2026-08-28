package com.yonte.core.security

import java.security.MessageDigest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppPinManagerTest {

    @Before
    fun setUp() {
        Argon2Kdf.installKdfEngineForTesting { password, salt ->
            MessageDigest.getInstance("SHA-256").apply {
                update(salt)
                update(password)
            }.digest()
        }
    }

    @After
    fun tearDown() {
        Argon2Kdf.installKdfEngineForTesting(null)
    }

    private fun createManager() = AppPinManager(RuntimeEnvironment.getApplication())

    @Test
    fun `isPinSet returns false initially`() {
        assertFalse(createManager().isPinSet())
    }

    @Test
    fun `setPin makes isPinSet return true`() {
        val manager = createManager()
        manager.setPin("1234".toCharArray())
        assertTrue(manager.isPinSet())
    }

    @Test
    fun `correct PIN verifies after setPin`() {
        val manager = createManager()
        manager.setPin("1234".toCharArray())
        assertTrue(manager.verify("1234".toCharArray()))
    }

    @Test
    fun `wrong PIN fails verification`() {
        val manager = createManager()
        manager.setPin("1234".toCharArray())
        assertFalse(manager.verify("5678".toCharArray()))
    }

    @Test
    fun `5 consecutive wrong attempts trigger lockout`() {
        val manager = createManager()
        manager.setPin("1234".toCharArray())
        repeat(5) { manager.verify("0000".toCharArray()) }
        assertTrue(manager.lockoutSecondsRemaining() > 0)
    }

    @Test
    fun `correct PIN after successful verify resets attempt counter`() {
        val manager = createManager()
        manager.setPin("1234".toCharArray())
        // Fail 4 times (just under lockout threshold)
        repeat(4) { manager.verify("0000".toCharArray()) }
        assertEquals(0L, manager.lockoutSecondsRemaining())
        // Correct PIN resets counter
        assertTrue(manager.verify("1234".toCharArray()))
        // 4 more failures still under threshold because counter was reset
        repeat(4) { manager.verify("0000".toCharArray()) }
        assertEquals(0L, manager.lockoutSecondsRemaining())
    }
}
