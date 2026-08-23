package com.yonte.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yonte.core.designsystem.YonteTheme
import com.yonte.feature.notes.NotesRoute

class MainActivity : ComponentActivity() {
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedText = intent.sharedText()
        setContent {
            YonteTheme {
                NotesRoute(
                    repository = (application as YonteApplication).noteRepository,
                    sharedText = sharedText,
                    onSharedTextConsumed = { sharedText = null },
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
