package com.yonte.app

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelStore
import com.yonte.core.database.isDatabaseVersionMismatch
import com.yonte.core.security.AppPinManager
import com.yonte.core.security.BiometricUnlockManager
import com.yonte.core.security.LocalKeyManager
import com.yonte.core.backup.ScheduledBackupWorker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
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
    fun `passphrase validates candidate before committing session`() = runTest {
        val candidate = byteArrayOf(9, 8, 7, 6)
        var validated = false
        var committedBeforeValidation = false

        `when`(mockLocalKeyManager.unlock(charArrayOf('p', 'a', 's', 's'))).thenReturn(candidate)
        val validator = ProtectedDatabaseValidator {
            committedBeforeValidation = hasInvocation("cacheSessionKeyDirectly")
            validated = true
        }
        val validatingViewModel = createViewModel(validator)

        val job = validatingViewModel.submitPassphrase(
            passphrase = charArrayOf('p', 'a', 's', 's'),
            isUnlocking = false,
            isArabic = false,
            onUnlockStarted = {},
            onUnlockFinished = {},
        )

        job?.join()
        advanceUntilIdle()

        assertTrue(validated)
        assertFalse(committedBeforeValidation)
        assertTrue(validatingViewModel.uiState.value.unlocked)
        assertTrue(candidate.all { it == 0.toByte() })
    }

    @Test
    fun `passphrase validation failure leaves session uncommitted`() = runTest {
        val viewModel = createViewModel(
            ProtectedDatabaseValidator { throw IllegalStateException("wrong database key") },
        )
        val candidate = byteArrayOf(1, 2, 3, 4)
        `when`(mockLocalKeyManager.unlock(charArrayOf('p', 'a', 's', 's'))).thenReturn(candidate)

        val job = viewModel.submitPassphrase(
            passphrase = charArrayOf('p', 'a', 's', 's'),
            isUnlocking = false,
            isArabic = false,
            onUnlockStarted = {},
            onUnlockFinished = {},
        )

        job?.join()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.unlocked)
        assertEquals(MainUiState.UnlockScreen.PASSPHRASE, viewModel.uiState.value.unlockScreen)
        assertFalse(hasInvocation("cacheSessionKeyDirectly"))
    }

    @Test
    fun `a stale passphrase attempt cannot overwrite a newer successful attempt`() = runTest {
        val firstValidationStarted = CompletableDeferred<Unit>()
        val releaseFirstValidation = CompletableDeferred<Unit>()
        var validationCount = 0
        val viewModel = createViewModel(
            ProtectedDatabaseValidator {
                validationCount++
                if (validationCount == 1) {
                    firstValidationStarted.complete(Unit)
                    withContext(NonCancellable) { releaseFirstValidation.await() }
                }
            },
        )
        val firstCandidate = byteArrayOf(1, 1, 1, 1)
        val secondCandidate = byteArrayOf(2, 2, 2, 2)
        `when`(mockLocalKeyManager.unlock(org.mockito.ArgumentMatchers.any(CharArray::class.java)))
            .thenReturn(firstCandidate, secondCandidate)

        val first = viewModel.submitPassphrase(
            charArrayOf('1'), false, false, {}, {},
        )
        firstValidationStarted.await()
        val second = viewModel.submitPassphrase(
            charArrayOf('2'), false, false, {}, {},
        )
        second?.join()
        releaseFirstValidation.complete(Unit)
        first?.join()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.unlocked)
        assertTrue(firstCandidate.all { it == 0.toByte() })
        assertTrue(secondCandidate.all { it == 0.toByte() })
    }

    @Test
    fun `passphrase cancellation before dispatched work clears caller buffer and finishes`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val viewModel = createViewModel()
            val passphrase = charArrayOf('s', 'e', 'c', 'r', 'e', 't')
            var finished = false

            val job = viewModel.submitPassphrase(passphrase, false, false, {}, { finished = true })
            viewModel.invalidateSession()
            job?.join()

            assertTrue(passphrase.all { it == '\u0000' })
            assertTrue(finished)
            assertFalse(viewModel.uiState.value.unlocked)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `database warming keeps session locked until protected access succeeds`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val viewModel = createViewModel()
            val releaseWarm = CompletableDeferred<Unit>()
            viewModel.setDatabaseWarmer { releaseWarm.await() }

            viewModel.onUnlocked()
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.unlocked)
            assertTrue(viewModel.uiState.value.isWarmingDatabase)

            releaseWarm.complete(Unit)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.unlocked)
            assertFalse(viewModel.uiState.value.isWarmingDatabase)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `biometric candidate validation failure never commits session`() = runTest {
        val viewModel = createViewModel(
            ProtectedDatabaseValidator { throw IllegalStateException("wrong biometric key") },
        )
        val attemptId = viewModel.beginBiometricUnlock()
        val sessionKey = byteArrayOf(6, 5, 4, 3)

        val job = viewModel.handleBiometricUnlockSuccess(sessionKey, attemptId)
        job?.join()

        assertFalse(viewModel.uiState.value.unlocked)
        assertFalse(hasInvocation("cacheSessionKeyDirectly"))
        assertTrue(sessionKey.all { it == 0.toByte() })
    }

    @Test
    fun `PIN cancellation during validation clears candidate and cannot unlock`() = runTest {
        val validationStarted = CompletableDeferred<Unit>()
        val releaseValidation = CompletableDeferred<Unit>()
        val pinUnlockKey = byteArrayOf(4, 3, 2, 1)
        val viewModel = createViewModel(
            ProtectedDatabaseValidator {
                validationStarted.complete(Unit)
                releaseValidation.await()
            },
        )
        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(charArrayOf('1', '2', '3', '4'))).thenReturn(true)
        `when`(mockLocalKeyManager.cachedPinUnlockKey()).thenReturn(pinUnlockKey)

        val job = viewModel.submitPin(charArrayOf('1', '2', '3', '4'), isArabic = false)
        validationStarted.await()
        viewModel.invalidateSession()
        releaseValidation.complete(Unit)
        job?.join()

        assertTrue(pinUnlockKey.all { it == 0.toByte() })
        assertFalse(viewModel.uiState.value.unlocked)
        assertFalse(hasInvocation("cacheSessionKeyDirectly"))
    }

    @Test
    fun `PIN persistence failure rolls back credential cache pending PIN and method`() = runTest {
        val viewModel = createViewModel()
        viewModel.choosePinCreate()
        `when`(mockLocalKeyManager.cachedSessionKey()).thenReturn(byteArrayOf(7, 7, 7, 7))

        viewModel.submitPin(charArrayOf('1', '2', '3', '4'), isArabic = false)?.join()
        doThrow(IllegalStateException("metadata write failed"))
            .`when`(mockLocalKeyManager)
            .setUnlockMethod(LocalKeyManager.METHOD_PIN)

        viewModel.submitPin(charArrayOf('1', '2', '3', '4'), isArabic = false)?.join()

        verify(mockLocalKeyManager).clearPinUnlockConfiguration()
        verify(mockAppPinManager).clearPin()
        val methodWrites = mockingDetails(mockLocalKeyManager).invocations
            .filter { it.method.name == "clearPinUnlockConfiguration" }
        assertTrue(methodWrites.isNotEmpty())

        // The old confirmation is gone: this is a fresh first entry, not a retry
        // that can persist a credential with stale state.
        viewModel.submitPin(charArrayOf('1', '2', '3', '4'), isArabic = false)?.join()
        assertEquals(
            1,
            mockingDetails(mockLocalKeyManager).invocations.count {
                it.method.name == "cachePinUnlockKey"
            },
        )
    }

    @Test
    fun `PIN rollback attempts credential clearing when local rollback reports failure`() = runTest {
        val viewModel = createViewModel()
        viewModel.choosePinCreate()
        `when`(mockLocalKeyManager.cachedSessionKey()).thenReturn(byteArrayOf(8, 8, 8, 8))
        viewModel.submitPin(charArrayOf('1', '2', '3', '4'), isArabic = false)?.join()
        doThrow(IllegalStateException("metadata write failed"))
            .`when`(mockLocalKeyManager)
            .setUnlockMethod(LocalKeyManager.METHOD_PIN)
        doThrow(IllegalStateException("local rollback failed"))
            .`when`(mockLocalKeyManager)
            .clearPinUnlockConfiguration()

        viewModel.submitPin(charArrayOf('1', '2', '3', '4'), isArabic = false)?.join()

        // A failed local transaction is observable, but it cannot prevent removal
        // of the independent PIN credential; therefore no usable PIN remains.
        verify(mockAppPinManager).clearPin()
        assertTrue(hasInvocation("clearSessionCache"))
        assertFalse(viewModel.uiState.value.unlocked)
        assertEquals(MainUiState.UnlockScreen.PIN, viewModel.uiState.value.unlockScreen)
    }

    @Test
    fun `PIN verify success restores session key and clears error state`() = runTest {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')
        val fakeKey = byteArrayOf(1, 2, 3, 4)

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(charArrayOf('1', '2', '3', '4'))).thenReturn(true)
        `when`(mockLocalKeyManager.cachedPinUnlockKey()).thenReturn(fakeKey)

        viewModel.submitPin(pin, isArabic = false)?.join()
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

        viewModel.submitPin(firstPin, isArabic = false)?.join()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.unlocked)

        viewModel.submitPin(secondPin, isArabic = false)?.join()
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

        viewModel.submitPin(pin, isArabic = false)?.join()
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

        viewModel.submitPin(pin, isArabic = false)?.join()
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

        viewModel.submitPin(pin, isArabic = true)?.join()
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

        viewModel.submitPin(pin, isArabic = false)?.join()
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

        viewModel.submitPin(pin, isArabic = false)?.join()
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

        viewModel.submitPin(pin, isArabic = false)?.join()
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
    fun `missing database warmer fails closed instead of publishing unlock`() = runTest {
        val viewModel = createViewModel(configureWarmer = false)

        viewModel.onUnlocked()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.unlocked)
        assertFalse(viewModel.uiState.value.isWarmingDatabase)
        assertEquals(MainUiState.UnlockScreen.PASSPHRASE, viewModel.uiState.value.unlockScreen)
    }

    @Test
    fun `ViewModel teardown cancels warming and clears the session`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val viewModel = createViewModel()
            val releaseWarm = CompletableDeferred<Unit>()
            viewModel.setDatabaseWarmer { releaseWarm.await() }
            val store = ViewModelStore()
            store.put("main", viewModel)

            viewModel.onUnlocked()
            store.clear()
            releaseWarm.complete(Unit)
            advanceUntilIdle()

            assertTrue(hasInvocation("clearSessionCache"))
            assertFalse(viewModel.uiState.value.unlocked)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `ViewModel teardown clears an in-flight passphrase candidate`() = runTest {
        val validationStarted = CompletableDeferred<Unit>()
        val releaseValidation = CompletableDeferred<Unit>()
        val candidate = byteArrayOf(3, 3, 3, 3)
        `when`(mockLocalKeyManager.unlock(org.mockito.ArgumentMatchers.any(CharArray::class.java)))
            .thenReturn(candidate)
        val viewModel = createViewModel(
            ProtectedDatabaseValidator {
                validationStarted.complete(Unit)
                releaseValidation.await()
            },
        )
        val store = ViewModelStore()
        store.put("main", viewModel)
        val passphrase = charArrayOf('s', 'e', 'c', 'r', 'e', 't')

        val job = viewModel.submitPassphrase(passphrase, false, false, {}, {})
        validationStarted.await()
        store.clear()
        releaseValidation.complete(Unit)
        job?.join()

        assertTrue(passphrase.all { it == '\u0000' })
        assertTrue(candidate.all { it == 0.toByte() })
        assertTrue(hasInvocation("clearSessionCache"))
        assertFalse(viewModel.uiState.value.unlocked)
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
    fun `PIN attempt completed after invalidation cannot publish a session`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val viewModel = createViewModel()
            viewModel.submitPin(charArrayOf('1', '2', '3', '4'), isArabic = false)
            viewModel.invalidateSession()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.unlocked)
            assertFalse(viewModel.uiState.value.isWarmingDatabase)
            assertEquals(MainUiState.UnlockScreen.PASSPHRASE, viewModel.uiState.value.unlockScreen)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `biometric result received after invalidation is rejected and cleared`() {
        val viewModel = createViewModel()
        val attemptId = viewModel.beginBiometricUnlock()
        viewModel.invalidateSession()
        val sessionKey = byteArrayOf(1, 2, 3, 4)

        assertNull(viewModel.handleBiometricUnlockSuccess(sessionKey, attemptId))
        assertTrue(sessionKey.all { it == 0.toByte() })
        assertFalse(viewModel.uiState.value.unlocked)
    }

    @Test
    fun `PIN candidate validation failure does not commit the session`() = runTest {
        val viewModel = createViewModel(
            ProtectedDatabaseValidator { throw IllegalStateException("wrong database key") },
        )
        val pin = charArrayOf('1', '2', '3', '4')
        val pinUnlockKey = byteArrayOf(1, 2, 3, 4)

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(charArrayOf('1', '2', '3', '4'))).thenReturn(true)
        `when`(mockLocalKeyManager.cachedPinUnlockKey()).thenReturn(pinUnlockKey)

        viewModel.submitPin(pin, isArabic = false)?.join()

        assertFalse(viewModel.uiState.value.unlocked)
        assertEquals(MainUiState.UnlockScreen.PIN, viewModel.uiState.value.unlockScreen)
        assertEquals("Unable to unlock with PIN", viewModel.uiState.value.unlockErrorMessage)
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

    private fun hasInvocation(methodName: String): Boolean =
        mockingDetails(mockLocalKeyManager).invocations.any { it.method.name == methodName }

    private fun createViewModel(
        candidateValidator: ProtectedDatabaseValidator = ProtectedDatabaseValidator { },
        configureWarmer: Boolean = true,
    ): MainViewModel {
        return MainViewModel(
            appContext = mockContext,
            localKeyManager = mockLocalKeyManager,
            appPinManager = mockAppPinManager,
            biometricUnlockManager = mockBiometricUnlockManager,
            protectedDatabaseValidator = candidateValidator,
        ).also { viewModel ->
            if (configureWarmer) viewModel.setDatabaseWarmer { }
        }
    }
}
