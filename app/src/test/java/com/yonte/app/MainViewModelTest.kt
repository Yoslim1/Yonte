package com.yonte.app

import android.content.Context
import android.content.SharedPreferences
import com.yonte.core.security.AppPinManager
import com.yonte.core.security.BiometricGateCipher
import com.yonte.core.security.LocalKeyManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    private lateinit var mockBiometricGateCipher: BiometricGateCipher

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockLocalKeyManager = mock(LocalKeyManager::class.java)
        mockAppPinManager = mock(AppPinManager::class.java)
        mockBiometricGateCipher = mock(BiometricGateCipher::class.java)

        `when`(mockContext.getSharedPreferences(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.getString(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(null)
        `when`(mockLocalKeyManager.isFirstRun()).thenReturn(false)
        `when`(mockLocalKeyManager.unlockMethod()).thenReturn(LocalKeyManager.METHOD_PASSPHRASE)
    }

    @Test
    fun `PIN verify success restores session key and clears error state`() = runTest {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')
        val fakeKey = byteArrayOf(1, 2, 3, 4)

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(pin)).thenReturn(true)
        `when`(mockLocalKeyManager.cachedPinUnlockKey()).thenReturn(fakeKey)

        viewModel.submitPin(pin, isArabic = false)

        val state = viewModel.uiState.value
        assertNull(state.unlockErrorMessage)
        assertNull(state.unlockScreen)
        assertTrue(state.unlocked)
    }

    @Test
    fun `PIN verify failure with lockout produces expected remaining seconds`() {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(pin)).thenReturn(false)
        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(25L)

        viewModel.submitPin(pin, isArabic = false)

        val state = viewModel.uiState.value
        assertEquals("Wait 25 seconds", state.unlockErrorMessage)
        assertFalse(state.unlocked)
    }

    @Test
    fun `PIN verify failure without lockout shows wrong PIN`() {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(pin)).thenReturn(false)
        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)

        viewModel.submitPin(pin, isArabic = false)

        val state = viewModel.uiState.value
        assertEquals("Wrong PIN", state.unlockErrorMessage)
    }

    @Test
    fun `PIN verify failure with lockout shows Arabic message`() {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(pin)).thenReturn(false)
        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(15L)

        viewModel.submitPin(pin, isArabic = true)

        val state = viewModel.uiState.value
        assertEquals("انتظر 15 ثانية", state.unlockErrorMessage)
    }

    @Test
    fun `PIN verify success with missing pin unlock key falls back to passphrase`() {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(pin)).thenReturn(true)
        `when`(mockLocalKeyManager.cachedPinUnlockKey()).thenReturn(null)

        viewModel.submitPin(pin, isArabic = false)

        val state = viewModel.uiState.value
        assertEquals(MainUiState.UnlockScreen.PASSPHRASE, state.unlockScreen)
        assertEquals("Setup needs to be refreshed — enter your passphrase", state.unlockErrorMessage)
    }

    @Test
    fun `PIN lockout before verify shows wait message`() {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(30L)

        viewModel.submitPin(pin, isArabic = false)

        val state = viewModel.uiState.value
        assertEquals("Wait 30 seconds", state.unlockErrorMessage)
        assertFalse(state.unlocked)
    }

    @Test
    fun `clearUnlockError clears error message`() {
        val viewModel = createViewModel()
        val pin = charArrayOf('1', '2', '3', '4')

        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(0L)
        `when`(mockAppPinManager.verify(pin)).thenReturn(false)
        `when`(mockAppPinManager.lockoutSecondsRemaining()).thenReturn(10L)

        viewModel.submitPin(pin, isArabic = false)
        assertNotNull(viewModel.uiState.value.unlockErrorMessage)

        viewModel.clearUnlockError()
        assertNull(viewModel.uiState.value.unlockErrorMessage)
    }

    private fun createViewModel(): MainViewModel {
        return MainViewModel(
            appContext = mockContext,
            localKeyManager = mockLocalKeyManager,
            appPinManager = mockAppPinManager,
            biometricGateCipher = mockBiometricGateCipher,
        )
    }
}
