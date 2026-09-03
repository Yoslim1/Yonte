package com.yonte.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Approved copy for the app lock gate. Do not let an agent reword any string here.
 * Shown once (QuickUnlockSetupRoute) right after onboarding finishes, and on every
 * cold start thereafter (PinUnlockRoute / BiometricUnlockRoute / PassphraseUnlockRoute)
 * depending on the method the user chose.
 */

@Composable
fun PassphraseUnlockRoute(
    isArabic: Boolean,
    errorMessage: String?,
    onSubmit: (CharArray) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Password, contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                if (isArabic) "أدخل كلمة سر فتح التطبيق" else "Enter your app passphrase",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text(if (isArabic) "كلمة السر" else "Passphrase") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = errorMessage != null,
                supportingText = if (errorMessage != null) { { Text(errorMessage) } } else null,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { if (passphrase.isNotBlank()) onSubmit(passphrase.toCharArray()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isArabic) "فتح" else "Unlock")
            }
        }
    }
}
