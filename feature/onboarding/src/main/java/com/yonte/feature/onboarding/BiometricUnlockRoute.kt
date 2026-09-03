package com.yonte.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Approved copy for the app lock gate. Do not let an agent reword any string here.
 * Shown once (QuickUnlockSetupRoute) right after onboarding finishes, and on every
 * cold start thereafter (PinUnlockRoute / BiometricUnlockRoute / PassphraseUnlockRoute)
 * depending on the method the user chose.
 */

@Composable
fun BiometricUnlockRoute(
    isArabic: Boolean,
    errorMessage: String?,
    onTriggerBiometric: () -> Unit,
    onUseFallbackInstead: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                if (isArabic) "افتح Yonte ببصمتك" else "Unlock Yonte with your fingerprint",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(32.dp))
            Button(onClick = onTriggerBiometric, modifier = Modifier.fillMaxWidth()) {
                Text(if (isArabic) "استخدم البصمة" else "Use fingerprint")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onUseFallbackInstead) {
                Text(if (isArabic) "استخدم رمز PIN أو كلمة السر" else "Use PIN or passphrase instead")
            }
        }
    }
}
