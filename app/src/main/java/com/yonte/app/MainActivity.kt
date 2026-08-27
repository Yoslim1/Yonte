package com.yonte.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.yonte.core.backup.BackupGateway
import com.yonte.core.database.NoteRepository
import com.yonte.core.designsystem.YonteTheme
import com.yonte.core.security.LocalKeyManager
import com.yonte.core.update.UpdateGateway
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.yonte.feature.notes.NotesRoute
import com.yonte.feature.onboarding.OnboardingRoute
import com.yonte.feature.settings.SettingsRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var noteRepository: Lazy<NoteRepository>
    @Inject lateinit var backupGateway: BackupGateway
    @Inject lateinit var updateGateway: UpdateGateway
    @Inject lateinit var localKeyManager: LocalKeyManager
    private var sharedText by mutableStateOf<String?>(null)
    private var darkTheme by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)
    private var showOnboarding by mutableStateOf(true)
    private var unlocked by mutableStateOf(false)
    private var isUnlocking by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedText = intent.sharedText()
        // NoteRepository is injected as Lazy so the YonteDatabase Hilt provider only
        // executes after onboarding/unlock produced the SQLCipher key; first run must
        // never reach the database.
        showOnboarding = localKeyManager.isFirstRun()
        unlocked = !showOnboarding && localKeyManager.cachedSessionKey() != null
        setContent {
            YonteTheme(darkTheme = darkTheme) {
                when {
                    showOnboarding -> OnboardingRoute(
                        isProcessing = isUnlocking,
                        onComplete = ::completeOnboarding,
                    )
                    unlocked -> NotesOrSettings()
                }
            }
        }
    }

    private fun completeOnboarding(passphrase: String) {
        if (isUnlocking) return // guard against double-submit
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
            unlocked = true
            isUnlocking = false
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
