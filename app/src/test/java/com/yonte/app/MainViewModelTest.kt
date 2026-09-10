package com.yonte.app

import android.content.Context
import android.content.SharedPreferences
import com.yonte.core.database.isDatabaseVersionMismatch
import com.yonte.core.security.AppPinManager
import com.yonte.core.security.BiometricUnlockManager
import com.yonte.core.security.LocalKeyManager
import com.yonte.core.backup.ScheduledBackupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class MainViewModelTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockLocalKeyManager: LocalKeyManager
    private lateinit var mockAppPinManager: AppPinManager
    private lateinit var mockBiometricUnlockManager: BiometricUnlockManager

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockLocalKeyManager = mock(LocalKeyManager::class.java)
        mockAppPinManager = mock(AppPinManager::class.java)
        mockBiometricUnlockManager = mock(BiometricUnlockManager::class.java)
        Dispatchers.setMain(UnconfinedTestDispatcher())

        `when`(
            mockContext.getSharedPreferences(ScheduledBackupWorker.PREFS_NAME, Context.MODE_PRIVATE),
        ).thenReturn(mockPrefs)
        `when`(mockPrefs.getString(ScheduledBackupWorker.KEY_DESTINATION_URI, null)).thenReturn(null)
        `when`(mockLocalKeyManager.isFirstRun()).thenReturn(false)
        `when`(mockLocalKeyManager.unlockMethod()).thenReturn(LocalKeyManager.METHOD_PASSPHRASE)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `PIN verify success restores session key and clears error state`() = runTest {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')
        val fakeKey = byteArrayOf(1, 2, 3, 4)

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(charArrayOf('1', '2', '3', '4'))).thenReturn(true)
        `when`(mockLocalKeyManager.cachedPinUnlockKey()).thenReturn(fakeKey)

        viewModel.submitPin(pin, isArabic = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.unlockErrorMessage)
        assertNull(state.unlockScreen)
        assertTrue(state.unlocked)
    }

    @Test
    fun `multiple PIN attempts leave the latest result authoritative`() = runTest {
        val viewModel = createViewModel()
        val firstPin = charArrayOf('1', '1', '1', '1')
        val secondPin = charArrayOf('2', '2', '2', '2')

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(charArrayOf('1', '1', '1', '1'))).thenReturn(false)
        `when`(mockAppPinManager.verify(charArrayOf('2', '2', '2', '2'))).thenReturn(true)
        `when`(mockLocalKeyManager.cachedPinUnlockKey()).thenReturn(byteArrayOf(1, 2, 3, 4))

        viewModel.submitPin(firstPin, isArabic = false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.unlocked)

        viewModel.submitPin(secondPin, isArabic = false)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.unlocked)
        assertFalse(viewModel.uiState.value.isWarmingDatabase)
    }

    @Test
    fun `PIN verify failure with lockout produces expected remaining seconds`() = runTest {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(charArrayOf('1', '2', '3', '4'))).thenReturn(false)
        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(25L)

        viewModel.submitPin(pin, isArabic = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Wait 25 seconds", state.unlockErrorMessage)
        assertFalse(state.unlocked)
    }

    @Test
    fun `PIN verify failure without lockout shows wrong PIN`() = runTest {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(charArrayOf('1', '2', '3', '4'))).thenReturn(false)
        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)

        viewModel.submitPin(pin, isArabic = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Wrong PIN", state.unlockErrorMessage)
    }

    @Test
    fun `PIN verify failure with lockout shows Arabic message`() = runTest {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(charArrayOf('1', '2', '3', '4'))).thenReturn(false)
        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(15L)

        viewModel.submitPin(pin, isArabic = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("انتظر 15 ثانية", state.unlockErrorMessage)
    }

    @Test
    fun `PIN verify success with missing pin unlock key falls back to passphrase`() = runTest {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(charArrayOf('1', '2', '3', '4'))).thenReturn(true)
        `when`(mockLocalKeyManager.cachedPinUnlockKey()).thenReturn(null)

        viewModel.submitPin(pin, isArabic = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(MainUiState.UnlockScreen.PASSPHRASE, state.unlockScreen)
        assertEquals("Setup needs to be refreshed — enter your passphrase", state.unlockErrorMessage)
    }

    @Test
    fun `PIN lockout before verify shows wait message`() = runTest {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(30L)

        viewModel.submitPin(pin, isArabic = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Wait 30 seconds", state.unlockErrorMessage)
        assertFalse(state.unlocked)
    }

    @Test
    fun `clearUnlockError clears error message`() = runTest {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(charArrayOf('1', '2', '3', '4'))).thenReturn(false)
        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(10L)

        viewModel.submitPin(pin, isArabic = false)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.unlockErrorMessage)

        viewModel.clearUnlockError()
        assertNull(viewModel.uiState.value.unlockErrorMessage)
    }

    @Test
    fun `onUnlocked with missing migration warmer shows blocking database state`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val viewModel = createViewModel()
            viewModel.setDatabaseWarmer {
                throw IllegalStateException(
                    "A migration from 2 to 1 was required but not found. " +
                        "Please provide the necessary Migration path via " +
                        "RoomDatabase.Builder.addMigration(Migration ...).",
                )
            }

            viewModel.onUnlocked()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.unlocked)
            assertFalse(state.isWarmingDatabase)
            assertTrue(state.isDatabaseBlocked)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `database warm failure returns to locked passphrase state`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val viewModel = createViewModel()
            viewModel.setDatabaseWarmer {
                throw IllegalStateException("database open failed")
            }

            viewModel.onUnlocked()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.unlocked)
            assertFalse(state.isWarmingDatabase)
            assertEquals(MainUiState.UnlockScreen.PASSPHRASE, state.unlockScreen)
            assertEquals("Unable to open protected notes", state.unlockErrorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `session invalidation clears unlocked state and cancels warming`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val viewModel = createViewModel()

            viewModel.onUnlocked()
            viewModel.invalidateSession()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.unlocked)
            assertFalse(state.isWarmingDatabase)
            assertEquals(MainUiState.UnlockScreen.PASSPHRASE, state.unlockScreen)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `missing migration message is detected as version mismatch`() {
        val error = IllegalStateException(
            "A migration from 3 to 2 was required but not found. " +
                "Please provide the necessary Migration path.",
        )
        assertTrue(isDatabaseVersionMismatch(error))
    }

    @Test
    fun `wrapped missing migration is detected through the cause chain`() {
        val root = IllegalStateException("A migration from 2 to 1 was required but not found.")
        assertTrue(isDatabaseVersionMismatch(RuntimeException("open failed", root)))
    }

    @Test
    fun `unrelated errors are not version mismatches`() {
        assertFalse(isDatabaseVersionMismatch(IllegalStateException("Wrong passphrase")))
        assertFalse(isDatabaseVersionMismatch(RuntimeException("nope")))
        assertFalse(isDatabaseVersionMismatch(IllegalStateException("A migration ran fine")))
    }

    private fun createViewModel(): MainViewModel {
        return MainViewModel(
            appContext = mockContext,
            localKeyManager = mockLocalKeyManager,
            appPinManager = mockAppPinManager,
            biometricUnlockManager = mockBiometricUnlockManager,
        )
    }
}
