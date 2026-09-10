package com.yonte.app

import android.content.Intent
import android.os.Bundle
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
import com.yonte.core.security.BiometricUnlockManager
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var noteRepository: Lazy<NoteRepository>
    @Inject lateinit var backupGateway: BackupGateway
    @Inject lateinit var updateGateway: UpdateGateway
    @Inject lateinit var localKeyManager: LocalKeyManager
    @Inject lateinit var appPinManager: AppPinManager
    @Inject lateinit var biometricUnlockManager: BiometricUnlockManager

    private val viewModel: MainViewModel by viewModels()

    private var sharedText by mutableStateOf<String?>(null)
    private var darkTheme by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)
    private var isUnlocking by mutableStateOf(false)
    private var biometricPrompt: BiometricPrompt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedText = intent.sharedText()

        viewModel.setDatabaseWarmer {
            withContext(Dispatchers.IO) { noteRepository.get().getAll() }
        }

        val biometricAvailable = BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            YonteTheme(darkTheme = darkTheme) {
                when {
                    uiState.isDatabaseBlocked -> DatabaseBlockedRoute(isArabic = isArabic())
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
                            launchBiometricSetupPrompt { setupSucceeded ->
                                if (setupSucceeded) {
                                    localKeyManager.setUnlockMethod(LocalKeyManager.METHOD_BIOMETRIC)
                                }
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
                    uiState.isWarmingDatabase -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
        val attemptId = viewModel.beginBiometricUnlock()
        viewModel.clearUnlockError()
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                biometricPrompt = null
                try {
                    val cryptoCipher = result.cryptoObject?.cipher
                    if (cryptoCipher != null) {
                        try {
                            val sessionKey = biometricUnlockManager.unwrapSessionKey(cryptoCipher)
                            viewModel.handleBiometricUnlockSuccess(sessionKey, attemptId)
                        } catch (_: Exception) {
                            // Decryption failed (missing or corrupted cache) — fall back.
                            viewModel.handleBiometricUnlockFallback(attemptId)
                        }
                    } else {
                        viewModel.handleBiometricUnlockFailure(isArabic(), attemptId)
                    }
                } catch (_: Exception) {
                    viewModel.handleBiometricUnlockFailure(isArabic(), attemptId)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                biometricPrompt = null
                viewModel.handleBiometricUnlockError(errorCode, errString, attemptId)
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (isArabic()) "افتح Yonte" else "Unlock Yonte")
            .setSubtitle(if (isArabic()) "استخدم بصمتك" else "Use your fingerprint")
            .setNegativeButtonText(if (isArabic()) "إلغاء" else "Cancel")
            .build()

        val cipher = biometricUnlockManager.buildDecryptCipher()
        if (cipher != null) {
            try {
                biometricPrompt = BiometricPrompt(this, executor, callback)
                biometricPrompt?.authenticate(
                    promptInfo,
                    BiometricPrompt.CryptoObject(cipher),
                )
            } catch (e: Exception) {
                biometricPrompt = null
                if (e is android.security.keystore.KeyPermanentlyInvalidatedException ||
                    generateSequence(e as Throwable?) { it.cause }.any { it is android.security.keystore.KeyPermanentlyInvalidatedException }
                ) {
                    viewModel.handleBiometricUnlockFallback(attemptId)
                } else {
                    biometricPrompt = null
                    viewModel.handleBiometricUnlockFailure(isArabic(), attemptId)
                }
            }
        } else {
            // No IV stored — biometric key is missing or never enrolled.
            viewModel.handleBiometricUnlockFallback(attemptId)
        }
    }

    private fun launchBiometricSetupPrompt(onResult: (Boolean) -> Unit) {
        val sessionKey = localKeyManager.cachedSessionKey()
        if (sessionKey == null) { onResult(false); return }
        val operation = BiometricEnrollmentOperation(sessionKey, onResult)
        try {
            val executor = ContextCompat.getMainExecutor(this)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    try {
                        super.onAuthenticationSucceeded(result)
                        operation.succeed { key ->
                            val cipher = result.cryptoObject?.cipher
                                ?: error("Biometric enrollment returned no cipher")
                            biometricUnlockManager.persistEncryptedKey(cipher, key)
                        }
                    } catch (_: Exception) {
                        operation.fail()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // A failed attempt is non-terminal; keep the session key for
                    // the next authentication attempt.
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    try {
                        super.onAuthenticationError(errorCode, errString)
                    } finally {
                        operation.fail()
                    }
                }
            }
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(if (isArabic()) "تأكيد البصمة" else "Confirm fingerprint")
                .setSubtitle(if (isArabic()) "لإعداد الفتح بالبصمة" else "To set up fingerprint unlock")
                .setNegativeButtonText(if (isArabic()) "إلغاء" else "Cancel")
                .build()
            val cipher = biometricUnlockManager.buildEncryptCipher()
            BiometricPrompt(this, executor, callback).authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } catch (_: Exception) {
            operation.fail()
        }
    }

    @Composable
    private fun DatabaseBlockedRoute(isArabic: Boolean) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (isArabic) {
                        "قاعدة البيانات تحتاج إصدارًا أحدث من Yonte"
                    } else {
                        "Your notes need a newer version of Yonte"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (isArabic) {
                        "هذا الإصدار لا يستطيع فتح قاعدة البيانات. حدّث Yonte إلى آخر إصدار للمتابعة. ملاحظاتك سليمة ولم يُحذف أي شيء."
                    } else {
                        "This version cannot open your database. Update Yonte to the latest version to continue. Your notes are safe — nothing was deleted."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
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
                onSetupRequired = { method ->
                    when (method) {
                        LocalKeyManager.METHOD_BIOMETRIC -> {
                            showSettings = false
                            launchBiometricSetupPrompt { setupSucceeded ->
                                if (setupSucceeded) {
                                    localKeyManager.setUnlockMethod(LocalKeyManager.METHOD_BIOMETRIC)
                                } else {
                                    android.widget.Toast.makeText(
                                        this,
                                        if (isArabic()) "لم يتم إعداد البصمة" else "Biometric setup was not completed",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                viewModel.clearUnlockError()
                            }
                        }
                        LocalKeyManager.METHOD_PIN -> {
                            showSettings = false
                            viewModel.choosePinCreate()
                        }
                    }
                },
                onMethodChanged = { oldMethod, newMethod ->
                    if (oldMethod == LocalKeyManager.METHOD_BIOMETRIC && newMethod != LocalKeyManager.METHOD_BIOMETRIC) {
                        biometricUnlockManager.clearEnrolledKey()
                    }
                },
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

    override fun onDestroy() {
        biometricPrompt?.cancelAuthentication()
        biometricPrompt = null
        viewModel.invalidateSession()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText = intent.sharedText()
    }

    private fun Intent.sharedText(): String? =
        if (action == Intent.ACTION_SEND && type == "text/plain") getStringExtra(Intent.EXTRA_TEXT) else null
}
