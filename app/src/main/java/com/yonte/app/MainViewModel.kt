package com.yonte.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yonte.core.backup.ScheduledBackupWorker
import com.yonte.core.database.YonteDatabase
import com.yonte.core.security.AppPinManager
import com.yonte.core.security.BiometricGateCipher
import com.yonte.core.security.LocalKeyManager
import com.yonte.feature.onboarding.PinFieldMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val localKeyManager: LocalKeyManager,
    private val appPinManager: AppPinManager,
    @Suppress("unused") private val biometricGateCipher: BiometricGateCipher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var warmDatabase: (suspend () -> Unit)? = null

    fun setDatabaseWarmer(warmer: suspend () -> Unit) {
        warmDatabase = warmer
    }

    init {
        val isFirstRun = localKeyManager.isFirstRun()
        _uiState.update {
            it.copy(showOnboarding = isFirstRun)
        }
        if (!isFirstRun) {
            localKeyManager.clearSessionCache()
            val method = localKeyManager.unlockMethod()
            _uiState.update {
                it.copy(
                    unlocked = false,
                    unlockScreen = when (method) {
                        LocalKeyManager.METHOD_PIN -> MainUiState.UnlockScreen.PIN
                        LocalKeyManager.METHOD_BIOMETRIC -> MainUiState.UnlockScreen.BIOMETRIC
                        else -> MainUiState.UnlockScreen.PASSPHRASE
                    },
                )
            }
        }
    }

    fun completeOnboarding(passphrase: String, isUnlocking: Boolean, onStarted: () -> Unit, onFinished: () -> Unit) {
        if (isUnlocking) return
        onStarted()
        viewModelScope.launch {
            val chars = passphrase.toCharArray()
            try {
                withContext(Dispatchers.Default) {
                    localKeyManager.setupPassphrase(chars)
                }
            } finally {
                chars.fill('\u0000')
            }
            _uiState.update {
                it.copy(showOnboarding = false, unlockScreen = MainUiState.UnlockScreen.SETUP)
            }
            onFinished()
        }
    }

    fun submitPassphrase(passphrase: CharArray, context: Context, isUnlocking: Boolean, isArabic: Boolean, onUnlockStarted: () -> Unit, onUnlockFinished: () -> Unit) {
        if (isUnlocking) return
        onUnlockStarted()
        _uiState.update { it.copy(unlockErrorMessage = null) }
        viewModelScope.launch {
            val chars = passphrase.copyOf()
            try {
                withContext(Dispatchers.Default) {
                    localKeyManager.unlock(chars)
                }
                withContext(Dispatchers.IO) {
                    val key = localKeyManager.cachedSessionKey()
                        ?: error("No cached key after unlock")
                    YonteDatabase.get(context, key).noteDao().getAll()
                }
                _uiState.update { it.copy(unlockScreen = null) }
                onUnlocked()
                refreshAutoBackupKeyCacheIfEnabled()
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(unlockErrorMessage = if (isArabic) "كلمة السر غلط" else "Wrong passphrase")
                }
            } finally {
                chars.fill('\u0000')
                onUnlockFinished()
            }
        }
    }

    fun submitPin(pin: CharArray, isArabic: Boolean) {
        _uiState.update { it.copy(unlockErrorMessage = null) }
        val currentMode = _uiState.value.pinMode
        if (currentMode == PinFieldMode.CREATE) {
            val currentCreatedPin = _uiState.value.createdPin
            if (currentCreatedPin == null) {
                _uiState.update {
                    it.copy(createdPin = pin.copyOf(), pinMode = PinFieldMode.CREATE, unlockScreen = MainUiState.UnlockScreen.PIN)
                }
            } else {
                if (!pin.contentEquals(currentCreatedPin)) {
                    _uiState.update {
                        it.copy(createdPin = null, unlockScreen = MainUiState.UnlockScreen.PIN)
                    }
                } else {
                    appPinManager.setPin(pin)
                    localKeyManager.setUnlockMethod(LocalKeyManager.METHOD_PIN)
                    localKeyManager.cachedSessionKey()?.let { localKeyManager.cachePinUnlockKey(it) }
                    _uiState.update {
                        it.copy(createdPin = null, unlockScreen = null)
                    }
                    onUnlocked()
                    refreshAutoBackupKeyCacheIfEnabled()
                }
            }
        } else {
            if (appPinManager.lockoutSecondsRemaining() > 0) {
                val secs = appPinManager.lockoutSecondsRemaining()
                _uiState.update {
                    it.copy(unlockErrorMessage = if (isArabic) "انتظر $secs ثانية" else "Wait $secs seconds")
                }
                return
            }
            if (appPinManager.verify(pin)) {
                val pinUnlockKey = localKeyManager.cachedPinUnlockKey()
                if (pinUnlockKey == null) {
                    _uiState.update {
                        it.copy(
                            unlockScreen = MainUiState.UnlockScreen.PASSPHRASE,
                            unlockErrorMessage = if (isArabic)
                                "محتاجين نعيد الإعداد، ادخل كلمة السر" else "Setup needs to be refreshed — enter your passphrase",
                        )
                    }
                    return
                }
                localKeyManager.cacheSessionKeyDirectly(pinUnlockKey)
                _uiState.update { it.copy(unlockScreen = null) }
                onUnlocked()
                refreshAutoBackupKeyCacheIfEnabled()
            } else {
                val remaining = appPinManager.lockoutSecondsRemaining()
                _uiState.update {
                    it.copy(
                        unlockErrorMessage = if (remaining > 0) {
                            if (isArabic) "انتظر $remaining ثانية" else "Wait $remaining seconds"
                        } else {
                            if (isArabic) "رمز غلط" else "Wrong PIN"
                        },
                    )
                }
            }
        }
    }

    fun handleBiometricUnlockSuccess(sessionKey: ByteArray) {
        localKeyManager.cacheSessionKeyDirectly(sessionKey)
        _uiState.update { it.copy(unlockScreen = null) }
        onUnlocked()
        refreshAutoBackupKeyCacheIfEnabled()
    }

    fun handleBiometricUnlockError(errorCode: Int, errString: CharSequence) {
        if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
            errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
        ) {
            _uiState.update { it.copy(unlockErrorMessage = errString.toString()) }
        }
    }

    fun handleBiometricUnlockFailure(isArabic: Boolean) {
        _uiState.update {
            it.copy(unlockErrorMessage = if (isArabic) "فشل فتح القفل" else "Unlock failed")
        }
    }

    fun clearUnlockError() {
        _uiState.update { it.copy(unlockErrorMessage = null) }
    }

    fun onUnlocked() {
        _uiState.update { it.copy(unlocked = true, isWarmingDatabase = true) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { warmDatabase?.invoke() }
            }
            _uiState.update { it.copy(isWarmingDatabase = false) }
        }
    }

    fun chooseBiometricUnlock() {
        localKeyManager.setUnlockMethod(LocalKeyManager.METHOD_BIOMETRIC)
    }

    fun choosePinCreate() {
        _uiState.update {
            it.copy(pinMode = PinFieldMode.CREATE, unlockScreen = MainUiState.UnlockScreen.PIN)
        }
    }

    fun chooseSkipUnlock() {
        localKeyManager.setUnlockMethod(LocalKeyManager.METHOD_PASSPHRASE)
        _uiState.update { it.copy(unlockScreen = null) }
        onUnlocked()
        refreshAutoBackupKeyCacheIfEnabled()
    }

    fun switchToPassphrase() {
        _uiState.update {
            it.copy(unlockScreen = MainUiState.UnlockScreen.PASSPHRASE, unlockErrorMessage = null)
        }
    }

    fun switchToPinOrPassphrase() {
        _uiState.update {
            it.copy(
                unlockScreen = if (appPinManager.isPinSet()) MainUiState.UnlockScreen.PIN else MainUiState.UnlockScreen.PASSPHRASE,
                unlockErrorMessage = null,
            )
        }
    }

    private fun refreshAutoBackupKeyCacheIfEnabled() {
        val prefs = appContext.getSharedPreferences(ScheduledBackupWorker.PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(ScheduledBackupWorker.KEY_DESTINATION_URI, null) == null) return
        val key = localKeyManager.cachedSessionKey() ?: return
        localKeyManager.cacheAutoBackupKey(key)
    }
}
