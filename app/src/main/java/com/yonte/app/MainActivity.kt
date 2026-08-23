package com.yonte.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yonte.core.backup.BackupGateway
import com.yonte.core.database.NoteRepository
import com.yonte.core.designsystem.YonteTheme
import com.yonte.core.update.UpdateGateway
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.yonte.feature.notes.NotesRoute

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var noteRepository: NoteRepository
    @Inject lateinit var backupGateway: BackupGateway
    @Inject lateinit var updateGateway: UpdateGateway
    private var sharedText by mutableStateOf<String?>(null)
    private var darkTheme by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedText = intent.sharedText()
        setContent {
            YonteTheme(darkTheme = darkTheme) {
                NotesRoute(
                    repository = noteRepository,
                    backupGateway = backupGateway,
                    updateGateway = updateGateway,
                    sharedText = sharedText,
                    onSharedTextConsumed = { sharedText = null },
                    darkTheme = darkTheme,
                    onThemeChanged = { darkTheme = it },
                    currentVersionCode = BuildConfig.VERSION_CODE,
                )
            }
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
