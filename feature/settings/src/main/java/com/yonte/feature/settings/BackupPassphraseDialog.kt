package com.yonte.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Passphrase prompt shown before export or import. Kept independent from the
 * onboarding/app-unlock passphrase by design — a backup can carry its own credential.
 * Approved copy; do not let an agent reword it.
 */
enum class BackupPassphraseMode { EXPORT, IMPORT }

@Composable
fun BackupPassphraseDialog(
    mode: BackupPassphraseMode,
    isArabic: Boolean,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showMismatch by remember { mutableStateOf(false) }
    val requireConfirmation = mode == BackupPassphraseMode.EXPORT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (mode) {
                    BackupPassphraseMode.EXPORT -> if (isArabic) "كلمة سر النسخة الاحتياطية" else "Backup passphrase"
                    BackupPassphraseMode.IMPORT -> if (isArabic) "أدخل كلمة سر النسخة" else "Enter backup passphrase"
                },
            )
        },
        text = {
            Column {
                Text(
                    when (mode) {
                        BackupPassphraseMode.EXPORT ->
                            if (isArabic)
                                "اختار كلمة سر لهذه النسخة بس — مستقلة عن كلمة سر فتح التطبيق، وممكن تشاركها مع حد تاني لو حبيت."
                            else
                                "Choose a passphrase for this backup only — separate from your app unlock passphrase, and you can share it with someone else if you like."
                        BackupPassphraseMode.IMPORT ->
                            if (isArabic)
                                "أدخل كلمة السر اللي استخدمتها وقت عمل النسخة ده."
                            else
                                "Enter the passphrase you used when this backup was created."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it; showMismatch = false },
                    label = { Text(if (isArabic) "كلمة السر" else "Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                )
                if (requireConfirmation) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it; showMismatch = false },
                        label = { Text(if (isArabic) "تأكيد كلمة السر" else "Confirm passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = showMismatch,
                        supportingText = if (showMismatch) {
                            { Text(if (isArabic) "كلمتا السر مش متطابقتين" else "Passphrases don't match") }
                        } else null,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (requireConfirmation && passphrase != confirm) {
                    showMismatch = true
                } else if (passphrase.isNotBlank()) {
                    onConfirm(passphrase.toCharArray())
                }
            }) {
                Text(
                    when (mode) {
                        BackupPassphraseMode.EXPORT -> if (isArabic) "تصدير" else "Export"
                        BackupPassphraseMode.IMPORT -> if (isArabic) "استيراد" else "Import"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        },
    )
}
