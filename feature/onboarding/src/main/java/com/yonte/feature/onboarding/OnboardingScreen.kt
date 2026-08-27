package com.yonte.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhonelinkLock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * First-run welcome + passphrase setup. Shown once, before any note can be created.
 * Approved copy (Arabic-first product, English fallback) — do not let an agent
 * regenerate this wording; it was reviewed explicitly for tone (reassuring, honest,
 * no fear-based language) before being written here.
 */
@Composable
fun OnboardingRoute(
    isProcessing: Boolean = false,
    onComplete: (passphrase: String) -> Unit,
) {
    val isArabic = LocalLayoutDirection.current == LayoutDirection.Rtl
    var step by remember { mutableIntStateOf(0) }
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showMismatch by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            when (step) {
                0 -> WelcomeStep(isArabic) { step = 1 }
                1 -> PrivacyStep(isArabic) { step = 2 }
                else -> PassphraseStep(
                    isArabic = isArabic,
                    isProcessing = isProcessing,
                    passphrase = passphrase,
                    confirm = confirm,
                    showMismatch = showMismatch,
                    onPassphraseChange = { passphrase = it; showMismatch = false },
                    onConfirmChange = { confirm = it; showMismatch = false },
                    onSubmit = {
                        if (passphrase.isNotBlank() && passphrase == confirm) {
                            onComplete(passphrase)
                        } else {
                            showMismatch = true
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(isArabic: Boolean, onNext: () -> Unit) {
    Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(64.dp))
    Spacer(Modifier.height(24.dp))
    Text(
        if (isArabic) "أهلًا بيك في Yonte" else "Welcome to Yonte",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        if (isArabic) "مساحتك الشخصية لملاحظاتك مهامك وعاداتك — كل حاجة في مكان واحد هادئ"
        else "Your personal space for notes, tasks, and habits — all in one calm place.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(40.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text(if (isArabic) "التالي" else "Next")
    }
}

@Composable
private fun PrivacyStep(isArabic: Boolean, onNext: () -> Unit) {
    Icon(Icons.Outlined.PhonelinkLock, contentDescription = null, modifier = Modifier.size(64.dp))
    Spacer(Modifier.height(24.dp))
    Text(
        if (isArabic) "بياناتك محمية بمفتاحك انت بس" else "Protected by a key only you hold",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        if (isArabic)
            "طك حاجة في Yonte بتتخزن على جهازك فقط — مفيش سحابة، مفيش سيرفر, مفيش حد يقدر يشوفها غيرك. عشان كده هنطلب منك كلمة سر عشان نشفّر بياناتك بمفتاح خاص بيك انت بس."
        else
            "Everything in Yonte stays on your device only — no cloud, no server, no one else can see it. That's why we'll ask you for a passphrase, to encrypt your data with a key that's yours alone.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(40.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text(if (isArabic) "التالي" else "Next")
    }
}

@Composable
private fun PassphraseStep(
    isArabic: Boolean,
    isProcessing: Boolean = false,
    passphrase: String,
    confirm: String,
    showMismatch: Boolean,
    onPassphraseChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(64.dp))
    Spacer(Modifier.height(24.dp))
    Text(
        if (isArabic) "اختار كلمة سر" else "Choose a passphrase",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        if (isArabic)
            "هتستخدمها مرة واحدة بس دلوقتي — بعد كده هتفتح التطبيق يوميًا ببصمتك أو رمز قصير. احتفظ بيها في مكان آمن، هي المفتاح الوحيد لبياناتك."
        else
            "You'll use it once, right now — after that you'll unlock the app daily with your fingerprint or a short code. Keep it somewhere safe: it's the only key to your data.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
        value = passphrase,
        onValueChange = onPassphraseChange,
        label = { Text(if (isArabic) "كلمة السر" else "Passphrase") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = confirm,
        onValueChange = onConfirmChange,
        label = { Text(if (isArabic) "تأكيد كلمة السر" else "Confirm passphrase") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        isError = showMismatch,
        supportingText = if (showMismatch) {
            { Text(if (isArabic) "كلمتا السر مش متطابقتين" else "Passphrases don't match") }
        } else null,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(32.dp))
    Button(
        onClick = onSubmit,
        enabled = !isProcessing,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(if (isArabic) "تشفير بياناتي والمتابعة" else "Encrypt my data and continue")
        }
    }
}
