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
fun QuickUnlockSetupRoute(
    biometricAvailable: Boolean,
    isArabic: Boolean,
    onChooseBiometric: () -> Unit,
    onChoosePin: () -> Unit,
    onSkip: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp))
            Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(24.dp))
            Text(
                if (isArabic) "قفل سريع" else "Quick unlock",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (isArabic)
                    "عشان متكتبه كلمة السر كل مرة، اختار طريقة أسرع لفتح التطبيق. تقدر تغيرها في أي وقت من الإعدادات."
                else
                    "So you don't type your full passphrase every time, pick a faster way to unlock. You can change this anytime in Settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))
            if (biometricAvailable) {
                Button(onClick = onChooseBiometric, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isArabic) "استخدم البصمة" else "Use fingerprint")
                }
                Spacer(Modifier.height(12.dp))
            }
            OutlinedButton(onClick = onChoosePin, modifier = Modifier.fillMaxWidth()) {
                Text(if (isArabic) "استخدم رمز PIN" else "Use a PIN")
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(if (isArabic) "تخطي، هستخدم كلمة السر كل مرة" else "Skip, I'll type my passphrase each time")
            }
        }
    }
}
