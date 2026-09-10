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
import kotlinx.coroutines.CancellationException
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
    private val lifecycleLock = Any()

    private var warmDatabase: (suspend () -> Unit)? = null
    private var databaseWarmJob: Job? = null
    private var authenticationJob: Job? = null
    private var pinSubmissionJob: Job? = null
    @Volatile private var lifecycleGeneration = 0L

    @Volatile private var pinSubmissionId = 0L
    @Volatile private var activePinSubmissionId: Long? = null
    @Volatile private var biometricAttemptId = 0L
    @Volatile private var activeBiometricAttemptId: Long? = null
    @Volatile private var biometricAttemptGeneration = 0L

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

    fun submitPassphrase(
        passphrase: CharArray,
        isUnlocking: Boolean,
        isArabic: Boolean,
        onUnlockStarted: () -> Unit,
        onUnlockFinished: () -> Unit,
    ) {
        if (isUnlocking) {
            passphrase.fill('\u0000')
            return
        }
        onUnlockStarted()
        _uiState.update { it.copy(unlockErrorMessage = null) }
        val generation = ++lifecycleGeneration
        authenticationJob?.cancel()
        authenticationJob = viewModelScope.launch {
            val chars = passphrase.copyOf()
            passphrase.fill('\u0000')
            var candidateKey: ByteArray? = null
            try {
                candidateKey = withContext(Dispatchers.Default) {
                    localKeyManager.unlock(chars)
                }
                validateCandidate(candidateKey ?: error("No candidate key after derivation"))
                synchronized(lifecycleLock) {
                    check(generation == lifecycleGeneration) { "Session invalidated during authentication" }
                    val validatedKey = candidateKey ?: error("No candidate key after validation")
                    localKeyManager.cacheSessionKeyDirectly(validatedKey)
                    validatedKey.fill(0)
                    candidateKey = null
                    _uiState.update { it.copy(unlockScreen = null) }
                    refreshAutoBackupKeyCacheIfEnabled()
                    onUnlocked()
                }
            } catch (_: CancellationException) {
                if (generation == lifecycleGeneration) {
                    failClosed(generation)
                }
                return@launch
            } catch (e: Exception) {
                if (generation != lifecycleGeneration) return@launch
                val mismatch = isDatabaseVersionMismatch(e)
                failClosed(
                    generation = generation,
                    unlockScreen = if (mismatch) null else MainUiState.UnlockScreen.PASSPHRASE,
                    errorMessage = if (mismatch) null else if (isArabic) "كلمة السر غلط" else "Wrong passphrase",
                    databaseBlocked = mismatch,
                )
            } finally {
                candidateKey?.fill(0)
                chars.fill('\u0000')
                onUnlockFinished()
            }
        }
    }

    fun submitPin(pin: CharArray, isArabic: Boolean): Job? {
        if (pinSubmissionInFlight) {
            pin.fill('\u0000')
            return null
        }
        pinSubmissionInFlight = true
        val generation = lifecycleGeneration
        val attemptId = ++pinSubmissionId
        activePinSubmissionId = attemptId
        val chars = pin.copyOf()
        pin.fill('\u0000')
        val job = viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    if (!isCurrentPinAttempt(generation, attemptId)) return@withContext
                    _uiState.update { it.copy(unlockErrorMessage = null) }
                    val currentMode = _uiState.value.pinMode
                    if (currentMode == PinFieldMode.CREATE) {
                        val currentCreatedPin = createdPin
                        if (currentCreatedPin == null) {
                            if (!isCurrentPinAttempt(generation, attemptId)) return@withContext
                            createdPin = chars.copyOf()
                            _uiState.update {
                                it.copy(pinMode = PinFieldMode.CREATE, unlockScreen = MainUiState.UnlockScreen.PIN)
                            }
                        } else {
                            if (!chars.contentEquals(currentCreatedPin)) {
                                if (!isCurrentPinAttempt(generation, attemptId)) return@withContext
                                currentCreatedPin.fill('\u0000')
                                createdPin = null
                                _uiState.update {
                                    it.copy(unlockScreen = MainUiState.UnlockScreen.PIN)
                                }
                            } else {
                                if (!isCurrentPinAttempt(generation, attemptId)) return@withContext
                                val sessionKey = localKeyManager.cachedSessionKey()
                                    ?: error("No session key available for PIN setup")
                                try {
                                    localKeyManager.cachePinUnlockKey(sessionKey)
                                    if (!isCurrentPinAttempt(generation, attemptId)) {
                                        localKeyManager.clearPinUnlockKey()
                                        return@withContext
                                    }
                                    appPinManager.setPin(chars)
                                    localKeyManager.setUnlockMethod(LocalKeyManager.METHOD_PIN)
                                } catch (e: Exception) {
                                    localKeyManager.clearPinUnlockKey()
                                    throw e
                                } finally {
                                    sessionKey.fill(0)
                                }
                                currentCreatedPin.fill('\u0000')
                                createdPin = null
                                _uiState.update {
                                    it.copy(unlockScreen = null)
                                }
                                refreshAutoBackupKeyCacheIfEnabled()
                                onUnlocked()
                            }
                        }
                    } else {
                        if (appPinManager.lockoutSecondsRemaining() > 0) {
                            val secs = appPinManager.lockoutSecondsRemaining()
                            if (!isCurrentPinAttempt(generation, attemptId)) return@withContext
                            _uiState.update {
                                it.copy(unlockErrorMessage = if (isArabic) "انتظر $secs ثانية" else "Wait $secs seconds")
                            }
                            return@withContext
                        }
                        if (!isCurrentPinAttempt(generation, attemptId)) return@withContext
                        if (appPinManager.verify(chars)) {
                            val pinUnlockKey = localKeyManager.cachedPinUnlockKey()
                            if (pinUnlockKey == null) {
                                if (!isCurrentPinAttempt(generation, attemptId)) return@withContext
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
                                validateCandidate(pinUnlockKey)
                                synchronized(lifecycleLock) {
                                    if (!isCurrentPinAttempt(generation, attemptId)) return@withContext
                                    localKeyManager.cacheSessionKeyDirectly(pinUnlockKey)
                                    refreshAutoBackupKeyCacheIfEnabled()
                                    _uiState.update { it.copy(unlockScreen = null) }
                                    onUnlocked()
                                }
                            } finally {
                                pinUnlockKey.fill(0)
                            }
                        } else {
                            val remaining = appPinManager.lockoutSecondsRemaining()
                            if (!isCurrentPinAttempt(generation, attemptId)) return@withContext
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
            } catch (_: CancellationException) {
                if (isCurrentPinAttempt(generation, attemptId)) {
                    failClosed(generation)
                }
            } catch (e: Exception) {
                if (isCurrentPinAttempt(generation, attemptId)) {
                    val mismatch = isDatabaseVersionMismatch(e)
                    failClosed(
                        generation = generation,
                        unlockScreen = if (mismatch) null else MainUiState.UnlockScreen.PIN,
                        errorMessage = if (mismatch) null else "Unable to unlock with PIN",
                        databaseBlocked = mismatch,
                    )
                }
            } finally {
                chars.fill('\u0000')
                if (activePinSubmissionId == attemptId) {
                    activePinSubmissionId = null
                    pinSubmissionInFlight = false
                }
            }
        }
        pinSubmissionJob = job
        return job
    }

    /** Starts one biometric attempt and returns its lifecycle token. */
    fun beginBiometricUnlock(): Long {
        val generation = ++lifecycleGeneration
        val attemptId = ++biometricAttemptId
        biometricAttemptGeneration = generation
        activeBiometricAttemptId = attemptId
        return attemptId
    }

    fun handleBiometricUnlockSuccess(sessionKey: ByteArray, attemptId: Long): Job? {
        val generation = biometricAttemptGeneration
        if (activeBiometricAttemptId != attemptId || generation != lifecycleGeneration) {
            sessionKey.fill(0)
            return null
        }
        val job = viewModelScope.launch {
            try {
                validateCandidate(sessionKey)
                synchronized(lifecycleLock) {
                    if (!isCurrentBiometricAttempt(generation, attemptId)) return@launch
                    localKeyManager.cacheSessionKeyDirectly(sessionKey)
                    activeBiometricAttemptId = null
                    refreshAutoBackupKeyCacheIfEnabled()
                    _uiState.update { it.copy(unlockScreen = null) }
                    onUnlocked()
                }
            } catch (_: CancellationException) {
                if (isCurrentBiometricAttempt(generation, attemptId)) failClosed(generation)
            } catch (e: Exception) {
                if (isCurrentBiometricAttempt(generation, attemptId)) {
                    val mismatch = isDatabaseVersionMismatch(e)
                    failClosed(
                        generation = generation,
                        unlockScreen = if (mismatch) null else MainUiState.UnlockScreen.BIOMETRIC,
                        errorMessage = if (mismatch) null else "Unable to unlock with biometrics",
                        databaseBlocked = mismatch,
                    )
                }
            } finally {
                sessionKey.fill(0)
            }
        }
        return job
    }

    fun handleBiometricUnlockError(
        errorCode: Int,
        errString: CharSequence,
        attemptId: Long? = null,
    ) {
        if (attemptId != null && activeBiometricAttemptId != attemptId) return
        activeBiometricAttemptId = null
        if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
            errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
        ) {
            _uiState.update { it.copy(unlockErrorMessage = errString.toString()) }
        }
    }

    fun handleBiometricUnlockFailure(isArabic: Boolean, attemptId: Long? = null) {
        if (attemptId != null && activeBiometricAttemptId != attemptId) return
        activeBiometricAttemptId = null
        _uiState.update {
            it.copy(unlockErrorMessage = if (isArabic) "فشل فتح القفل" else "Unlock failed")
        }
    }

    fun clearUnlockError() {
        _uiState.update { it.copy(unlockErrorMessage = null) }
    }

    fun onUnlocked() {
        val generation = ++lifecycleGeneration
        activeBiometricAttemptId = null
        databaseWarmJob?.cancel()
        _uiState.update { it.copy(unlocked = true, isWarmingDatabase = true) }
        databaseWarmJob = viewModelScope.launch {
            val result = try {
                warmDatabase?.invoke()
                Result.success(Unit)
            } catch (_: CancellationException) {
                if (generation == lifecycleGeneration) failClosed(generation)
                return@launch
            } catch (e: Exception) {
                Result.failure<Unit>(e)
            }
            if (generation != lifecycleGeneration) return@launch
            val failure = result.exceptionOrNull()
            if (failure != null) {
                val mismatch = isDatabaseVersionMismatch(failure)
                failClosed(
                    generation = generation,
                    unlockScreen = if (mismatch) null else MainUiState.UnlockScreen.PASSPHRASE,
                    errorMessage = if (mismatch) null else "Unable to open protected notes",
                    databaseBlocked = mismatch,
                )
                _uiState.update { it.copy(isWarmingDatabase = false) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    unlocked = true,
                    isWarmingDatabase = false,
                    unlockScreen = it.unlockScreen,
                )
            }
        }
    }

    /** Invalidates the interactive session and cancels any pending database warm. */
    fun invalidateSession() = synchronized(lifecycleLock) {
        lifecycleGeneration++
        activeBiometricAttemptId = null
        activePinSubmissionId = null
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

    private fun isCurrentPinAttempt(generation: Long, attemptId: Long): Boolean =
        generation == lifecycleGeneration && activePinSubmissionId == attemptId

    private fun isCurrentBiometricAttempt(generation: Long, attemptId: Long): Boolean =
        generation == lifecycleGeneration && activeBiometricAttemptId == attemptId

    private suspend fun validateCandidate(candidateKey: ByteArray) {
        val validationDatabase = withContext(Dispatchers.IO) {
            YonteDatabase.openForValidation(appContext, candidateKey)
        }
        try {
            withContext(Dispatchers.IO) {
                validationDatabase.noteDao().getAll()
            }
        } finally {
            validationDatabase.close()
        }
    }

    private fun failClosed(
        generation: Long,
        unlockScreen: MainUiState.UnlockScreen? = MainUiState.UnlockScreen.PASSPHRASE,
        errorMessage: String? = null,
        databaseBlocked: Boolean = false,
    ) = synchronized(lifecycleLock) {
        if (generation != lifecycleGeneration) return@synchronized
        YonteDatabase.close()
        localKeyManager.clearSessionCache()
        _uiState.update {
            it.copy(
                unlocked = false,
                isWarmingDatabase = false,
                unlockScreen = unlockScreen,
                unlockErrorMessage = errorMessage,
                isDatabaseBlocked = it.isDatabaseBlocked || databaseBlocked,
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
        try {
            refreshAutoBackupKeyCacheIfEnabled()
            _uiState.update { it.copy(unlockScreen = null) }
            onUnlocked()
        } catch (_: Exception) {
            val generation = lifecycleGeneration
            failClosed(
                generation = generation,
                unlockScreen = MainUiState.UnlockScreen.PASSPHRASE,
                errorMessage = "Unable to open protected notes",
            )
        }
    }

    fun switchToPassphrase() {
        activeBiometricAttemptId = null
        _uiState.update {
            it.copy(unlockScreen = MainUiState.UnlockScreen.PASSPHRASE, unlockErrorMessage = null)
        }
    }

    fun switchToPinOrPassphrase() {
        activeBiometricAttemptId = null
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
        activeBiometricAttemptId = null
        activePinSubmissionId = null
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
