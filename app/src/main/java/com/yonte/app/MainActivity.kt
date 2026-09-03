package com.yonte.app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.yonte.core.backup.BackupGateway
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

    private val viewModel: MainViewModel by viewModels()

    private var sharedText by mutableStateOf<String?>(null)
    private var darkTheme by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)
    private var isUnlocking by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedText = intent.sharedText()

        viewModel.setDatabaseWarmer {
            withContext(Dispatchers.IO) { noteRepository.get() }
        }

        val biometricAvailable = BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            YonteTheme(darkTheme = darkTheme) {
                when {
                    uiState.showOnboarding -> OnboardingRoute(
                        isProcessing = isUnlocking,
                        onComplete = { passphrase ->
                            viewModel.completeOnboarding(
                                passphrase = passphrase,
                                isUnlocking = isUnlocking,
                                onStarted = { isUnlocking = true },
                                onFinished = { isUnlocking = false },
                            )
                        },
                    )
                    uiState.unlockScreen == MainUiState.UnlockScreen.SETUP -> QuickUnlockSetupRoute(
                        biometricAvailable = biometricAvailable,
                        isArabic = isArabic(),
                        onChooseBiometric = {
                            viewModel.chooseBiometricUnlock()
                            launchBiometricSetupPrompt {
                                viewModel.clearUnlockError()
                                viewModel.onUnlocked()
                            }
                        },
                        onChoosePin = { viewModel.choosePinCreate() },
                        onSkip = { viewModel.chooseSkipUnlock() },
                    )
                    uiState.unlockScreen == MainUiState.UnlockScreen.PASSPHRASE -> PassphraseUnlockRoute(
                        isArabic = isArabic(),
                        errorMessage = uiState.unlockErrorMessage,
                        onSubmit = { passphrase ->
                            viewModel.submitPassphrase(
                                passphrase = passphrase,
                                context = this@MainActivity,
                                isUnlocking = isUnlocking,
                                isArabic = isArabic(),
                                onUnlockStarted = { isUnlocking = true },
                                onUnlockFinished = { isUnlocking = false },
                            )
                        },
                    )
                    uiState.unlockScreen == MainUiState.UnlockScreen.PIN -> PinRoute(
                        mode = uiState.pinMode,
                        isArabic = isArabic(),
                        errorMessage = uiState.unlockErrorMessage,
                        onSubmit = { pin -> viewModel.submitPin(pin, isArabic()) },
                        onUsePassphraseInstead = { viewModel.switchToPassphrase() },
                    )
                    uiState.unlockScreen == MainUiState.UnlockScreen.BIOMETRIC -> BiometricUnlockRoute(
                        isArabic = isArabic(),
                        errorMessage = uiState.unlockErrorMessage,
                        onTriggerBiometric = ::launchBiometricPrompt,
                        onUseFallbackInstead = { viewModel.switchToPinOrPassphrase() },
                    )
                    uiState.unlocked && uiState.isWarmingDatabase -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    uiState.unlocked -> NotesOrSettings()
                }
            }
        }
    }

    private fun isArabic(): Boolean =
        resources.configuration.layoutDirection == android.util.LayoutDirection.RTL

    private fun launchBiometricPrompt() {
        viewModel.clearUnlockError()
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
                        viewModel.handleBiometricUnlockSuccess(sessionKey)
                    } else {
                        viewModel.handleBiometricUnlockFailure(isArabic())
                    }
                } catch (_: Exception) {
                    viewModel.handleBiometricUnlockFailure(isArabic())
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                viewModel.handleBiometricUnlockError(errorCode, errString)
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
            viewModel.handleBiometricUnlockFailure(isArabic())
        }
    }

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
