package com.yonte.app

import com.yonte.feature.onboarding.PinFieldMode

internal data class MainUiState(
    val showOnboarding: Boolean = true,
    val unlocked: Boolean = false,
    val isWarmingDatabase: Boolean = false,
    val unlockScreen: UnlockScreen? = null,
    val pinMode: PinFieldMode = PinFieldMode.VERIFY,
    val unlockErrorMessage: String? = null,
) {
    enum class UnlockScreen { SETUP, PASSPHRASE, PIN, BIOMETRIC }
}
