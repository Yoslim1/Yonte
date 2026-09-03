package com.yonte.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pin
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

enum class PinFieldMode { CREATE, VERIFY }

@Composable
fun PinRoute(
    mode: PinFieldMode,
    isArabic: Boolean,
    errorMessage: String?,
    onSubmit: (CharArray) -> Unit,
    onUsePassphraseInstead: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showMismatch by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp))
            Icon(Icons.Outlined.Pin, contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(24.dp))
            Text(
                when (mode) {
                    PinFieldMode.CREATE -> if (isArabic) "اختار رمز PIN" else "Choose a PIN"
                    PinFieldMode.VERIFY -> if (isArabic) "أدخل رمز PIN" else "Enter your PIN"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (mode == PinFieldMode.CREATE) {
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isArabic)
                        "الرمز ده خاص بالتطبيق بس، ومش مرتبط بقفل شاشة موبايلك."
                    else
                        "This PIN is specific to the app and separate from your phone's screen lock.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6) { pin = it; showMismatch = false } },
                label = { Text(if (isArabic) "رمز PIN" else "PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                isError = errorMessage != null,
            )
            if (mode == PinFieldMode.CREATE) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { if (it.length <= 6) { confirm = it; showMismatch = false } },
                    label = { Text(if (isArabic) "تأكيد الرمز" else "Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = showMismatch,
                    supportingText = if (showMismatch) {
                        { Text(if (isArabic) "الرمزين مش متطابقين" else "PINs don't match") }
                    } else null,
                )
            }
            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    if (mode == PinFieldMode.CREATE && pin != confirm) {
                        showMismatch = true
                    } else if (pin.length in 4..6) {
                        onSubmit(pin.toCharArray())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when (mode) {
                        PinFieldMode.CREATE -> if (isArabic) "حفظ الرمز" else "Save PIN"
                        PinFieldMode.VERIFY -> if (isArabic) "فتح" else "Unlock"
                    },
                )
            }
            if (mode == PinFieldMode.VERIFY) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onUsePassphraseInstead) {
                    Text(if (isArabic) "استخدم كلمة السر بدلاً من كده" else "Use passphrase instead")
                }
            }
        }
    }
}
