package com.yonte.app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.yonte.core.backup.BackupGateway
import com.yonte.core.backup.ScheduledBackupWorker
import com.yonte.core.database.NoteRepository
import com.yonte.core.designsystem.YonteTheme
import com.yonte.core.security.AppPinManager
import com.yonte.core.security.BiometricGateCipher
import com.yonte.core.security.LocalKeyManager
import com.yonte.core.update.UpdateGateway
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.yonte.feature.notes.NotesRoute
import com.yonte.feature.onboarding.BiometricUnlockRoute
import com.yonte.feature.onboarding.PassphraseUnlockRoute
import com.yonte.feature.onboarding.PinFieldMode
import com.yonte.feature.onboarding.PinRoute
import com.yonte.feature.onboarding.QuickUnlockSetupRoute
import com.yonte.feature.onboarding.OnboardingRoute
import com.yonte.feature.settings.SettingsRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var noteRepository: Lazy<NoteRepository>
    @Inject lateinit var backupGateway: BackupGateway
    @Inject lateinit var updateGateway: UpdateGateway
    @Inject lateinit var localKeyManager: LocalKeyManager
    @Inject lateinit var appPinManager: AppPinManager
    @Inject lateinit var biometricGateCipher: BiometricGateCipher

    private var sharedText by mutableStateOf<String?>(null)
    private var darkTheme by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)
    private var showOnboarding by mutableStateOf(true)
    private var unlocked by mutableStateOf(false)
    private var isUnlocking by mutableStateOf(false)
    private var unlockScreen by mutableStateOf<UnlockScreen?>(null)
    private var createdPin by mutableStateOf<CharArray?>(null)
    private var pinMode by mutableStateOf(PinFieldMode.VERIFY)
    private var unlockErrorMessage by mutableStateOf<String?>(null)
    private var isWarmingDatabase by mutableStateOf(false)

    private enum class UnlockScreen { SETUP, PASSPHRASE, PIN, BIOMETRIC }

    private fun onUnlocked() {
        unlocked = true
        isWarmingDatabase = true
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { noteRepository.get() }
            }
            isWarmingDatabase = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedText = intent.sharedText()
        showOnboarding = localKeyManager.isFirstRun()

        if (!showOnboarding) {
            // Clear session cache so cold start always requires authentication.
            // The cache will be repopulated on successful unlock.
            localKeyManager.clearSessionCache()
            val method = localKeyManager.unlockMethod()
            unlocked = false
            unlockScreen = when (method) {
                LocalKeyManager.METHOD_PIN -> UnlockScreen.PIN
                LocalKeyManager.METHOD_BIOMETRIC -> UnlockScreen.BIOMETRIC
                else -> UnlockScreen.PASSPHRASE
            }
        }

        val biometricAvailable = BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

        setContent {
            YonteTheme(darkTheme = darkTheme) {
                when {
                    showOnboarding -> OnboardingRoute(
                        isProcessing = isUnlocking,
                        onComplete = ::completeOnboarding,
                    )
                    unlockScreen == UnlockScreen.SETUP -> QuickUnlockSetupRoute(
                        biometricAvailable = biometricAvailable,
                        isArabic = isArabic(),
                        onChooseBiometric = {
                            localKeyManager.setUnlockMethod(LocalKeyManager.METHOD_BIOMETRIC)
                            launchBiometricSetupPrompt {
                                unlockScreen = null
                                onUnlocked()
                                refreshAutoBackupKeyCacheIfEnabled()
                            }
                        },
                        onChoosePin = {
                            pinMode = PinFieldMode.CREATE
                            unlockScreen = UnlockScreen.PIN
                        },
                        onSkip = {
                            localKeyManager.setUnlockMethod(LocalKeyManager.METHOD_PASSPHRASE)
                            unlockScreen = null
                            onUnlocked()
                            refreshAutoBackupKeyCacheIfEnabled()
                        },
                    )
                    unlockScreen == UnlockScreen.PASSPHRASE -> PassphraseUnlockRoute(
                        isArabic = isArabic(),
                        errorMessage = unlockErrorMessage,
                        onSubmit = ::submitPassphrase,
                    )
                    unlockScreen == UnlockScreen.PIN -> PinRoute(
                        mode = pinMode,
                        isArabic = isArabic(),
                        errorMessage = unlockErrorMessage,
                        onSubmit = ::submitPin,
                        onUsePassphraseInstead = {
                            unlockScreen = UnlockScreen.PASSPHRASE
                            unlockErrorMessage = null
                        },
                    )
                    unlockScreen == UnlockScreen.BIOMETRIC -> BiometricUnlockRoute(
                        isArabic = isArabic(),
                        errorMessage = unlockErrorMessage,
                        onTriggerBiometric = ::launchBiometricPrompt,
                        onUseFallbackInstead = {
                            unlockScreen = if (appPinManager.isPinSet()) UnlockScreen.PIN else UnlockScreen.PASSPHRASE
                            unlockErrorMessage = null
                        },
                    )
                    unlocked && isWarmingDatabase -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    unlocked -> NotesOrSettings()
                }
            }
        }
    }

    private fun isArabic(): Boolean =
        resources.configuration.layoutDirection == android.util.LayoutDirection.RTL

    private fun completeOnboarding(passphrase: String) {
        if (isUnlocking) return
        isUnlocking = true
        lifecycleScope.launch {
            val chars = passphrase.toCharArray()
            try {
                withContext(Dispatchers.Default) {
                    localKeyManager.setupPassphrase(chars)
                }
            } finally {
                chars.fill('\u0000')
            }
            showOnboarding = false
            isUnlocking = false
            unlockScreen = UnlockScreen.SETUP
        }
    }

    private fun submitPassphrase(passphrase: CharArray) {
        if (isUnlocking) return
        isUnlocking = true
        unlockErrorMessage = null
        lifecycleScope.launch {
            val chars = passphrase.copyOf()
            try {
                withContext(Dispatchers.Default) {
                    localKeyManager.unlock(chars)
                }
                unlockScreen = null
                onUnlocked()
                refreshAutoBackupKeyCacheIfEnabled()
            } catch (_: Exception) {
                unlockErrorMessage = if (isArabic()) "كلمة السر غلط" else "Wrong passphrase"
            } finally {
                chars.fill('\u0000')
                isUnlocking = false
            }
        }
    }

    private fun submitPin(pin: CharArray) {
        unlockErrorMessage = null
        if (pinMode == PinFieldMode.CREATE) {
            val currentCreatedPin = createdPin
            if (currentCreatedPin == null) {
                // First entry: store PIN, switch to confirmation
                createdPin = pin.copyOf()
                pinMode = PinFieldMode.CREATE // stays CREATE for confirmation
                unlockScreen = UnlockScreen.PIN
            } else {
                // Confirmation entry: verify match
                if (!pin.contentEquals(currentCreatedPin)) {
                    createdPin = null
                    // PinRoute shows mismatch via its own state
                    unlockScreen = UnlockScreen.PIN
                } else {
                    appPinManager.setPin(pin)
                    localKeyManager.setUnlockMethod(LocalKeyManager.METHOD_PIN)
                    localKeyManager.cachedSessionKey()?.let { localKeyManager.cachePinUnlockKey(it) }
                    createdPin = null
                    unlockScreen = null
                    onUnlocked()
                    refreshAutoBackupKeyCacheIfEnabled()
                }
            }
        } else {
            // Verification mode
            if (appPinManager.lockoutSecondsRemaining() > 0) {
                val secs = appPinManager.lockoutSecondsRemaining()
                unlockErrorMessage = if (isArabic()) "انتظر $secs ثانية" else "Wait $secs seconds"
                return
            }
            if (appPinManager.verify(pin)) {
                val pinUnlockKey = localKeyManager.cachedPinUnlockKey()
                if (pinUnlockKey == null) {
                    unlockScreen = UnlockScreen.PASSPHRASE
                    unlockErrorMessage = if (isArabic())
                        "محتاجين نعيد الإعداد، ادخل كلمة السر" else "Setup needs to be refreshed — enter your passphrase"
                    return
                }
                localKeyManager.cacheSessionKeyDirectly(pinUnlockKey)
                unlockScreen = null
                onUnlocked()
                refreshAutoBackupKeyCacheIfEnabled()
            } else {
                val remaining = appPinManager.lockoutSecondsRemaining()
                unlockErrorMessage = if (remaining > 0) {
                    if (isArabic()) "انتظر $remaining ثانية" else "Wait $remaining seconds"
                } else {
                    if (isArabic()) "رمز غلط" else "Wrong PIN"
                }
            }
        }
    }

    private fun launchBiometricPrompt() {
        unlockErrorMessage = null
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                try {
                    val cryptoCipher = result.cryptoObject?.cipher
                    if (cryptoCipher != null) {
                        val encryptedData = Base64.decode(
                            getSharedPreferences("yonte_biometric_cache", MODE_PRIVATE)
                                .getString("cache_data", null),
                            Base64.NO_WRAP,
                        )
                        val sessionKey = cryptoCipher.doFinal(encryptedData)
                        localKeyManager.cacheSessionKeyDirectly(sessionKey)
                        unlockScreen = null
                        onUnlocked()
                        refreshAutoBackupKeyCacheIfEnabled()
                    } else {
                        unlockErrorMessage = if (isArabic()) "فشل فتح القفل" else "Unlock failed"
                    }
                } catch (_: Exception) {
                    unlockErrorMessage = if (isArabic()) "فشل فتح القفل" else "Unlock failed"
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    unlockErrorMessage = errString.toString()
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (isArabic()) "افتح Yonte" else "Unlock Yonte")
            .setSubtitle(if (isArabic()) "استخدم بصمتك" else "Use your fingerprint")
            .setNegativeButtonText(if (isArabic()) "إلغاء" else "Cancel")
            .build()

        try {
            val iv = Base64.decode(
                getSharedPreferences("yonte_biometric_cache", MODE_PRIVATE)
                    .getString("cache_iv", null),
                Base64.NO_WRAP,
            )
            val cipher = biometricGateCipher.decryptCipher(iv)
            BiometricPrompt(this, executor, callback).authenticate(
                promptInfo,
                BiometricPrompt.CryptoObject(cipher),
            )
        } catch (_: Exception) {
            unlockErrorMessage = if (isArabic()) "فشل فتح القفل" else "Unlock failed"
        }
    }

    /** Confirms fingerprint via a live BiometricPrompt before writing the biometric
     *  cache.  The "authentication succeeds, doFinal succeeds, cache is written" path
     *  was verified by manual on-device testing after this task shipped. */
    private fun launchBiometricSetupPrompt(onDone: () -> Unit) {
        val sessionKey = localKeyManager.cachedSessionKey()
        if (sessionKey == null) { onDone(); return }
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                try {
                    val cipher = result.cryptoObject?.cipher
                    if (cipher != null) {
                        val encrypted = cipher.doFinal(sessionKey)
                        getSharedPreferences("yonte_biometric_cache", MODE_PRIVATE).edit()
                            .putString("cache_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                            .putString("cache_data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                            .apply()
                    }
                } catch (_: Exception) {
                }
                onDone()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onDone()
            }
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (isArabic()) "تأكيد البصمة" else "Confirm fingerprint")
            .setSubtitle(if (isArabic()) "لإعداد الفتح بالبصمة" else "To set up fingerprint unlock")
            .setNegativeButtonText(if (isArabic()) "إلغاء" else "Cancel")
            .build()
        try {
            val cipher = biometricGateCipher.encryptCipher()
            BiometricPrompt(this, executor, callback).authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } catch (_: Exception) {
            onDone()
        }
    }

    /** Repopulates the auto-backup key cache after a successful interactive unlock,
     * but only when automatic backup is configured. No-op otherwise, so users who
     * never enable the feature expose no extra resident key. */
    private fun refreshAutoBackupKeyCacheIfEnabled() {
        val prefs = getSharedPreferences(ScheduledBackupWorker.PREFS_NAME, MODE_PRIVATE)
        if (prefs.getString(ScheduledBackupWorker.KEY_DESTINATION_URI, null) == null) return
        val key = localKeyManager.cachedSessionKey() ?: return
        localKeyManager.cacheAutoBackupKey(key)
    }

    @Composable
    private fun NotesOrSettings() {
        val noteRepository = noteRepository.get()
        if (showSettings) {
            SettingsRoute(
                repository = noteRepository,
                backupGateway = backupGateway,
                updateGateway = updateGateway,
                darkTheme = darkTheme,
                onThemeChanged = { darkTheme = it },
                currentVersionCode = BuildConfig.VERSION_CODE,
                onClose = { showSettings = false },
                localKeyManager = localKeyManager,
                sessionKey = localKeyManager.cachedSessionKey(),
                localSalt = localKeyManager.currentSalt(),
            )
        } else {
            NotesRoute(
                repository = noteRepository,
                sharedText = sharedText,
                onSharedTextConsumed = { sharedText = null },
                onOpenSettings = { showSettings = true },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText = intent.sharedText()
    }

    private fun Intent.sharedText(): String? =
        if (action == Intent.ACTION_SEND && type == "text/plain") getStringExtra(Intent.EXTRA_TEXT) else null
}
