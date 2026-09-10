package com.yonte.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yonte.core.backup.ScheduledBackupWorker
import com.yonte.core.database.YonteDatabase
import com.yonte.core.database.isDatabaseVersionMismatch
import com.yonte.core.security.AppPinManager
import com.yonte.core.security.BiometricUnlockManager
import com.yonte.core.security.LocalKeyManager
import com.yonte.feature.onboarding.PinFieldMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val biometricUnlockManager: BiometricUnlockManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private var createdPin: CharArray? = null
    private var pinSubmissionInFlight = false

    private var warmDatabase: (suspend () -> Unit)? = null
    private var databaseWarmJob: Job? = null
    private var authenticationJob: Job? = null
    private var pinSubmissionJob: Job? = null
    @Volatile private var lifecycleGeneration = 0L

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
            var setupKey: ByteArray? = null
            try {
                setupKey = withContext(Dispatchers.Default) {
                    localKeyManager.setupPassphrase(chars)
                }
            } finally {
                setupKey?.fill(0)
                chars.fill('\u0000')
            }
            _uiState.update {
                it.copy(showOnboarding = false, unlockScreen = MainUiState.UnlockScreen.SETUP)
            }
            onFinished()
        }
    }

    fun submitPassphrase(passphrase: CharArray, isUnlocking: Boolean, isArabic: Boolean, onUnlockStarted: () -> Unit, onUnlockFinished: () -> Unit) {
        if (isUnlocking) return
        onUnlockStarted()
        _uiState.update { it.copy(unlockErrorMessage = null) }
        val generation = ++lifecycleGeneration
        authenticationJob?.cancel()
        authenticationJob = viewModelScope.launch {
            val chars = passphrase.copyOf()
            var candidateKey: ByteArray? = null
            try {
                candidateKey = withContext(Dispatchers.Default) {
                    // Derivation produces a candidate only. It is not committed to
                    // session state until the protected database validates it.
                    localKeyManager.unlock(chars)
                }
                withContext(Dispatchers.IO) {
                    val key = candidateKey ?: error("No candidate key after derivation")
                    YonteDatabase.get(appContext, key).noteDao().getAll()
                }
                check(generation == lifecycleGeneration) { "Session invalidated during authentication" }
                val validatedKey = candidateKey ?: error("No candidate key after validation")
                localKeyManager.cacheSessionKeyDirectly(validatedKey)
                validatedKey.fill(0)
                candidateKey = null
                _uiState.update { it.copy(unlockScreen = null) }
                onUnlocked()
                refreshAutoBackupKeyCacheIfEnabled()
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (generation == lifecycleGeneration) {
                    YonteDatabase.close()
                    localKeyManager.clearSessionCache()
                }
                return@launch
            } catch (e: Exception) {
                if (generation != lifecycleGeneration) return@launch
                YonteDatabase.close()
                localKeyManager.clearSessionCache()
                if (isDatabaseVersionMismatch(e)) {
                    _uiState.update {
                        it.copy(
                            unlocked = false,
                            unlockScreen = null,
                            unlockErrorMessage = null,
                            isDatabaseBlocked = true,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            unlocked = false,
                            unlockScreen = MainUiState.UnlockScreen.PASSPHRASE,
                            unlockErrorMessage = if (isArabic) "كلمة السر غلط" else "Wrong passphrase",
                        )
                    }
                }
            } finally {
                candidateKey?.fill(0)
                chars.fill('\u0000')
                onUnlockFinished()
            }
        }
    }

    fun submitPin(pin: CharArray, isArabic: Boolean) {
        if (pinSubmissionInFlight) { pin.fill('\u0000'); return }
        pinSubmissionInFlight = true
        val chars = pin.copyOf()
        pin.fill('\u0000')
        pinSubmissionJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    _uiState.update { it.copy(unlockErrorMessage = null) }
                    val currentMode = _uiState.value.pinMode
                    if (currentMode == PinFieldMode.CREATE) {
                        val currentCreatedPin = createdPin
                        if (currentCreatedPin == null) {
                            createdPin = chars.copyOf()
                            _uiState.update {
                                it.copy(pinMode = PinFieldMode.CREATE, unlockScreen = MainUiState.UnlockScreen.PIN)
                            }
                        } else {
                            if (!chars.contentEquals(currentCreatedPin)) {
                                currentCreatedPin.fill('\u0000')
                                createdPin = null
                                _uiState.update {
                                    it.copy(unlockScreen = MainUiState.UnlockScreen.PIN)
                                }
                            } else {
                                appPinManager.setPin(chars)
                                localKeyManager.setUnlockMethod(LocalKeyManager.METHOD_PIN)
                                localKeyManager.cachedSessionKey()?.let { key ->
                                    try {
                                        localKeyManager.cachePinUnlockKey(key)
                                    } finally {
                                        key.fill(0)
                                    }
                                }
                                currentCreatedPin.fill('\u0000')
                                createdPin = null
                                _uiState.update {
                                    it.copy(unlockScreen = null)
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
                            return@withContext
                        }
                        if (appPinManager.verify(chars)) {
                            val pinUnlockKey = localKeyManager.cachedPinUnlockKey()
                            if (pinUnlockKey == null) {
                                _uiState.update {
                                    it.copy(
                                        unlockScreen = MainUiState.UnlockScreen.PASSPHRASE,
                                        unlockErrorMessage = if (isArabic)
                                            "محتاجين نعيد الإعداد، ادخل كلمة السر" else "Setup needs to be refreshed — enter your passphrase",
                                    )
                                }
                                return@withContext
                            }
                            try {
                                localKeyManager.cacheSessionKeyDirectly(pinUnlockKey)
                                _uiState.update { it.copy(unlockScreen = null) }
                                onUnlocked()
                                refreshAutoBackupKeyCacheIfEnabled()
                            } finally {
                                pinUnlockKey.fill(0)
                            }
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
            } finally {
                chars.fill('\u0000')
                pinSubmissionInFlight = false
            }
        }
    }

    fun handleBiometricUnlockSuccess(sessionKey: ByteArray) {
        try {
            localKeyManager.cacheSessionKeyDirectly(sessionKey)
            _uiState.update { it.copy(unlockScreen = null) }
            onUnlocked()
            refreshAutoBackupKeyCacheIfEnabled()
        } finally {
            sessionKey.fill(0)
        }
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
        val generation = ++lifecycleGeneration
        databaseWarmJob?.cancel()
        _uiState.update { it.copy(unlocked = true, isWarmingDatabase = true) }
        databaseWarmJob = viewModelScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    warmDatabase?.invoke()
                }
                Result.success(Unit)
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (generation == lifecycleGeneration) {
                    YonteDatabase.close()
                    localKeyManager.clearSessionCache()
                }
                return@launch
            } catch (e: Exception) {
                Result.failure<Unit>(e)
            }
            if (generation != lifecycleGeneration) return@launch
            val failure = result.exceptionOrNull()
            if (failure != null) {
                YonteDatabase.close()
                localKeyManager.clearSessionCache()
            }
            val mismatch = failure?.let(::isDatabaseVersionMismatch) ?: false
            _uiState.update {
                it.copy(
                    unlocked = failure == null,
                    isWarmingDatabase = false,
                    unlockScreen = when {
                        mismatch -> null
                        failure != null -> MainUiState.UnlockScreen.PASSPHRASE
                        else -> it.unlockScreen
                    },
                    unlockErrorMessage = when {
                        mismatch -> null
                        failure != null -> "Unable to open protected notes"
                        else -> it.unlockErrorMessage
                    },
                    isDatabaseBlocked = it.isDatabaseBlocked || mismatch,
                )
            }
        }
    }

    /** Invalidates the interactive session and cancels any pending database warm. */
    fun invalidateSession() {
        lifecycleGeneration++
        authenticationJob?.cancel()
        pinSubmissionJob?.cancel()
        databaseWarmJob?.cancel()
        databaseWarmJob = null
        createdPin?.fill('\u0000')
        createdPin = null
        pinSubmissionInFlight = false
        YonteDatabase.close()
        localKeyManager.clearSessionCache()
        _uiState.update {
            it.copy(
                unlocked = false,
                isWarmingDatabase = false,
                unlockScreen = when (localKeyManager.unlockMethod()) {
                    LocalKeyManager.METHOD_PIN -> MainUiState.UnlockScreen.PIN
                    LocalKeyManager.METHOD_BIOMETRIC -> MainUiState.UnlockScreen.BIOMETRIC
                    else -> MainUiState.UnlockScreen.PASSPHRASE
                },
                unlockErrorMessage = null,
            )
        }
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
        try {
            localKeyManager.cacheAutoBackupKey(key)
        } finally {
            key.fill(0)
        }
    }

    override fun onCleared() {
        lifecycleGeneration++
        authenticationJob?.cancel()
        pinSubmissionJob?.cancel()
        databaseWarmJob?.cancel()
        YonteDatabase.close()
        localKeyManager.clearSessionCache()
        super.onCleared()
        createdPin?.fill('\u0000')
        createdPin = null
        pinSubmissionInFlight = false
    }
}
